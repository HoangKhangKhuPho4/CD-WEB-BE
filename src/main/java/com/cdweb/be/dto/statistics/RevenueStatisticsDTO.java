package com.cdweb.be.dto.statistics;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RevenueStatisticsDTO {
  private BigDecimal totalRevenue;
  private List<DailyRevenueDTO> dailyRevenue;

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DailyRevenueDTO {
    private String date;
    private BigDecimal revenue;
  }
}
