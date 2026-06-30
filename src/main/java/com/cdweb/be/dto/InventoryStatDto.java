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
  private String productName;
  private String variantName;
  private String skuCode;
  private Integer stockQuantity;
  private Integer lowStockThreshold;
  private Integer defectiveQuantity;
  /** Gợi ý vị trí kệ từ serial AVAILABLE mới nhất */
  private String shelfLocationHint;
  private Double unitPrice;
  private Double stockValue;
  private String status;
}
