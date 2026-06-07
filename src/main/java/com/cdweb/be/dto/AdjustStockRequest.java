package com.cdweb.be.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdjustStockRequest {

  @NotNull(message = "Variant ID không được để trống")
  private Integer variantId;

  @NotNull(message = "Số lượng điều chỉnh không được để trống")
  @Min(value = 1, message = "Số lượng điều chỉnh phải lớn hơn 0")
  private Integer quantity;

  @NotBlank(message = "Hướng điều chỉnh không được để trống")
  @Pattern(regexp = "INCREASE|DECREASE", message = "Hướng điều chỉnh phải là INCREASE hoặc DECREASE")
  private String direction;

  private String reason;
}
