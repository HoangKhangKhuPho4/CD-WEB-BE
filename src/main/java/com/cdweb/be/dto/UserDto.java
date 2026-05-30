package com.cdweb.be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 100, message = "Username must be between 3 and 100 characters")
    private String username;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Full name is required")
    private String fullName; // Đã đổi từ name -> fullName

    private String phone;
    private LocalDate birth;
    private String gender;
    private String address;
    private Long roleId = 4L; // Đã đổi sang Long

    // Các helper để giữ tương thích với code cũ (nếu cần)
    public String getName() { return this.fullName; }
    public void setName(String name) { this.fullName = name; }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String fullName; // Đã đổi từ name -> fullName
    private String phone;
    private LocalDate birth;
    private String gender;
    private String address;

    public String getName() { return this.fullName; }
    public void setName(String name) { this.fullName = name; }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  @Builder
  public static class Response {
    private Long id;
    private String username;
    private String email;
    private String fullName; // Đã đổi từ name -> fullName
    private String phone;
    private LocalDate birth;
    private String gender;
    private String address;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastLoginAt;

    private Boolean enabled; // ĐỔI TỪ Integer status SANG Boolean enabled

    private Set<RoleDto> roles;

    /** Mã quyền gộp từ tất cả role (dùng cho FE ẩn menu / guard). */
    private Set<String> permissions;

    // Helper cho logic cũ
    public Integer getStatus() {
      return Boolean.TRUE.equals(enabled) ? 1 : 0;
    }

    public String getName() {
      return this.fullName;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RoleDto {
    private Long id; // Đổi Integer -> Long
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ChangePasswordRequest {
    @NotBlank(message = "Current password is required")
    private String currentPassword;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "New password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LoginRequest {
    @NotBlank(message = "Username or email is required")
    private String usernameOrEmail;

    @NotBlank(message = "Password is required")
    private String password;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LoginResponse {
    private String token;
    private String type = "Bearer";
    private Response user;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ForgotPasswordRequest {
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ResetPasswordRequest {
    @NotBlank(message = "Token is required")
    private String token;

    @NotBlank(message = "New password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String newPassword;

    @NotBlank(message = "Confirm password is required")
    private String confirmPassword;
  }
}