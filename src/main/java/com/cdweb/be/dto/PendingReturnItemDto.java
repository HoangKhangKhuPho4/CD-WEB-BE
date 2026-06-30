package com.cdweb.be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingReturnItemDto {
  private Integer productItemId;
  private String imei;
  private String serialNumber;
  private String productName;
  private String skuCode;
  private String orderCode;
  private String updatedAt;
}
