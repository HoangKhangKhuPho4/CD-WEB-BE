package com.cdweb.be.dto.statistics;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO biểu đồ doanh thu — Line Chart theo Ngày / Tháng / Năm */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueChartDTO {

  private String period; // "day" | "month" | "year"
  private List<DataPoint> dataPoints; // Mảng điểm dữ liệu cho biểu đồ
  private BigDecimal totalRevenue; // Tổng doanh thu trong khoảng thời gian
  private BigDecimal averageRevenue; // Trung bình doanh thu

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DataPoint {
    private String label; // Nhãn: "01/03", "03/2026", "2026"
    private BigDecimal revenue; // Doanh thu
    private Long orders; // Số đơn hàng
  }
}
