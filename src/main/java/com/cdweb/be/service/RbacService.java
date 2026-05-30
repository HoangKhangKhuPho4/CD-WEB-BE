package com.cdweb.be.service;

import com.cdweb.be.dto.PermissionDto;
import com.cdweb.be.dto.RoleDto;
import com.cdweb.be.dto.UserDto;
import com.cdweb.be.entity.Permission;
import com.cdweb.be.entity.Role;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.PermissionRepository;
import com.cdweb.be.repository.RoleRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RbacService {

  public static final String ROLE_CUSTOMER = "CUSTOMER";
  public static final Long CUSTOMER_ROLE_ID = 4L;

  private final RoleRepository roleRepository;
  private final PermissionRepository permissionRepository;
  private final ModelMapper modelMapper;

  public Set<String> resolvePermissionCodes(User user) {
    Set<String> codes = new LinkedHashSet<>();
    if (user == null || user.getRoles() == null) {
      return codes;
    }
    for (Role role : user.getRoles()) {
      if (role.getPermissions() == null) {
        continue;
      }
      for (Permission permission : role.getPermissions()) {
        if (permission.getCode() != null) {
          codes.add(permission.getCode());
        }
      }
    }
    return codes;
  }

  public UserDto.Response toUserResponse(User user) {
    UserDto.Response response = modelMapper.map(user, UserDto.Response.class);
    response.setPermissions(resolvePermissionCodes(user));
    return response;
  }

  @Transactional(readOnly = true)
  public List<RoleDto.DetailResponse> listRolesWithPermissions() {
    return roleRepository.findAll().stream().map(this::toRoleDetail).toList();
  }

  @Transactional(readOnly = true)
  public List<PermissionDto.Response> listPermissions() {
    return permissionRepository.findAll().stream()
        .map(p -> modelMapper.map(p, PermissionDto.Response.class))
        .toList();
  }

  @Transactional(readOnly = true)
  public RoleDto.DetailResponse getRoleDetail(Long roleId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));
    return toRoleDetail(role);
  }

  public RoleDto.DetailResponse updateRolePermissions(Long roleId, RoleDto.UpdatePermissionsRequest request) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new ResourceNotFoundException("Role", "id", roleId));

    if (ROLE_CUSTOMER.equalsIgnoreCase(role.getName()) && request.getPermissionIds() != null) {
      throw new BadRequestException("Cannot modify permissions for CUSTOMER role");
    }

    List<Permission> permissions = permissionRepository.findAllById(request.getPermissionIds());
    if (permissions.size() != request.getPermissionIds().size()) {
      throw new BadRequestException("One or more permission IDs are invalid");
    }
    role.setPermissions(new LinkedHashSet<>(permissions));
    return toRoleDetail(roleRepository.save(role));
  }

  public void assertRegisterRoleAllowed(Long roleId) {
    if (roleId == null || CUSTOMER_ROLE_ID.equals(roleId)) {
      return;
    }
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new BadRequestException("Role not found"));
    if (!ROLE_CUSTOMER.equalsIgnoreCase(role.getName())) {
      throw new BadRequestException("Public registration is only allowed for CUSTOMER role");
    }
  }

  public void assertStaffRoleAssignable(Long roleId) {
    Role role =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new BadRequestException("Role not found"));
    String name = role.getName() == null ? "" : role.getName().toUpperCase();
    if (ROLE_CUSTOMER.equals(name)) {
      return;
    }
    if (!Set.of("ADMIN", "WAREHOUSE", "SALES").contains(name)) {
      throw new BadRequestException("Invalid staff role: " + role.getName());
    }
  }

  private RoleDto.DetailResponse toRoleDetail(Role role) {
    RoleDto.DetailResponse dto = new RoleDto.DetailResponse();
    dto.setId(role.getId());
    dto.setName(role.getName());
    dto.setDescription(role.getDescription());
    if (role.getPermissions() != null) {
      dto.setPermissions(
          role.getPermissions().stream()
              .map(p -> modelMapper.map(p, PermissionDto.Response.class))
              .collect(Collectors.toList()));
      dto.setPermissionIds(
          role.getPermissions().stream().map(Permission::getId).collect(Collectors.toList()));
    }
    return dto;
  }
}
