package com.cdweb.be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryStatDto {
  private Integer variantId;
  private String variantName;
  private String skuCode;
  private Integer stockQuantity;
  private String status;
}
