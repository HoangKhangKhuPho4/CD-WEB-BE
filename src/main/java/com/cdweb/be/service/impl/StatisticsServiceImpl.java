package com.cdweb.be.service.impl;

import com.cdweb.be.dto.statistics.*;
import com.cdweb.be.entity.Order;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.repository.OrderDetailRepository;
import com.cdweb.be.repository.OrderRepository;
import com.cdweb.be.repository.ProductVariantRepository;
import com.cdweb.be.service.StatisticsService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Phase 6 — Implementation cho Dashboard Thống kê Doanh thu Nâng cao
 *
 * <p>6 API methods: 1. getOverviewStatistics() → 4 KPI Cards + Growth % 2. getRevenueChart() → Line
 * Chart (Day/Month/Year) 3. getOrderStatusStats() → Doughnut Pie Chart 4. getTopProductStats() →
 * Best-selling / Low-stock Tables 5. getRecentOrders() → 10 đơn hàng mới nhất 6.
 * getPaymentMethodStats() → Horizontal Bar Chart
 */
@Service
@org.springframework.transaction.annotation.Transactional(readOnly = true)
public class StatisticsServiceImpl implements StatisticsService {

  @Autowired private OrderRepository orderRepository;

  @Autowired private OrderDetailRepository orderDetailRepository;

  @Autowired private ProductVariantRepository productVariantRepository;

  @Autowired
  private com.cdweb.be.repository.UserInteractionRepository
      userInteractionRepository; // ✅ Thêm Repository

  private final List<Order.OrderStatus> COMPLETED_STATUSES =
      List.of(Order.OrderStatus.DELIVERED, Order.OrderStatus.COMPLETED);

  private final Order.OrderStatus CANCELLED_STATUS = Order.OrderStatus.CANCELLED;

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 1: OVERVIEW STATISTICS — 4 KPI Cards + Growth %                ██
  // ═══════════════════════════════════════════════════════════════════════════

  @Override
  public OverviewStatisticsDTO getOverviewStatistics() {
    // ── Lấy 4 KPI chính ──────────────────────────────────────────────────
    BigDecimal totalRevenue = orderRepository.sumRevenueCompleted(COMPLETED_STATUSES);
    Long totalOrders = orderRepository.countActiveOrders(CANCELLED_STATUS);
    Long totalCustomers = orderRepository.countDistinctCustomers(CANCELLED_STATUS);
    Long totalProductsSold = orderDetailRepository.sumTotalProductsSold(COMPLETED_STATUSES);
    Long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.PENDING);

    // ── Tính Growth % (tháng này vs tháng trước) ─────────────────────────
    LocalDateTime now = LocalDateTime.now();
    LocalDateTime startOfThisMonth = now.withDayOfMonth(1).with(LocalTime.MIN);
    LocalDateTime startOfLastMonth = startOfThisMonth.minusMonths(1);

    BigDecimal revenueThisMonth =
        orderRepository.sumRevenueByDateRange(startOfThisMonth, now, COMPLETED_STATUSES);
    BigDecimal revenueLastMonth =
        orderRepository.sumRevenueByDateRange(
            startOfLastMonth, startOfThisMonth, COMPLETED_STATUSES);

    Long ordersThisMonth =
        orderRepository.countOrdersByDateRange(startOfThisMonth, now, CANCELLED_STATUS);
    Long ordersLastMonth =
        orderRepository.countOrdersByDateRange(
            startOfLastMonth, startOfThisMonth, CANCELLED_STATUS);

    Long customersThisMonth =
        orderRepository.countDistinctCustomersByDateRange(startOfThisMonth, now, CANCELLED_STATUS);
    Long customersLastMonth =
        orderRepository.countDistinctCustomersByDateRange(
            startOfLastMonth, startOfThisMonth, CANCELLED_STATUS);

    return OverviewStatisticsDTO.builder()
        .totalRevenue(totalRevenue)
        .totalOrders(totalOrders)
        .totalCustomers(totalCustomers)
        .totalProductsSold(totalProductsSold)
        .pendingOrders(pendingOrders)
        .revenueGrowthPercent(calculateGrowthPercent(revenueThisMonth, revenueLastMonth))
        .orderGrowthPercent(calculateGrowthPercentLong(ordersThisMonth, ordersLastMonth))
        .customerGrowthPercent(calculateGrowthPercentLong(customersThisMonth, customersLastMonth))
        .build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 2: REVENUE CHART — Line Chart theo Ngày / Tháng / Năm         ██
  // ═══════════════════════════════════════════════════════════════════════════

