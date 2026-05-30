package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.statistics.ConversionRateStatsDTO;
import com.cdweb.be.dto.statistics.StaffOverviewStatisticsDTO;
import com.cdweb.be.dto.statistics.CustomerSegmentStatsDTO;
import com.cdweb.be.dto.statistics.OrderStatusStatsDTO;
import com.cdweb.be.dto.statistics.OverviewStatisticsDTO;
import com.cdweb.be.dto.statistics.PaymentMethodStatsDTO;
import com.cdweb.be.dto.statistics.RecentOrderDTO;
import com.cdweb.be.dto.statistics.RevenueChartDTO;
import com.cdweb.be.dto.statistics.RevenueStatisticsDTO;
import com.cdweb.be.dto.statistics.TopProductStatsDTO;
import com.cdweb.be.dto.statistics.TopProductsStatisticsDTO;
import com.cdweb.be.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Phase 6 — Dashboard Thống kê Doanh thu Nâng cao
 *
 * <h3>6 API Endpoints:</h3>
 *
 * <ul>
 *   <li>GET /api/admin/statistics/overview — 4 KPI Cards + Growth %
 *   <li>GET /api/admin/statistics/revenue/chart — Line Chart (Day/Month/Year)
 *   <li>GET /api/admin/statistics/orders/by-status — Doughnut Pie Chart
 *   <li>GET /api/admin/statistics/top-products — Best-selling / Low-stock
 *   <li>GET /api/admin/statistics/orders/recent — 10 đơn hàng mới nhất
 *   <li>GET /api/admin/statistics/payment-methods — Payment Bar Chart
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/statistics")
public class StatisticsController {

  @Autowired private StatisticsService statisticsService;

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 1: OVERVIEW — Tổng quan Dashboard (4 KPI Cards)                ██
  // ═══════════════════════════════════════════════════════════════════════════

  @GetMapping("/overview")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'ROLE_ADMIN')")
  public ResponseEntity<OverviewStatisticsDTO> getOverviewStatistics() {
    return ResponseEntity.ok(statisticsService.getOverviewStatistics());
  }

  @GetMapping("/staff-overview")
  @PreAuthorize(
      "hasAnyAuthority('ORDER_MANAGE', 'ORDER_VIEW_ALL', 'STOCK_IMPORT', 'REPORT_SALES', "
          + "'INVENTORY_STAT', 'ROLE_ADMIN')")
  public ResponseEntity<StaffOverviewStatisticsDTO> getStaffOverviewStatistics() {
    return ResponseEntity.ok(statisticsService.getStaffOverviewStatistics());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 2: REVENUE CHART — Biểu đồ doanh thu (Ngày / Tháng / Năm)     ██
  // ═══════════════════════════════════════════════════════════════════════════

  @GetMapping("/revenue/chart")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'ROLE_ADMIN')")
  public ResponseEntity<RevenueChartDTO> getRevenueChart(
      @RequestParam(defaultValue = "month") String period,
      @RequestParam(required = false) String startDate,
      @RequestParam(required = false) String endDate) {
    return ResponseEntity.ok(statisticsService.getRevenueChart(period, startDate, endDate));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 3: ORDER STATUS — Phân bổ đơn hàng theo trạng thái            ██
  // ═══════════════════════════════════════════════════════════════════════════

  @GetMapping("/orders/by-status")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'REPORT_SALES', 'ROLE_ADMIN')")
  public ResponseEntity<OrderStatusStatsDTO> getOrderStatusStats() {
    return ResponseEntity.ok(statisticsService.getOrderStatusStats());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 4: TOP PRODUCTS — Bán chạy / Tồn kho thấp                     ██
  // ═══════════════════════════════════════════════════════════════════════════

  @GetMapping("/top-products")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'REPORT_SALES', 'ROLE_ADMIN')")
  public ResponseEntity<TopProductStatsDTO> getTopProductStats(
      @RequestParam(defaultValue = "best-selling") String type,
      @RequestParam(defaultValue = "10") int limit) {
    return ResponseEntity.ok(statisticsService.getTopProductStats(type, limit));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 5: RECENT ORDERS — Đơn hàng gần nhất                          ██
  // ═══════════════════════════════════════════════════════════════════════════

  @GetMapping("/orders/recent")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'REPORT_SALES', 'ROLE_ADMIN')")
  public ResponseEntity<RecentOrderDTO> getRecentOrders(
      @RequestParam(defaultValue = "10") int limit) {
    return ResponseEntity.ok(statisticsService.getRecentOrders(limit));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 6: PAYMENT METHODS — Thống kê phương thức thanh toán           ██
  // ═══════════════════════════════════════════════════════════════════════════

  @GetMapping("/payment-methods")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'ROLE_ADMIN')")
  public ResponseEntity<PaymentMethodStatsDTO> getPaymentMethodStats() {
    return ResponseEntity.ok(statisticsService.getPaymentMethodStats());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 7: CONVERSION RATE — Tỷ lệ chuyển đổi %                       ██
  // ═══════════════════════════════════════════════════════════════════════════
  @GetMapping("/conversion-rate")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'ROLE_ADMIN')")
  public ResponseEntity<ConversionRateStatsDTO> getConversionRateStats() {
    return ResponseEntity.ok(statisticsService.getConversionRateStats());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 8: CUSTOMER SEGMENTS — Phân khúc Sở thích                     ██
  // ═══════════════════════════════════════════════════════════════════════════
  @GetMapping("/customer-segments")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'ROLE_ADMIN')")
  public ResponseEntity<CustomerSegmentStatsDTO> getCustomerSegments() {
    return ResponseEntity.ok(statisticsService.getCustomerSegments());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  GIỮ LẠI 2 API CŨ (BACKWARD COMPATIBILITY)                         ██
  // ═══════════════════════════════════════════════════════════════════════════

  @GetMapping("/revenue")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'ROLE_ADMIN')")
  public RevenueStatisticsDTO getRevenueStatistics() {
    return statisticsService.getRevenueStatistics();
  }

  @GetMapping("/top-products-legacy")
  @PreAuthorize("hasAnyAuthority('REPORT_REVENUE', 'REPORT_SALES', 'ROLE_ADMIN')")
  public TopProductsStatisticsDTO getTopProductsStatistics() {
    return statisticsService.getTopProductsStatistics();
  }
}
