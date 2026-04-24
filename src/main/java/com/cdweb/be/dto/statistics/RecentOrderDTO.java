package com.cdweb.be.dto.statistics;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO đơn hàng gần nhất — Table trên Dashboard */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecentOrderDTO {

  private List<OrderSummary> recentOrders;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderSummary {
    private Integer orderId;
    private String orderCode;
    private String customerName;
    private String customerEmail;
    private BigDecimal totalAmount;
    private String status;
    private String paymentMethod;
    private String paymentStatus;
    private String orderDate; // ISO format: "2026-03-08T10:25:00"
    private Integer itemCount; // Số sản phẩm trong đơn
  }
}
