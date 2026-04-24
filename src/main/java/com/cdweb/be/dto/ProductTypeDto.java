package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProductTypeDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotBlank(message = "Product type name is required")
    private String name;

    private String code;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String name;
    private String code;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Integer id;
    private String name;
    private String code;
  }
}