  @Override
  public RevenueChartDTO getRevenueChart(String period, String startDateStr, String endDateStr) {
    // ── Xác định khoảng thời gian mặc định ──────────────────────────────
    LocalDateTime start;
    LocalDateTime end = LocalDateTime.now();

    if (startDateStr != null && endDateStr != null) {
      start = LocalDate.parse(startDateStr).atStartOfDay();
      end = LocalDate.parse(endDateStr).atTime(LocalTime.MAX);
    } else {
      switch (period != null ? period : "month") {
        case "day":
          start = end.minusDays(30).with(LocalTime.MIN); // 30 ngày gần nhất
          break;
        case "year":
          start = end.minusYears(5).with(LocalTime.MIN); // 5 năm gần nhất
          break;
        case "month":
        default:
          start = end.minusMonths(12).with(LocalTime.MIN); // 12 tháng gần nhất
          break;
      }
    }

    String effectivePeriod = period != null ? period : "month";
    List<RevenueChartDTO.DataPoint> dataPoints;

    switch (effectivePeriod) {
      case "day":
        dataPoints = buildDailyDataPoints(start, end);
        break;
      case "year":
        dataPoints = buildYearlyDataPoints();
        break;
      case "month":
      default:
        dataPoints = buildMonthlyDataPoints(start, end);
        break;
    }

    // ── Tính tổng và trung bình ──────────────────────────────────────────
    BigDecimal totalRevenue =
        dataPoints.stream()
            .map(RevenueChartDTO.DataPoint::getRevenue)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

    BigDecimal averageRevenue =
        dataPoints.isEmpty()
            ? BigDecimal.ZERO
            : totalRevenue.divide(BigDecimal.valueOf(dataPoints.size()), 0, RoundingMode.HALF_UP);

    return RevenueChartDTO.builder()
        .period(effectivePeriod)
        .dataPoints(dataPoints)
        .totalRevenue(totalRevenue)
        .averageRevenue(averageRevenue)
        .build();
  }

  private List<RevenueChartDTO.DataPoint> buildDailyDataPoints(
      LocalDateTime start, LocalDateTime end) {
    List<Object[]> results = orderRepository.getRevenueByDay(start, end, COMPLETED_STATUSES);
    DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");

    return results.stream()
        .map(
            row -> {
              java.sql.Date date = (java.sql.Date) row[0];
              return RevenueChartDTO.DataPoint.builder()
                  .label(date.toLocalDate().format(fmt))
                  .revenue(toBigDecimal(row[1]))
                  .orders(toLong(row[2]))
                  .build();
            })
        .collect(Collectors.toList());
  }

  private List<RevenueChartDTO.DataPoint> buildMonthlyDataPoints(
      LocalDateTime start, LocalDateTime end) {
    List<Object[]> results = orderRepository.getRevenueByMonth(start, end, COMPLETED_STATUSES);

    return results.stream()
        .map(
            row -> {
              int year = toInt(row[0]);
              int month = toInt(row[1]);
              return RevenueChartDTO.DataPoint.builder()
                  .label(String.format("%02d/%d", month, year))
                  .revenue(toBigDecimal(row[2]))
                  .orders(toLong(row[3]))
                  .build();
            })
        .collect(Collectors.toList());
  }

