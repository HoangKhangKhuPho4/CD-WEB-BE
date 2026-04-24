package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.UserDto;
import com.cdweb.be.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

  @Autowired private UserService userService;

  /** GET /api/users/me Lấy thông tin cá nhân của người dùng hiện tại */
  @GetMapping("/me")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<UserDto.Response>> getMyProfile() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    UserDto.Response user = userService.getUserByUsername(username);
    return ResponseEntity.ok(ApiResponse.success("Lấy thông tin cá nhân thành công", user));
  }

  /** PUT /api/users/me Cập nhật thông tin cá nhân của người dùng hiện tại */
  @PutMapping("/me")
  @PreAuthorize("hasAuthority('USER_PROFILE_UPDATE')")
  public ResponseEntity<ApiResponse<UserDto.Response>> updateMyProfile(
      @Valid @RequestBody UserDto.UpdateRequest updateRequest) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    UserDto.Response currentUser = userService.getUserByUsername(username);
    UserDto.Response updatedUser = userService.updateUser(currentUser.getId(), updateRequest);
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật thông tin cá nhân thành công", updatedUser));
  }

  /** PUT /api/users/change-password Người dùng hiện tại đổi mật khẩu */
  @PutMapping("/change-password")
  @PreAuthorize("hasAuthority('USER_PROFILE_UPDATE')")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @Valid @RequestBody UserDto.ChangePasswordRequest request) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();
    userService.changePassword(username, request);
    return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
  }

  /**
   * GET /api/users/search Tìm kiếm người dùng (Dành cho nhân viên kiểm tra thông tin khách hàng)
   */
  @GetMapping("/search")
  @PreAuthorize("hasAnyAuthority('USER_MANAGE', 'CUSTOMER_VIEW')")
  public ResponseEntity<ApiResponse<Page<UserDto.Response>>> searchUsers(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<UserDto.Response> users = userService.searchUsers(keyword, pageable);
    return ResponseEntity.ok(ApiResponse.success("Tìm kiếm người dùng hoàn tất", users));
  }
}
