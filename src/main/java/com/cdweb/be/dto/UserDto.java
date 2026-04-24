package com.cdweb.be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
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

    @NotBlank(message = "Name is required")
    private String name;

    private String phone;
    private LocalDate birth;
    private String gender;
    private String address;
    private Integer roleId = 4; // Default USER role

    public void setFullName(String fullName) {
      this.name = fullName;
    }

    public String getFullName() {
      return this.name;
    }

    public void setBirthAt(LocalDate birthAt) {
      this.birth = birthAt;
    }

    public LocalDate getBirthAt() {
      return this.birth;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String name;
    private String phone;
    private LocalDate birth;
    private String gender;
    private String address;

    public void setFullName(String fullName) {
      this.name = fullName;
    }

    public String getFullName() {
      return this.name;
    }

    public void setBirthAt(LocalDate birthAt) {
      this.birth = birthAt;
    }

    public LocalDate getBirthAt() {
      return this.birth;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Integer id;
    private String username;
    private String email;
    private String name;
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
    private Integer status;
    private Set<RoleDto> roles;

    public void setIsActive(boolean isActive) {
      this.status = isActive ? 1 : 0;
    }

    public String getFullName() {
      return this.name;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RoleDto {
    private Integer id;
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
