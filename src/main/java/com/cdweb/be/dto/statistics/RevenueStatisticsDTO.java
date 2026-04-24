package com.cdweb.be.dto.statistics;

import java.math.BigDecimal;
import java.util.List;

public class RevenueStatisticsDTO {
  private BigDecimal totalRevenue;
  private List<DailyRevenueDTO> dailyRevenue;

  // getters and setters
  public BigDecimal getTotalRevenue() {
    return totalRevenue;
  }

  public void setTotalRevenue(BigDecimal totalRevenue) {
    this.totalRevenue = totalRevenue;
  }

  public List<DailyRevenueDTO> getDailyRevenue() {
    return dailyRevenue;
  }

  public void setDailyRevenue(List<DailyRevenueDTO> dailyRevenue) {
    this.dailyRevenue = dailyRevenue;
  }
}

class DailyRevenueDTO {
  private String date;
  private BigDecimal revenue;

  // getters and setters
  public String getDate() {
    return date;
  }

  public void setDate(String date) {
    this.date = date;
  }

  public BigDecimal getRevenue() {
    return revenue;
  }

  public void setRevenue(BigDecimal revenue) {
    this.revenue = revenue;
  }
}
