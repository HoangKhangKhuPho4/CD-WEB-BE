package com.cdweb.be.service.impl;

import com.cdweb.be.dto.statistics.*;
import com.cdweb.be.entity.Order;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.exception.BadRequestException;
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
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
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

  @Autowired private com.cdweb.be.repository.UserRepository userRepository;

  @Autowired
  private com.cdweb.be.repository.UserInteractionRepository
      userInteractionRepository; // ✅ Thêm Repository

  private final List<Order.OrderStatus> COMPLETED_STATUSES =
      List.of(Order.OrderStatus.DELIVERED, Order.OrderStatus.COMPLETED);

  private final Order.OrderStatus CANCELLED_STATUS = Order.OrderStatus.CANCELLED;

  private static final Set<String> REVENUE_PERIODS = Set.of("day", "month", "year");
  private static final Set<String> TOP_PRODUCT_TYPES = Set.of("best-selling", "low-stock");
  private static final int MIN_LIST_LIMIT = 1;
  private static final int MAX_LIST_LIMIT = 100;

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 1: OVERVIEW STATISTICS — 4 KPI Cards + Growth %                ██
  // ═══════════════════════════════════════════════════════════════════════════

  @Override
  public OverviewStatisticsDTO getOverviewStatistics(String fromDateStr, String toDateStr) {
    Optional<DateRange> range = parseOptionalDateRange(fromDateStr, toDateStr, "fromDate", "toDate");

    if (range.isEmpty()) {
      BigDecimal totalRevenue = orderRepository.sumRevenueCompleted(COMPLETED_STATUSES);
      Long totalOrders = orderRepository.countActiveOrders(CANCELLED_STATUS);
      Long totalCustomers = orderRepository.countDistinctCustomers(CANCELLED_STATUS);
      Long totalProductsSold = orderDetailRepository.sumTotalProductsSold(COMPLETED_STATUSES);
      Long pendingOrders = orderRepository.countByStatus(Order.OrderStatus.PENDING);

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
          orderRepository.countDistinctCustomersByDateRange(
              startOfThisMonth, now, CANCELLED_STATUS);
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
          .customerGrowthPercent(
              calculateGrowthPercentLong(customersThisMonth, customersLastMonth))
          .build();
    }

    DateRange dr = range.get();
    BigDecimal totalRevenue =
        orderRepository.sumRevenueByDateRange(dr.start(), dr.end(), COMPLETED_STATUSES);
    Long totalOrders =
        orderRepository.countOrdersByDateRange(dr.start(), dr.end(), CANCELLED_STATUS);
    Long totalCustomers =
        orderRepository.countDistinctCustomersByDateRange(dr.start(), dr.end(), CANCELLED_STATUS);
    Long totalProductsSold =
        orderDetailRepository.sumTotalProductsSoldByDateRange(
            COMPLETED_STATUSES, dr.start(), dr.end());
    Long pendingOrders =
        orderRepository.countByStatusAndDateRange(
            Order.OrderStatus.PENDING, dr.start(), dr.end());

    DateRange previous = previousPeriodOfSameLength(dr);
    BigDecimal revenuePrev =
        orderRepository.sumRevenueByDateRange(
            previous.start(), previous.end(), COMPLETED_STATUSES);
    Long ordersPrev =
        orderRepository.countOrdersByDateRange(
            previous.start(), previous.end(), CANCELLED_STATUS);
    Long customersPrev =
        orderRepository.countDistinctCustomersByDateRange(
            previous.start(), previous.end(), CANCELLED_STATUS);

    return OverviewStatisticsDTO.builder()
        .totalRevenue(totalRevenue)
        .totalOrders(totalOrders)
        .totalCustomers(totalCustomers)
        .totalProductsSold(totalProductsSold)
        .pendingOrders(pendingOrders)
        .revenueGrowthPercent(calculateGrowthPercent(totalRevenue, revenuePrev))
        .orderGrowthPercent(calculateGrowthPercentLong(totalOrders, ordersPrev))
        .customerGrowthPercent(calculateGrowthPercentLong(totalCustomers, customersPrev))
        .build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  API 2: REVENUE CHART — Line Chart theo Ngày / Tháng / Năm         ██
  // ═══════════════════════════════════════════════════════════════════════════

  @Override
  public RevenueChartDTO getRevenueChart(String period, String startDateStr, String endDateStr) {
    String effectivePeriod = normalizeRevenuePeriod(period);
    validatePairedDateParams(startDateStr, endDateStr);

    // ── Xác định khoảng thời gian mặc định ──────────────────────────────
    LocalDateTime start;
    LocalDateTime end = LocalDateTime.now();

    if (hasText(startDateStr) && hasText(endDateStr)) {
      LocalDate startDate = parseIsoDate(startDateStr.trim(), "startDate");
      LocalDate endDate = parseIsoDate(endDateStr.trim(), "endDate");
      if (startDate.isAfter(endDate)) {
        throw new BadRequestException("startDate không được lớn hơn endDate");
      }
      start = startDate.atStartOfDay();
      end = endDate.atTime(LocalTime.MAX);
    } else {
      validatePairedDateParams(startDateStr, endDateStr);
      switch (effectivePeriod) {
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

    List<RevenueChartDTO.DataPoint> dataPoints;

    switch (effectivePeriod) {
      case "day":
        dataPoints = buildDailyDataPoints(start, end);
        break;
      case "year":
        dataPoints = buildYearlyDataPoints(start, end);
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

  private List<RevenueChartDTO.DataPoint> buildYearlyDataPoints(
      LocalDateTime start, LocalDateTime end) {
    List<Object[]> results =
        orderRepository.getRevenueByYearInRange(start, end, COMPLETED_STATUSES);

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
  public OrderStatusStatsDTO getOrderStatusStats(String fromDateStr, String toDateStr) {
    Optional<DateRange> range = parseOptionalDateRange(fromDateStr, toDateStr, "fromDate", "toDate");
    List<Object[]> results =
        range.isEmpty()
            ? orderRepository.countOrdersByEachStatus()
            : orderRepository.countOrdersByEachStatusInRange(range.get().start(), range.get().end());
    return buildOrderStatusStats(results);
  }

  private OrderStatusStatsDTO buildOrderStatusStats(List<Object[]> results) {
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
  public TopProductStatsDTO getTopProductStats(
      String type, int limit, String fromDate, String toDate) {
    String normalizedType = normalizeTopProductType(type);
    int safeLimit = normalizeListLimit(limit);
    if ("low-stock".equals(normalizedType)) {
      return buildLowStockStats();
    }
    Optional<DateRange> dateRange =
        parseOptionalDateRange(fromDate, toDate, "fromDate", "toDate");
    return buildBestSellingStats(safeLimit, dateRange);
  }

  private TopProductStatsDTO buildBestSellingStats(int limit, Optional<DateRange> dateRange) {
    List<Object[]> results =
        dateRange
            .map(
                range ->
                    orderDetailRepository.findTopSellingProductsByDateRange(
                        COMPLETED_STATUSES,
                        range.start(),
                        range.end(),
                        PageRequest.of(0, limit)))
            .orElseGet(
                () ->
                    orderDetailRepository.findTopSellingProducts(
                        COMPLETED_STATUSES, PageRequest.of(0, limit)));
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
  public RecentOrderDTO getRecentOrders(int limit, String fromDate, String toDate) {
    int safeLimit = normalizeListLimit(limit);
    Optional<DateRange> dateRange =
        parseOptionalDateRange(fromDate, toDate, "fromDate", "toDate");
    List<Order> orders =
        dateRange
            .map(
                range ->
                    orderRepository.findRecentOrdersByDateRange(
                        range.start(), range.end(), PageRequest.of(0, safeLimit)))
            .orElseGet(() -> orderRepository.findRecentOrders(PageRequest.of(0, safeLimit)));

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
  public PaymentMethodStatsDTO getPaymentMethodStats(String fromDateStr, String toDateStr) {
    Optional<DateRange> range = parseOptionalDateRange(fromDateStr, toDateStr, "fromDate", "toDate");
    List<Object[]> results =
        range.isEmpty()
            ? orderRepository.getPaymentMethodStats(CANCELLED_STATUS)
            : orderRepository.getPaymentMethodStatsByDateRange(
                range.get().start(), range.get().end(), CANCELLED_STATUS);
    return buildPaymentMethodStats(results);
  }

  @Override
  public byte[] exportRevenueCsv(String period, String startDate, String endDate) {
    RevenueChartDTO chart = getRevenueChart(period, startDate, endDate);
    StringBuilder sb = new StringBuilder();
    sb.append("period,label,revenue,orders\n");
    if (chart.getDataPoints() != null) {
      for (RevenueChartDTO.DataPoint dp : chart.getDataPoints()) {
        sb.append(csvEscape(chart.getPeriod())).append(",");
        sb.append(csvEscape(dp.getLabel())).append(",");
        sb.append(dp.getRevenue() != null ? dp.getRevenue() : BigDecimal.ZERO).append(",");
        sb.append(dp.getOrders() != null ? dp.getOrders() : 0L).append("\n");
      }
    }
    sb.append("\nmetric,value\n");
    sb.append("totalRevenue,").append(chart.getTotalRevenue() != null ? chart.getTotalRevenue() : 0).append("\n");
    sb.append("averageRevenue,").append(chart.getAverageRevenue() != null ? chart.getAverageRevenue() : 0).append("\n");
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private PaymentMethodStatsDTO buildPaymentMethodStats(List<Object[]> results) {
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
    RevenueChartDTO chart = getRevenueChart("day", null, null);
    List<RevenueStatisticsDTO.DailyRevenueDTO> daily =
        chart.getDataPoints() == null
            ? List.of()
            : chart.getDataPoints().stream()
                .map(
                    dp ->
                        new RevenueStatisticsDTO.DailyRevenueDTO(
                            dp.getLabel(), dp.getRevenue()))
                .collect(Collectors.toList());
    RevenueStatisticsDTO dto = new RevenueStatisticsDTO();
    dto.setTotalRevenue(chart.getTotalRevenue() != null ? chart.getTotalRevenue() : BigDecimal.ZERO);
    dto.setDailyRevenue(daily);
    return dto;
  }

  @Override
  public TopProductsStatisticsDTO getTopProductsStatistics() {
    TopProductStatsDTO stats = getTopProductStats("best-selling", 10, null, null);
    TopProductsStatisticsDTO dto = new TopProductsStatisticsDTO();
    if (stats.getProducts() == null) {
      dto.setTopProducts(List.of());
      return dto;
    }
    List<TopProductDTO> legacy =
        stats.getProducts().stream()
            .map(
                p -> {
                  TopProductDTO item = new TopProductDTO();
                  item.setProductId(p.getProductId() != null ? p.getProductId().longValue() : 0L);
                  item.setProductName(p.getProductName());
                  item.setQuantitySold(
                      p.getQuantitySold() != null ? p.getQuantitySold().intValue() : 0);
                  item.setTotalRevenue(
                      p.getRevenue() != null ? p.getRevenue().doubleValue() : 0.0);
                  return item;
                })
            .collect(Collectors.toList());
    dto.setTopProducts(legacy);
    return dto;
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

  @Override
  public StaffOverviewStatisticsDTO getStaffOverviewStatistics(
      String fromDate, String toDate) {
    Optional<DateRange> dateRange =
        parseOptionalDateRange(fromDate, toDate, "fromDate", "toDate");

    long pending;
    long confirmed;
    long shipping;
    long ordersInPeriod;

    if (dateRange.isPresent()) {
      DateRange range = dateRange.get();
      pending =
          nullSafeCount(
              orderRepository.countByStatusAndDateRange(
                  Order.OrderStatus.PENDING, range.start(), range.end()));
      confirmed =
          nullSafeCount(
              orderRepository.countByStatusAndDateRange(
                  Order.OrderStatus.CONFIRMED, range.start(), range.end()));
      shipping =
          nullSafeCount(
              orderRepository.countByStatusAndDateRange(
                  Order.OrderStatus.SHIPPING, range.start(), range.end()));
      Long counted =
          orderRepository.countOrdersByDateRange(
              range.start(), range.end(), CANCELLED_STATUS);
      ordersInPeriod = counted != null ? counted : 0L;
    } else {
      LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
      LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
      Long ordersToday =
          orderRepository.countOrdersByDateRange(startOfDay, endOfDay, CANCELLED_STATUS);
      ordersInPeriod = ordersToday != null ? ordersToday : 0L;
      pending = nullSafeCount(orderRepository.countByStatus(Order.OrderStatus.PENDING));
      confirmed = nullSafeCount(orderRepository.countByStatus(Order.OrderStatus.CONFIRMED));
      shipping = nullSafeCount(orderRepository.countByStatus(Order.OrderStatus.SHIPPING));
    }

    return StaffOverviewStatisticsDTO.builder()
        .pendingOrders(pending)
        .confirmedOrders(confirmed)
        .shippingOrders(shipping)
        .ordersToday(ordersInPeriod)
        .lowStockVariants(productVariantRepository.countLowStockVariants())
        .customerAccounts(userRepository.countCustomerAccounts())
        .build();
  }

  private long nullSafeCount(Long value) {
    return value != null ? value : 0L;
  }

  private String normalizeRevenuePeriod(String period) {
    if (!hasText(period)) {
      return "month";
    }
    String normalized = period.trim().toLowerCase(Locale.ROOT);
    if (!REVENUE_PERIODS.contains(normalized)) {
      throw new BadRequestException("period phải là day, month hoặc year");
    }
    return normalized;
  }

  private void validatePairedDateParams(String fromStr, String toStr) {
    boolean hasFrom = hasText(fromStr);
    boolean hasTo = hasText(toStr);
    if (hasFrom != hasTo) {
      throw new BadRequestException("Tham số ngày phải được cung cấp theo cặp");
    }
  }

  private Optional<DateRange> parseOptionalDateRange(
      String fromStr, String toStr, String fromLabel, String toLabel) {
    validatePairedDateParams(fromStr, toStr);
    if (!hasText(fromStr)) {
      return Optional.empty();
    }
    LocalDate from = parseIsoDate(fromStr.trim(), fromLabel);
    LocalDate to = parseIsoDate(toStr.trim(), toLabel);
    if (from.isAfter(to)) {
      throw new BadRequestException(fromLabel + " không được lớn hơn " + toLabel);
    }
    return Optional.of(new DateRange(from.atStartOfDay(), to.atTime(LocalTime.MAX)));
  }

  private DateRange previousPeriodOfSameLength(DateRange current) {
    long days =
        ChronoUnit.DAYS.between(current.start().toLocalDate(), current.end().toLocalDate()) + 1;
    LocalDateTime prevEnd = current.start().minusSeconds(1);
    LocalDateTime prevStart = prevEnd.toLocalDate().minusDays(days - 1).atStartOfDay();
    return new DateRange(prevStart, prevEnd);
  }

  private String csvEscape(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private LocalDate parseIsoDate(String value, String paramName) {
    try {
      return LocalDate.parse(value);
    } catch (DateTimeParseException ex) {
      throw new BadRequestException(paramName + " phải có định dạng yyyy-MM-dd");
    }
  }

  private String normalizeTopProductType(String type) {
    if (!hasText(type)) {
      return "best-selling";
    }
    String normalized = type.trim().toLowerCase(Locale.ROOT);
    if (!TOP_PRODUCT_TYPES.contains(normalized)) {
      throw new BadRequestException("type phải là best-selling hoặc low-stock");
    }
    return normalized;
  }

  private int normalizeListLimit(int limit) {
    if (limit < MIN_LIST_LIMIT) {
      throw new BadRequestException("limit phải từ " + MIN_LIST_LIMIT + " đến " + MAX_LIST_LIMIT);
    }
    return Math.min(limit, MAX_LIST_LIMIT);
  }

  private boolean hasText(String value) {
    return value != null && !value.isBlank();
  }

  private record DateRange(LocalDateTime start, LocalDateTime end) {}
}
