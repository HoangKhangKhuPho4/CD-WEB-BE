package com.cdweb.be.dto;

import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductItemListDto {
  private Integer id;
  private String imei;
  private String serialNumber;
  private String productName;
  private String variantName;
  private String skuCode;
  private ProductItemStatus status;
  private String orderCode;

  @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
  private LocalDateTime createdAt;
}
