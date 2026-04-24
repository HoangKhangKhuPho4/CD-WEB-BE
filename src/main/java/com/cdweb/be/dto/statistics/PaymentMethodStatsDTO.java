package com.cdweb.be.dto.statistics;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO thống kê phương thức thanh toán — Horizontal Bar Chart */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodStatsDTO {

  private List<PaymentStat> paymentStats;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PaymentStat {
    private String method; // "COD", "VNPAY", "MOMO", "ZALOPAY", "BANK_TRANSFER"
    private String label; // "Thanh toán khi nhận hàng", "VNPay", ...
    private Long orderCount; // Số đơn hàng dùng phương thức này
    private BigDecimal totalAmount; // Tổng tiền
    private Double percentage; // Phần trăm
    private String color; // Mã màu hex
  }
}
