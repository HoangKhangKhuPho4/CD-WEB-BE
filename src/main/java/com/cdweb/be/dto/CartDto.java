package com.cdweb.be.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CartDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AddItemRequest {
    @NotNull(message = "Variant ID is required")
    private Integer variantId;

    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateItemRequest {
    @NotNull(message = "Quantity is required")
    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CartItemResponse {
    private Integer id;
    private Integer quantity;
    private Double unitPrice;
    private Double subtotal;
    private CartVariantDto variant;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CartVariantDto {
    private Integer id;
    private String skuCode;
    private String variantName;
    private Double price;
    private Double originalPrice;
    private Integer stockQuantity;
    private Boolean isDefault;
    private CartProductDto product;
    private String imageUrl;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CartProductDto {
    private Integer id;
    private String name;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CartResponse {
    private Integer id;
    private List<CartItemResponse> items;
    private Integer totalItems;
    private Double totalAmount;
  }
}
