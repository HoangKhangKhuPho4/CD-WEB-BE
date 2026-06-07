package com.cdweb.be.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReturnQuantityRequest {

  @NotNull(message = "Variant ID không được để trống")
  private Integer variantId;

  @NotNull(message = "Số lượng trả không được để trống")
  @Min(value = 1, message = "Số lượng trả phải lớn hơn 0")
  private Integer quantity;

  private String reason;
  private Boolean isDefective = false;
}
