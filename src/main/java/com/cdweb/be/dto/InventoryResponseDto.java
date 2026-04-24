package com.cdweb.be.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponseDto {
  private Integer id;
  private String transactionType;
  private Integer quantity;
  private String referenceType;
  private Integer referenceId;
  private String reason;
  private LocalDateTime createdAt;

  // Variant info
  private Integer variantId;
  private String variantName;
  private String skuCode;

  // ProductItem info (optional)
  private Integer productItemId;
  private String imei;

  // User who made the transaction
  private Integer userId;
  private String userName;
}
