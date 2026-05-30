package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class RoleDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotBlank(message = "Role name is required")
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Long id;
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DetailResponse {
    private Long id;
    private String name;
    private String description;
    private List<Integer> permissionIds;
    private List<PermissionDto.Response> permissions;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdatePermissionsRequest {
    @NotEmpty(message = "permissionIds is required")
    private List<Integer> permissionIds;
  }
}
