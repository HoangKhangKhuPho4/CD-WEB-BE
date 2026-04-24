package com.cdweb.be.dto.statistics;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversionRateStatsDTO {

  private List<ProductRate> productRates;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductRate {
    private Integer productId;
    private String productName;
    private Long viewCount;
    private Long purchaseCount;
    private Double conversionRate; // (Purchase / View) * 100
  }
}
