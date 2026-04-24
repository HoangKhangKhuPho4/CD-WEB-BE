package com.cdweb.be.dto.statistics;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO phân bổ đơn hàng theo trạng thái — Doughnut/Pie Chart */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderStatusStatsDTO {

  private Long totalOrders;
  private List<StatusBreakdown> statusBreakdown;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StatusBreakdown {
    private String status; // "PENDING", "CONFIRMED", ...
    private String label; // "Chờ xác nhận", "Đã xác nhận", ...
    private Long count; // Số lượng đơn
    private Double percentage; // Phần trăm
    private String color; // Mã màu hex cho biểu đồ
  }
}
