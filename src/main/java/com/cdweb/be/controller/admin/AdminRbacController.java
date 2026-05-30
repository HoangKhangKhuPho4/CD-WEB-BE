package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.PermissionDto;
import com.cdweb.be.dto.RoleDto;
import com.cdweb.be.service.RbacService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/rbac")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_PERM_EDIT', 'ROLE_ADMIN')")
public class AdminRbacController {

  private final RbacService rbacService;

  @GetMapping("/roles")
  public ResponseEntity<ApiResponse<List<RoleDto.DetailResponse>>> listRoles() {
    return ResponseEntity.ok(ApiResponse.success("OK", rbacService.listRolesWithPermissions()));
  }

  @GetMapping("/roles/{id}")
  public ResponseEntity<ApiResponse<RoleDto.DetailResponse>> getRole(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success("OK", rbacService.getRoleDetail(id)));
  }

  @GetMapping("/permissions")
  public ResponseEntity<ApiResponse<List<PermissionDto.Response>>> listPermissions() {
    return ResponseEntity.ok(ApiResponse.success("OK", rbacService.listPermissions()));
  }

  @PutMapping("/roles/{id}/permissions")
  public ResponseEntity<ApiResponse<RoleDto.DetailResponse>> updateRolePermissions(
      @PathVariable Long id, @Valid @RequestBody RoleDto.UpdatePermissionsRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Role permissions updated", rbacService.updateRolePermissions(id, request)));
  }
}
