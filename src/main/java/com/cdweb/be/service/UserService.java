package com.cdweb.be.service;

import com.cdweb.be.dto.UserDto;
import com.cdweb.be.entity.Role;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.entity.UserAddress;
import com.cdweb.be.repository.AddressRepository;
import com.cdweb.be.repository.RoleRepository;
import com.cdweb.be.repository.UserRepository;
import java.util.Collections;
import lombok.RequiredArgsConstructor; // Sử dụng để hết cảnh báo Field Injection
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final AddressRepository addressRepository;
  private final RoleRepository roleRepository;
  private final ModelMapper modelMapper;
  private final PasswordEncoder passwordEncoder;
  private final AuditLogService auditLogService;
  private final RbacService rbacService;

  public Page<UserDto.Response> getAllUsers(Pageable pageable) {
    return userRepository
            .findAllActive(pageable)
            .map(user -> modelMapper.map(user, UserDto.Response.class));
  }

  public Page<UserDto.Response> getAllUsersForAdmin(String keyword, Pageable pageable) {
    Page<User> usersPage;
    if (keyword != null && !keyword.trim().isEmpty()) {
      usersPage = userRepository.searchUsers(keyword.trim(), pageable);
    } else {
      usersPage = userRepository.findAll(pageable);
    }
    return usersPage.map(user -> modelMapper.map(user, UserDto.Response.class));
  }

  // Đã sửa: Integer -> Long
  public UserDto.Response getUserById(Long id) {
    User user =
            userRepository
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    return rbacService.toUserResponse(user);
  }

  public UserDto.Response getUserByUsername(String username) {
    User user =
            userRepository
                    .findByUsernameOrEmail(username, username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
    return rbacService.toUserResponse(user);
  }

  @Transactional(readOnly = true)
  public UserDto.Response getProfileByUsername(String username) {
    UserDto.Response response = getUserByUsername(username);
    enrichWithDefaultAddress(response);
    return response;
  }

  private void enrichWithDefaultAddress(UserDto.Response response) {
    if (response.getId() == null) {
      return;
    }
    addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(response.getId()).stream()
            .findFirst()
            .ifPresent(addr -> response.setAddress(addr.getAddressDetail()));
  }

  public UserDto.Response createUser(UserDto.CreateRequest request) {
    if (userRepository.existsByUsername(request.getUsername())) {
      throw new BadRequestException("Username is already taken!");
    }
    if (userRepository.existsByEmail(request.getEmail())) {
      throw new BadRequestException("Email Address already in use!");
    }

    User user = new User();
    user.setUsername(request.getUsername());
    user.setEmail(request.getEmail());
    user.setPassword(passwordEncoder.encode(request.getPassword()));
    // Đã sửa: setName -> setFullName
    user.setFullName(request.getName());
    user.setPhone(request.getPhone());
    user.setBirth(request.getBirth());
    user.setGender(request.getGender());
    user.setEnabled(true);

    Long roleId = request.getRoleId() != null ? request.getRoleId() : RbacService.CUSTOMER_ROLE_ID;
    rbacService.assertStaffRoleAssignable(roleId);
    Role userRole =
            roleRepository
                    .findById(roleId)
                    .orElseThrow(() -> new BadRequestException("Role not found"));
    user.setRoles(Collections.singleton(userRole));

    User savedUser = userRepository.save(user);
    auditLogService.log(
            "CREATE_USER",
            "User",
            savedUser.getId().toString(),
            "Created user: " + savedUser.getUsername());
    return modelMapper.map(savedUser, UserDto.Response.class);
  }

  public UserDto.Response updateUser(Long id, UserDto.UpdateRequest updateRequest) {
    User user =
            userRepository
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));

    if (updateRequest.getName() != null) {
      // Đã sửa: setName -> setFullName
      user.setFullName(updateRequest.getName());
    }
    if (updateRequest.getPhone() != null) {
      user.setPhone(updateRequest.getPhone());
    }
    if (updateRequest.getBirth() != null) {
      user.setBirth(updateRequest.getBirth());
    }
    if (updateRequest.getGender() != null) {
      user.setGender(updateRequest.getGender());
    }

    User updatedUser = userRepository.save(user);

    if (updateRequest.getAddress() != null) {
      syncDefaultAddress(updatedUser, updateRequest.getAddress());
    }

    auditLogService.log("UPDATE_USER", "User", updatedUser.getId().toString(), "Updated user info");
    UserDto.Response response = rbacService.toUserResponse(updatedUser);
    enrichWithDefaultAddress(response);
    return response;
  }

  private void syncDefaultAddress(User user, String addressDetail) {
    var existing =
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId()).stream()
                    .findFirst()
                    .orElse(null);
    if (existing != null) {
      existing.setAddressDetail(addressDetail);
      if (user.getFullName() != null) {
        existing.setReceiverName(user.getFullName());
      }
      if (user.getPhone() != null) {
        existing.setPhone(user.getPhone());
      }
      addressRepository.save(existing);
      return;
    }
    UserAddress address = new UserAddress();
    address.setUser(user);
    address.setReceiverName(user.getFullName() != null ? user.getFullName() : user.getUsername());
    address.setPhone(user.getPhone() != null ? user.getPhone() : "");
    address.setAddressDetail(addressDetail);
    address.setIsDefault(true);
    address.setLabel("Nhà riêng");
    addressRepository.save(address);
  }

  // Đã sửa: Integer -> Long, setStatus -> setEnabled
  public void deleteUser(Long id) {
    User user =
            userRepository
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    user.setEnabled(false);
    userRepository.save(user);
    auditLogService.log("DELETE_USER", "User", id.toString(), "Soft deleted user");
  }

  public void activateUser(Long id) {
    User user =
            userRepository
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    user.setEnabled(true);
    userRepository.save(user);
  }

  public void deactivateUser(Long id) {
    User user =
            userRepository
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    user.setEnabled(false);
    userRepository.save(user);
  }

  public UserDto.Response toggleStatus(Long id) {
    User user =
            userRepository
                    .findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "id", id));
    user.setEnabled(!user.isAccountEnabled());
    User updatedUser = userRepository.save(user);
    auditLogService.log(
            "TOGGLE_STATUS", "User", id.toString(), "New status: " + updatedUser.getEnabled());
    return rbacService.toUserResponse(updatedUser);
  }

  public Page<UserDto.Response> searchUsers(String keyword, Pageable pageable) {
    return userRepository
            .searchUsers(keyword, pageable)
            .map(rbacService::toUserResponse);
  }

  @Transactional(readOnly = true)
  public Page<UserDto.Response> getCustomersForStaff(String keyword, Pageable pageable) {
    Page<User> page;
    if (keyword != null && !keyword.trim().isEmpty()) {
      page = userRepository.searchCustomers(keyword.trim(), pageable);
    } else {
      page = userRepository.findCustomers(pageable);
    }
    return page.map(u -> rbacService.toUserResponse(u));
  }

  public void changePassword(String username, UserDto.ChangePasswordRequest request) {
    User user =
            userRepository
                    .findByUsername(username)
                    .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
      throw new BadRequestException("Current password is incorrect");
    }

    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new BadRequestException("New password and confirm password do not match");
    }

    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);
  }
}