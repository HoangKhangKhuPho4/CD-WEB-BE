package com.cdweb.be.dto;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class WishlistDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {
    @NotNull(message = "Product ID is required")
    private Integer productId;

    private Integer variantId; // Optional
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Integer id;
    private LocalDateTime createdAt;
    private ProductDto.Response product;
    private ProductDto.VariantDto variant;
  }
}
