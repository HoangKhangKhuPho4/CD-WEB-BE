package com.cdweb.be.dto.statistics;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO tổng quan Dashboard — 4 KPI Cards + Growth % */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewStatisticsDTO {

  // ── 4 KPI chính ──────────────────────────────────────────────────────────
  private BigDecimal totalRevenue; // Tổng doanh thu (đơn DELIVERED + COMPLETED)
  private Long totalOrders; // Tổng đơn hàng (trừ CANCELLED)
  private Long totalCustomers; // Tổng khách hàng (DISTINCT user_id)
  private Long totalProductsSold; // Tổng số lượng SP đã bán

  // ── Đơn hàng chờ xử lý ──────────────────────────────────────────────────
  private Long pendingOrders; // Đơn đang chờ xác nhận (PENDING)

  // ── Tăng trưởng so với tháng trước (%) ───────────────────────────────────
  private Double revenueGrowthPercent; // % tăng trưởng doanh thu
  private Double orderGrowthPercent; // % tăng trưởng đơn hàng
  private Double customerGrowthPercent; // % tăng trưởng khách hàng
}