  private List<RevenueChartDTO.DataPoint> buildYearlyDataPoints() {
    List<Object[]> results = orderRepository.getRevenueByYear(COMPLETED_STATUSES);

    return results.stream()
        .map(
            row ->
                RevenueChartDTO.DataPoint.builder()
                    .label(String.valueOf(toInt(row[0])))
                    .revenue(toBigDecimal(row[1]))
                    .orders(toLong(row[2]))
                    .build())
        .collect(Collectors.toList());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 3: ORDER STATUS STATS — Doughnut Pie Chart                     ██
  // ═══════════════════════════════════════════════════════════════════════════

  @Override
  public OrderStatusStatsDTO getOrderStatusStats() {
    List<Object[]> results = orderRepository.countOrdersByEachStatus();

    // Map label tiếng Việt + màu sắc cho từng trạng thái
    Map<String, String> labelMap =
        Map.of(
            "PENDING", "Chờ xác nhận",
            "CONFIRMED", "Đã xác nhận",
            "PROCESSING", "Đang xử lý",
            "SHIPPING", "Đang giao hàng",
            "DELIVERED", "Đã giao",
            "COMPLETED", "Hoàn thành",
            "CANCELLED", "Đã hủy",
            "REFUNDED", "Hoàn tiền");

    Map<String, String> colorMap =
        Map.of(
            "PENDING", "#FFA726",
            "CONFIRMED", "#42A5F5",
            "PROCESSING", "#AB47BC",
            "SHIPPING", "#26C6DA",
            "DELIVERED", "#66BB6A",
            "COMPLETED", "#4CAF50",
            "CANCELLED", "#EF5350",
            "REFUNDED", "#FF7043");

    long totalOrders = results.stream().mapToLong(row -> toLong(row[1])).sum();

    List<OrderStatusStatsDTO.StatusBreakdown> breakdown =
        results.stream()
            .map(
                row -> {
                  Order.OrderStatus status = (Order.OrderStatus) row[0];
                  String statusStr = status.name();
                  long count = toLong(row[1]);
                  double percentage =
                      totalOrders > 0 ? Math.round(count * 10000.0 / totalOrders) / 100.0 : 0.0;

                  return OrderStatusStatsDTO.StatusBreakdown.builder()
                      .status(statusStr)
                      .label(labelMap.getOrDefault(statusStr, statusStr))
                      .count(count)
                      .percentage(percentage)
                      .color(colorMap.getOrDefault(statusStr, "#9E9E9E"))
                      .build();
                })
            .collect(Collectors.toList());

    return OrderStatusStatsDTO.builder()
        .totalOrders(totalOrders)
        .statusBreakdown(breakdown)
        .build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 4: TOP PRODUCTS — Bán chạy + Tồn kho thấp                     ██
  // ═══════════════════════════════════════════════════════════════════════════

  @Override
  public TopProductStatsDTO getTopProductStats(String type, int limit) {
    if ("low-stock".equals(type)) {
      return buildLowStockStats();
    }
    return buildBestSellingStats(limit);
  }

  private TopProductStatsDTO buildBestSellingStats(int limit) {
    List<Object[]> results =
        orderDetailRepository.findTopSellingProducts(COMPLETED_STATUSES, PageRequest.of(0, limit));
    AtomicInteger rank = new AtomicInteger(1);

    List<TopProductStatsDTO.ProductStat> products =
        results.stream()
            .map(
                row ->
                    TopProductStatsDTO.ProductStat.builder()
                        .rank(rank.getAndIncrement())
                        .productId(toInt(row[0]))
                        .productName((String) row[1])
                        .variantName((String) row[2])
                        .quantitySold(toLong(row[3]))
                        .revenue(toBigDecimal(row[4]))
                        .currentStock(toInt(row[5]))
                        .categoryName(row.length > 6 ? (String) row[6] : null) // Ánh xạ danh mục
                        .imageUrl(row.length > 7 ? (String) row[7] : null)
                        .build())
            .collect(Collectors.toList());

    return TopProductStatsDTO.builder().type("best-selling").products(products).build();
  }

  private TopProductStatsDTO buildLowStockStats() {
    List<ProductVariant> lowStockVariants = productVariantRepository.findLowStockVariants();

    List<TopProductStatsDTO.ProductStat> products =
        lowStockVariants.stream()
            .map(
                v -> {
                  // Xác định mức độ cảnh báo
                  String status;
                  if (v.getStockQuantity() == 0) {
                    status = "OUT_OF_STOCK";
                  } else if (v.getStockQuantity() < 5) {
                    status = "CRITICAL";
                  } else {
                    status = "WARNING";
                  }

                  return TopProductStatsDTO.ProductStat.builder()
                      .productId(v.getProduct().getId())
                      .productName(v.getProduct().getName())
                      .variantName(v.getVariantName())
                      .currentStock(v.getStockQuantity())
                      .lowStockThreshold(v.getLowStockThreshold())
                      .status(status)
                      .categoryName(
                          v.getProduct().getProductType() != null
                              ? v.getProduct().getProductType().getName()
                              : null) // Ánh xạ danh mục danh mục
                      .imageUrl(
                          v.getProduct().getImages() != null
                                  && !v.getProduct().getImages().isEmpty()
                              ? v.getProduct().getImages().get(0).getImageUrl()
                              : null)
                      .build();
                })
            .collect(Collectors.toList());

    return TopProductStatsDTO.builder().type("low-stock").products(products).build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 5: RECENT ORDERS — 10 đơn hàng gần nhất                       ██
  // ═══════════════════════════════════════════════════════════════════════════

  @Override
  public RecentOrderDTO getRecentOrders(int limit) {
    List<Order> orders = orderRepository.findRecentOrders(PageRequest.of(0, limit));

    List<RecentOrderDTO.OrderSummary> summaries =
        orders.stream()
            .map(
                o -> {
                  // Đếm số items trong đơn
                  int itemCount = o.getOrderDetails() != null ? o.getOrderDetails().size() : 0;

                  return RecentOrderDTO.OrderSummary.builder()
                      .orderId(o.getId())
                      .orderCode(o.getOrderCode())
                      .customerName(o.getShippingName())
                      .customerEmail(o.getUser().getEmail())
                      .totalAmount(o.getTotalAmount())
                      .status(o.getStatus().name())
                      .paymentMethod(o.getPaymentMethod().name())
                      .paymentStatus(o.getPaymentStatus().name())
                      .orderDate(o.getOrderDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                      .itemCount(itemCount)
                      .build();
                })
            .collect(Collectors.toList());

    return RecentOrderDTO.builder().recentOrders(summaries).build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 6: PAYMENT METHOD STATS — Horizontal Bar Chart                 ██
  // ═══════════════════════════════════════════════════════════════════════════

  @Override
  public PaymentMethodStatsDTO getPaymentMethodStats() {
    List<Object[]> results = orderRepository.getPaymentMethodStats(CANCELLED_STATUS);

    // Map label tiếng Việt + màu sắc
    Map<String, String> labelMap =
        Map.of(
            "COD", "Thanh toán khi nhận hàng",
            "VNPAY", "VNPay",
            "MOMO", "Ví MoMo",
            "ZALOPAY", "ZaloPay",
            "BANK_TRANSFER", "Chuyển khoản ngân hàng");

    Map<String, String> colorMap =
        Map.of(
            "COD", "#66BB6A",
            "VNPAY", "#42A5F5",
            "MOMO", "#AB47BC",
            "ZALOPAY", "#26C6DA",
            "BANK_TRANSFER", "#FFA726");

    long totalOrders = results.stream().mapToLong(row -> toLong(row[1])).sum();

    List<PaymentMethodStatsDTO.PaymentStat> stats =
        results.stream()
            .map(
                row -> {
                  Order.PaymentMethod method = (Order.PaymentMethod) row[0];
                  String methodStr = method.name();
                  long count = toLong(row[1]);
                  double percentage =
                      totalOrders > 0 ? Math.round(count * 10000.0 / totalOrders) / 100.0 : 0.0;

                  return PaymentMethodStatsDTO.PaymentStat.builder()
                      .method(methodStr)
                      .label(labelMap.getOrDefault(methodStr, methodStr))
                      .orderCount(count)
                      .totalAmount(toBigDecimal(row[2]))
                      .percentage(percentage)
                      .color(colorMap.getOrDefault(methodStr, "#9E9E9E"))
                      .build();
                })
            .collect(Collectors.toList());

    return PaymentMethodStatsDTO.builder().paymentStats(stats).build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 7: CONVERSION RATE — Tỷ lệ chuyển đổi %                       ██
  // ═══════════════════════════════════════════════════════════════════════════
  @Override
  public ConversionRateStatsDTO getConversionRateStats() {
    List<Object[]> results = userInteractionRepository.countViewsAndPurchasesByProduct();

    List<ConversionRateStatsDTO.ProductRate> rates =
        results.stream()
            .map(
                row -> {
                  Integer productId = toInt(row[0]);
                  String productName = (String) row[1];
                  long viewCount = toLong(row[2]);
                  long purchaseCount = toLong(row[3]);

                  double conversionRate =
                      viewCount > 0 ? Math.round(purchaseCount * 10000.0 / viewCount) / 100.0 : 0.0;

                  return ConversionRateStatsDTO.ProductRate.builder()
                      .productId(productId)
                      .productName(productName)
                      .viewCount(viewCount)
                      .purchaseCount(purchaseCount)
                      .conversionRate(conversionRate)
                      .build();
                })
            .collect(Collectors.toList());

    return ConversionRateStatsDTO.builder().productRates(rates).build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 8: CUSTOMER SEGMENTS — Phân khúc Sở thích                     ██
  // ═══════════════════════════════════════════════════════════════════════════
  @Override
  public CustomerSegmentStatsDTO getCustomerSegments() {
    List<Object[]> results = userInteractionRepository.countInteractionsByProductType();

    // Mẫu nhãn phân mảnh (Labels Mapping)
    Map<String, String> labelMap =
        Map.of(
            "Điện thoại", "Đam mê Công nghệ",
            "Laptop", "Gamer / Dân văn phòng",
            "Phụ kiện", "Gia đình / Giải trí",
            "Đồng hồ", "Sức khỏe / Thể thao");

    Map<String, String> colorMap =
        Map.of(
            "Điện thoại", "#FF7043",
            "Laptop", "#42A5F5",
            "Phụ kiện", "#66BB6A",
            "Đồng hồ", "#AB47BC");

    long totalInteractions = results.stream().mapToLong(row -> toLong(row[2])).sum();

    List<CustomerSegmentStatsDTO.SegmentBreakdown> breakdown =
        results.stream()
            .map(
                row -> {
                  Integer typeId = toInt(row[0]);
                  String typeName = (String) row[1];
                  long count = toLong(row[2]);
                  double percentage =
                      totalInteractions > 0
                          ? Math.round(count * 10000.0 / totalInteractions) / 100.0
                          : 0.0;

                  return CustomerSegmentStatsDTO.SegmentBreakdown.builder()
                      .productTypeId(typeId)
                      .categoryName(typeName)
                      .segmentLabel(labelMap.getOrDefault(typeName, "Phân khúc chung"))
                      .userCount(count)
                      .percentage(percentage)
                      .color(colorMap.getOrDefault(typeName, "#9E9E9E"))
                      .build();
                })
            .collect(Collectors.toList());

    return CustomerSegmentStatsDTO.builder().segments(breakdown).build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  GIỮ LẠI 2 METHOD CŨ (BACKWARD COMPATIBILITY)                      ██
  // ═══════════════════════════════════════════════════════════════════════════

  @Override
  public RevenueStatisticsDTO getRevenueStatistics() {
    // TODO: Có thể delegate sang getRevenueChart ở phase sau
    return new RevenueStatisticsDTO();
  }

  @Override
  public TopProductsStatisticsDTO getTopProductsStatistics() {
    // TODO: Có thể delegate sang getTopProductStats ở phase sau
    return new TopProductsStatisticsDTO();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  HELPER METHODS — Type conversion & Growth calculation               ██
  // ═══════════════════════════════════════════════════════════════════════════

  /** Tính % tăng trưởng (BigDecimal) */
  private Double calculateGrowthPercent(BigDecimal current, BigDecimal previous) {
    if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
      return current != null && current.compareTo(BigDecimal.ZERO) > 0 ? 100.0 : 0.0;
    }
    return current
        .subtract(previous)
        .multiply(BigDecimal.valueOf(100))
        .divide(previous, 1, RoundingMode.HALF_UP)
        .doubleValue();
  }

  /** Tính % tăng trưởng (Long) */
  private Double calculateGrowthPercentLong(Long current, Long previous) {
    if (previous == null || previous == 0) {
      return current != null && current > 0 ? 100.0 : 0.0;
    }
    return Math.round((current - previous) * 1000.0 / previous) / 10.0;
  }

  /** Chuyển Object → BigDecimal an toàn */
  private BigDecimal toBigDecimal(Object value) {
    if (value == null) return BigDecimal.ZERO;
    if (value instanceof BigDecimal) return (BigDecimal) value;
    if (value instanceof Double) return BigDecimal.valueOf((Double) value);
    if (value instanceof Long) return BigDecimal.valueOf((Long) value);
    return new BigDecimal(value.toString());
  }

  /** Chuyển Object → Long an toàn */
  private Long toLong(Object value) {
    if (value == null) return 0L;
    if (value instanceof Long) return (Long) value;
    return ((Number) value).longValue();
  }

  /** Chuyển Object → int an toàn */
  private int toInt(Object value) {
    if (value == null) return 0;
    return ((Number) value).intValue();
  }
}
