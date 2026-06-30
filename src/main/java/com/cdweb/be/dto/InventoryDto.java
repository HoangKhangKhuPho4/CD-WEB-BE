package com.cdweb.be.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class InventoryDto {

  private InventoryDto() {}

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateImportItemResult {
    private Integer variantId;
    private String skuCode;
    private String productName;
    private String variantName;
    private Integer currentStock;
    private Integer requestedQuantity;
    private Double unitCost;
    private Double lineTotal;
    private boolean valid;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateImportResponse {
    private boolean allValid;
    private List<ValidateImportItemResult> results;
    private String supplier;
    private String note;
    private Double estimatedTotalValue;
  }
}
