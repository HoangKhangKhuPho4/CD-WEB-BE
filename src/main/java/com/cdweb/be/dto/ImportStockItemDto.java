package com.cdweb.be.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ImportStockItemDto {

  @NotNull(message = "Variant ID không được để trống")
  private Integer variantId;

  @NotNull(message = "Số lượng nhập không được để trống")
  @Min(value = 1, message = "Số lượng nhập phải lớn hơn 0")
  private Integer quantity;
}
