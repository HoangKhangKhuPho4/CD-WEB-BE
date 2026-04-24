package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AttributeDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotBlank(message = "Attribute name is required")
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    @NotBlank(message = "Attribute name is required")
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Integer id;
    private String name;
  }
}
