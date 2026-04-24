package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AttributeValueDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotNull(message = "Attribute ID is required")
    private Integer attributeId;

    @NotBlank(message = "Value is required")
    private String value;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Integer id;
    private String attributeName;
    private String value;
  }
}
