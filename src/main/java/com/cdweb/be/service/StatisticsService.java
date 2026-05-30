package com.cdweb.be.service;

import com.cdweb.be.dto.statistics.*;

/** Phase 6 — Service interface cho Dashboard Thống kê Doanh thu Nâng cao */
public interface StatisticsService {

  /** API 1: Tổng quan Dashboard — 4 KPI Cards + Growth % */
  OverviewStatisticsDTO getOverviewStatistics();

  /** API 2: Biểu đồ doanh thu theo Ngày/Tháng/Năm */
  RevenueChartDTO getRevenueChart(String period, String startDate, String endDate);

  /** API 3: Phân bổ đơn hàng theo trạng thái — Pie Chart */
  OrderStatusStatsDTO getOrderStatusStats();

  /** API 4: Sản phẩm bán chạy HOẶC tồn kho thấp */
  TopProductStatsDTO getTopProductStats(String type, int limit);

  /** API 5: Đơn hàng gần nhất */
  RecentOrderDTO getRecentOrders(int limit);

  /** API 6: Thống kê phương thức thanh toán */
  PaymentMethodStatsDTO getPaymentMethodStats();

  /** API 7: Tỷ lệ chuyển đổi % (Lượt xem -> Lượt mua) cho Admin */
  ConversionRateStatsDTO getConversionRateStats();

  /** API 8: Phân mảnh sở thích tập khách hàng */
  CustomerSegmentStatsDTO getCustomerSegments();

  // ── Giữ lại 2 method cũ cho tương thích ngược ───────────────────────────
  RevenueStatisticsDTO getRevenueStatistics();

  TopProductsStatisticsDTO getTopProductsStatistics();

  /** KPI cho nhân viên kho / sale (không cần quyền doanh thu). */
  StaffOverviewStatisticsDTO getStaffOverviewStatistics();
}
