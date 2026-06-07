package com.cdweb.be.service.impl;

import com.cdweb.be.dto.CouponDto;
import com.cdweb.be.entity.Coupon;
import com.cdweb.be.entity.Order;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.CouponRepository;
import com.cdweb.be.repository.CouponSpecification;
import com.cdweb.be.repository.OrderRepository;
import com.cdweb.be.service.CouponService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CouponServiceImpl implements CouponService {

  private static final List<Order.OrderStatus> EXCLUDED_ORDER_STATUSES =
      List.of(Order.OrderStatus.CANCELLED, Order.OrderStatus.REFUNDED);

  @Autowired private CouponRepository couponRepository;
  @Autowired private OrderRepository orderRepository;

  @Override
  @Transactional(readOnly = true)
  public Page<CouponDto.Response> getAllCoupons(
      String keyword,
      Boolean isActive,
      String discountType,
      String lifecycle,
      Pageable pageable) {
    LocalDateTime now = LocalDateTime.now();
    Specification<Coupon> spec =
        CouponSpecification.adminFilter(keyword, isActive, discountType, lifecycle, now);
    return couponRepository.findAll(spec, pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public CouponDto.AdminStatsResponse getAdminStats() {
    LocalDateTime now = LocalDateTime.now();
    CouponDto.AdminStatsResponse stats = new CouponDto.AdminStatsResponse();
    stats.setTotal(couponRepository.count());
    stats.setActive(couponRepository.countByIsActiveTrue());
    stats.setInactive(couponRepository.countByIsActiveFalse());
    stats.setExpired(couponRepository.countExpired(now));
    stats.setUpcoming(couponRepository.countUpcoming(now));
    stats.setExhausted(couponRepository.countExhausted());
    Long usedSum = couponRepository.sumUsedCount();
    stats.setTotalUsedCount(usedSum != null ? usedSum : 0L);
    stats.setFirstOrderOnlyCount(couponRepository.countByFirstOrderOnlyTrue());
    return stats;
  }

  @Override
  @Transactional(readOnly = true)
  public CouponDto.Response getCouponById(Integer id) {
    return mapToResponse(findCouponOrThrow(id));
  }

  @Override
  @Transactional(readOnly = true)
  public CouponDto.Response getCouponByCode(String code) {
    Coupon coupon =
        couponRepository
            .findByCode(normalizeCode(code))
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", code));
    return mapToResponse(coupon);
  }

  @Override
  public CouponDto.Response createCoupon(CouponDto.CreateRequest request) {
    validateCreateRequest(request);
    String code = normalizeCode(request.getCode());
    if (couponRepository.existsByCode(code)) {
      throw new BadRequestException("Mã coupon đã tồn tại");
    }

    Coupon coupon = new Coupon();
    applyCreateRequest(request, coupon);
    coupon.setCode(code);
    coupon.setUsedCount(0);
    return mapToResponse(couponRepository.save(coupon));
  }

  @Override
  public CouponDto.Response updateCoupon(Integer id, CouponDto.UpdateRequest request) {
    Coupon coupon = findCouponOrThrow(id);
    if (request.getCode() != null && !normalizeCode(request.getCode()).equals(coupon.getCode())) {
      String newCode = normalizeCode(request.getCode());
      if (couponRepository.existsByCode(newCode)) {
        throw new BadRequestException("Mã coupon đã tồn tại");
      }
      coupon.setCode(newCode);
    }
    applyUpdateRequest(request, coupon);
    validateDates(coupon.getDateStart(), coupon.getDateEnd());
    validateDiscount(coupon.getDiscountType(), coupon.getDiscountValue());
    validateScope(coupon);
    return mapToResponse(couponRepository.save(coupon));
  }

  @Override
  public void deleteCoupon(Integer id) {
    Coupon coupon = findCouponOrThrow(id);
    coupon.setIsActive(false);
    couponRepository.save(coupon);
  }

  @Override
  public void hardDeleteCoupon(Integer id) {
    Coupon coupon = findCouponOrThrow(id);
    if (coupon.getUsedCount() != null && coupon.getUsedCount() > 0) {
      throw new BadRequestException("Không thể xóa cứng coupon đã được sử dụng");
    }
    couponRepository.delete(coupon);
  }

  @Override
  public CouponDto.Response toggleCouponStatus(Integer id) {
    Coupon coupon = findCouponOrThrow(id);
    coupon.setIsActive(coupon.getIsActive() == null || !coupon.getIsActive());
    return mapToResponse(couponRepository.save(coupon));
  }

  @Override
  public List<CouponDto.Response> bulkUpdateStatus(CouponDto.BulkStatusRequest request) {
    if (request.getIds() == null || request.getIds().isEmpty()) {
      throw new BadRequestException("Danh sách ID không được trống");
    }
    List<Coupon> coupons = couponRepository.findAllById(request.getIds());
    if (coupons.size() != request.getIds().size()) {
      throw new BadRequestException("Một hoặc nhiều coupon không tồn tại");
    }
    coupons.forEach(c -> c.setIsActive(request.getIsActive()));
    return couponRepository.saveAll(coupons).stream().map(this::mapToResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CouponDto.PublicResponse> getAvailableCoupons() {
    return couponRepository.findAvailableCoupons(LocalDateTime.now()).stream()
        .map(this::mapToPublicResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public List<CouponDto.Response> getActiveCoupons() {
    return couponRepository.findActiveCoupons(LocalDateTime.now()).stream()
        .map(this::mapToResponse)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public CouponDto.ValidateResponse validateCoupon(CouponDto.ValidateRequest request) {
    return validateForCheckout(
        request.getCode(),
        request.getUserId(),
        request.getSubtotal() != null ? request.getSubtotal() : BigDecimal.ZERO,
        request.getItems());
  }

  @Override
  @Transactional(readOnly = true)
  public CouponDto.ValidateResponse validateForCheckout(
      String couponCode,
      Long userId,
      BigDecimal orderTotal,
      List<CouponDto.CheckoutLineItem> lineItems) {
    String code = normalizeCode(couponCode);
    CouponDto.ValidateResponse response = new CouponDto.ValidateResponse();
    response.setCode(code);

    Coupon coupon = couponRepository.findByCode(code).orElse(null);
    if (coupon == null) {
      return invalid(response, "Mã giảm giá không tồn tại");
    }

    LocalDateTime now = LocalDateTime.now();
    if (!Boolean.TRUE.equals(coupon.getIsActive())) {
      return invalid(response, "Mã giảm giá đã bị vô hiệu hóa");
    }
    if (coupon.getDateStart() != null && now.isBefore(coupon.getDateStart())) {
      return invalid(response, "Mã giảm giá chưa có hiệu lực");
    }
    if (coupon.getDateEnd() != null && now.isAfter(coupon.getDateEnd())) {
      return invalid(response, "Mã giảm giá đã hết hạn");
    }
    if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
      return invalid(response, "Mã giảm giá đã đạt giới hạn sử dụng");
    }

    BigDecimal eligibleSubtotal = resolveEligibleSubtotal(coupon, orderTotal, lineItems);
    if (eligibleSubtotal.compareTo(BigDecimal.ZERO) <= 0) {
      return invalid(response, "Giỏ hàng không có sản phẩm phù hợp với mã giảm giá này");
    }

    if (coupon.getMinOrderValue() != null
        && eligibleSubtotal.compareTo(coupon.getMinOrderValue()) < 0) {
      return invalid(
          response,
          "Đơn hàng tối thiểu "
              + coupon.getMinOrderValue().toPlainString()
              + " VNĐ để sử dụng mã này");
    }

    if (userId != null) {
      if (Boolean.TRUE.equals(coupon.getFirstOrderOnly())) {
        long orderCount =
            orderRepository.countActiveOrdersByUserId(userId, EXCLUDED_ORDER_STATUSES);
        if (orderCount > 0) {
          return invalid(response, "Mã giảm giá chỉ áp dụng cho đơn hàng đầu tiên");
        }
      }
      if (coupon.getPerUserLimit() != null && coupon.getPerUserLimit() > 0) {
        long userUsage =
            orderRepository.countCouponUsageByUser(
                userId, coupon.getCode(), EXCLUDED_ORDER_STATUSES);
        if (userUsage >= coupon.getPerUserLimit()) {
          return invalid(response, "Bạn đã sử dụng hết lượt cho mã giảm giá này");
        }
      }
    }

    BigDecimal discountAmount = calculateDiscount(coupon, eligibleSubtotal);
    BigDecimal finalAmount = eligibleSubtotal.subtract(discountAmount);
    if (finalAmount.compareTo(BigDecimal.ZERO) < 0) {
      finalAmount = BigDecimal.ZERO;
    }

    response.setValid(true);
    response.setMessage("Mã giảm giá hợp lệ");
    response.setDiscountType(coupon.getDiscountType().name());
    response.setDiscountValue(coupon.getDiscountValue());
    response.setOriginalSubtotal(eligibleSubtotal);
    response.setDiscountAmount(discountAmount);
    response.setFinalAmount(finalAmount);
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderTotal) {
    if (coupon == null || orderTotal == null || orderTotal.compareTo(BigDecimal.ZERO) <= 0) {
      return BigDecimal.ZERO;
    }
    BigDecimal discountAmount;
    if (coupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
      discountAmount =
          orderTotal
              .multiply(coupon.getDiscountValue())
              .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
      if (coupon.getMaxDiscountAmount() != null
          && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
        discountAmount = coupon.getMaxDiscountAmount();
      }
    } else {
      discountAmount = coupon.getDiscountValue();
      if (discountAmount.compareTo(orderTotal) > 0) {
        discountAmount = orderTotal;
      }
    }
    return discountAmount.max(BigDecimal.ZERO);
  }

  @Override
  @Transactional(readOnly = true)
  public Page<CouponDto.UsageOrderSummary> getCouponUsageOrders(String couponCode, Pageable pageable) {
    String code = normalizeCode(couponCode);
    if (!couponRepository.findByCode(code).isPresent()) {
      throw new ResourceNotFoundException("Coupon", "code", code);
    }
    return orderRepository
        .findByCouponCode(code, EXCLUDED_ORDER_STATUSES, pageable)
        .map(this::mapToUsageSummary);
  }

  @Override
  public void incrementUsage(String couponCode) {
    Coupon coupon =
        couponRepository
            .findByCode(normalizeCode(couponCode))
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", "code", couponCode));
    coupon.setUsedCount((coupon.getUsedCount() == null ? 0 : coupon.getUsedCount()) + 1);
    couponRepository.save(coupon);
  }

  @Override
  public void decrementUsage(String couponCode) {
    couponRepository
        .findByCode(normalizeCode(couponCode))
        .ifPresent(
            coupon -> {
              int used = coupon.getUsedCount() == null ? 0 : coupon.getUsedCount();
              coupon.setUsedCount(Math.max(0, used - 1));
              couponRepository.save(coupon);
            });
  }

  // ─── Private helpers ───────────────────────────────────────────────────────

  private Coupon findCouponOrThrow(Integer id) {
    return couponRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
  }

  private String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      throw new BadRequestException("Mã coupon không được trống");
    }
    return code.trim().toUpperCase();
  }

  private void validateCreateRequest(CouponDto.CreateRequest request) {
    validateDates(request.getDateStart(), request.getDateEnd());
    Coupon.DiscountType type = parseDiscountType(request.getDiscountType());
    validateDiscount(type, request.getDiscountValue());
    Coupon.ScopeType scope = parseScopeType(request.getScopeType());
    validateScopeFields(scope, request.getProductIds(), request.getProductTypeIds());
  }

  private void applyCreateRequest(CouponDto.CreateRequest request, Coupon coupon) {
    coupon.setName(request.getName());
    coupon.setDescription(request.getDescription());
    coupon.setDiscountType(parseDiscountType(request.getDiscountType()));
    coupon.setDiscountValue(request.getDiscountValue());
    coupon.setMinOrderValue(request.getMinOrderValue());
    coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
    coupon.setUsageLimit(request.getUsageLimit());
    coupon.setPerUserLimit(request.getPerUserLimit());
    coupon.setFirstOrderOnly(Boolean.TRUE.equals(request.getFirstOrderOnly()));
    coupon.setScopeType(parseScopeType(request.getScopeType()));
    coupon.setProductIds(copySet(request.getProductIds()));
    coupon.setProductTypeIds(copySet(request.getProductTypeIds()));
    coupon.setDateStart(request.getDateStart());
    coupon.setDateEnd(request.getDateEnd());
    coupon.setIsActive(request.getIsActive() == null || request.getIsActive());
  }

  private void applyUpdateRequest(CouponDto.UpdateRequest request, Coupon coupon) {
    if (request.getName() != null) coupon.setName(request.getName());
    if (request.getDescription() != null) coupon.setDescription(request.getDescription());
    if (request.getDiscountType() != null) {
      coupon.setDiscountType(parseDiscountType(request.getDiscountType()));
    }
    if (request.getDiscountValue() != null) coupon.setDiscountValue(request.getDiscountValue());
    if (request.getMinOrderValue() != null) coupon.setMinOrderValue(request.getMinOrderValue());
    if (request.getMaxDiscountAmount() != null) {
      coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
    }
    if (request.getUsageLimit() != null) coupon.setUsageLimit(request.getUsageLimit());
    if (request.getPerUserLimit() != null) coupon.setPerUserLimit(request.getPerUserLimit());
    if (request.getFirstOrderOnly() != null) coupon.setFirstOrderOnly(request.getFirstOrderOnly());
    if (request.getScopeType() != null) {
      coupon.setScopeType(parseScopeType(request.getScopeType()));
    }
    if (request.getProductIds() != null) coupon.setProductIds(copySet(request.getProductIds()));
    if (request.getProductTypeIds() != null) {
      coupon.setProductTypeIds(copySet(request.getProductTypeIds()));
    }
    if (request.getDateStart() != null) coupon.setDateStart(request.getDateStart());
    if (request.getDateEnd() != null) coupon.setDateEnd(request.getDateEnd());
    if (request.getIsActive() != null) coupon.setIsActive(request.getIsActive());
  }

  private Coupon.DiscountType parseDiscountType(String type) {
    try {
      return Coupon.DiscountType.valueOf(type.trim().toUpperCase());
    } catch (Exception e) {
      throw new BadRequestException("Loại giảm giá không hợp lệ. Dùng PERCENT hoặc FIXED");
    }
  }

  private Coupon.ScopeType parseScopeType(String scope) {
    if (scope == null || scope.isBlank()) {
      return Coupon.ScopeType.ALL;
    }
    try {
      return Coupon.ScopeType.valueOf(scope.trim().toUpperCase());
    } catch (Exception e) {
      throw new BadRequestException("Phạm vi áp dụng không hợp lệ. Dùng ALL, PRODUCTS hoặc PRODUCT_TYPES");
    }
  }

  private void validateDates(LocalDateTime start, LocalDateTime end) {
    if (start == null || end == null) {
      throw new BadRequestException("Ngày bắt đầu và kết thúc là bắt buộc");
    }
    if (!end.isAfter(start)) {
      throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
    }
  }

  private void validateDiscount(Coupon.DiscountType type, BigDecimal value) {
    if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BadRequestException("Giá trị giảm phải lớn hơn 0");
    }
    if (type == Coupon.DiscountType.PERCENT && value.compareTo(BigDecimal.valueOf(100)) > 0) {
      throw new BadRequestException("Giảm giá phần trăm không được vượt quá 100%");
    }
  }

  private void validateScope(Coupon coupon) {
    validateScopeFields(coupon.getScopeType(), coupon.getProductIds(), coupon.getProductTypeIds());
  }

  private void validateScopeFields(
      Coupon.ScopeType scope, Set<Integer> productIds, Set<Integer> productTypeIds) {
    if (scope == Coupon.ScopeType.PRODUCTS
        && (productIds == null || productIds.isEmpty())) {
      throw new BadRequestException("Cần chọn ít nhất một sản phẩm khi scope = PRODUCTS");
    }
    if (scope == Coupon.ScopeType.PRODUCT_TYPES
        && (productTypeIds == null || productTypeIds.isEmpty())) {
      throw new BadRequestException(
          "Cần chọn ít nhất một loại sản phẩm khi scope = PRODUCT_TYPES");
    }
  }

  private Set<Integer> copySet(Set<Integer> source) {
    return source == null ? new HashSet<>() : new HashSet<>(source);
  }

  private BigDecimal resolveEligibleSubtotal(
      Coupon coupon, BigDecimal orderTotal, List<CouponDto.CheckoutLineItem> lineItems) {
    if (lineItems == null || lineItems.isEmpty()) {
      return orderTotal != null ? orderTotal : BigDecimal.ZERO;
    }
    Coupon.ScopeType scope =
        coupon.getScopeType() != null ? coupon.getScopeType() : Coupon.ScopeType.ALL;
    if (scope == Coupon.ScopeType.ALL) {
      return lineItems.stream()
          .map(CouponDto.CheckoutLineItem::getLineTotal)
          .filter(t -> t != null)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    if (scope == Coupon.ScopeType.PRODUCTS) {
      Set<Integer> ids = coupon.getProductIds() != null ? coupon.getProductIds() : Set.of();
      return lineItems.stream()
          .filter(item -> item.getProductId() != null && ids.contains(item.getProductId()))
          .map(CouponDto.CheckoutLineItem::getLineTotal)
          .filter(t -> t != null)
          .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
    Set<Integer> typeIds =
        coupon.getProductTypeIds() != null ? coupon.getProductTypeIds() : Set.of();
    return lineItems.stream()
        .filter(
            item ->
                item.getProductTypeId() != null && typeIds.contains(item.getProductTypeId()))
        .map(CouponDto.CheckoutLineItem::getLineTotal)
        .filter(t -> t != null)
        .reduce(BigDecimal.ZERO, BigDecimal::add);
  }

  private CouponDto.ValidateResponse invalid(CouponDto.ValidateResponse response, String message) {
    response.setValid(false);
    response.setMessage(message);
    response.setDiscountAmount(BigDecimal.ZERO);
    return response;
  }

  private String resolveLifecycleStatus(Coupon coupon) {
    LocalDateTime now = LocalDateTime.now();
    if (!Boolean.TRUE.equals(coupon.getIsActive())) {
      return "INACTIVE";
    }
    if (coupon.getDateEnd() != null && now.isAfter(coupon.getDateEnd())) {
      return "EXPIRED";
    }
    if (coupon.getDateStart() != null && now.isBefore(coupon.getDateStart())) {
      return "UPCOMING";
    }
    if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
      return "EXHAUSTED";
    }
    return "ACTIVE";
  }

  private CouponDto.Response mapToResponse(Coupon coupon) {
    CouponDto.Response response = new CouponDto.Response();
    response.setId(coupon.getId());
    response.setCode(coupon.getCode());
    response.setName(coupon.getName());
    response.setDescription(coupon.getDescription());
    response.setDiscountType(coupon.getDiscountType().name());
    response.setDiscountValue(coupon.getDiscountValue());
    response.setMinOrderValue(coupon.getMinOrderValue());
    response.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
    response.setUsageLimit(coupon.getUsageLimit());
    response.setUsedCount(coupon.getUsedCount());
    response.setPerUserLimit(coupon.getPerUserLimit());
    response.setFirstOrderOnly(coupon.getFirstOrderOnly());
    response.setScopeType(
        coupon.getScopeType() != null ? coupon.getScopeType().name() : Coupon.ScopeType.ALL.name());
    response.setProductIds(
        coupon.getProductIds() != null ? new HashSet<>(coupon.getProductIds()) : new HashSet<>());
    response.setProductTypeIds(
        coupon.getProductTypeIds() != null
            ? new HashSet<>(coupon.getProductTypeIds())
            : new HashSet<>());
    response.setDateStart(coupon.getDateStart());
    response.setDateEnd(coupon.getDateEnd());
    response.setIsActive(coupon.getIsActive());
    response.setCreatedAt(coupon.getCreatedAt());
    response.setUpdatedAt(coupon.getUpdatedAt());
    response.setLifecycleStatus(resolveLifecycleStatus(coupon));
    return response;
  }

  private CouponDto.PublicResponse mapToPublicResponse(Coupon coupon) {
    CouponDto.PublicResponse response = new CouponDto.PublicResponse();
    response.setCode(coupon.getCode());
    response.setName(coupon.getName());
    response.setDescription(coupon.getDescription());
    response.setDiscountType(coupon.getDiscountType().name());
    response.setDiscountValue(coupon.getDiscountValue());
    response.setMinOrderValue(coupon.getMinOrderValue());
    response.setMaxDiscountAmount(coupon.getMaxDiscountAmount());
    response.setDateEnd(coupon.getDateEnd());
    return response;
  }

  private CouponDto.UsageOrderSummary mapToUsageSummary(Order order) {
    CouponDto.UsageOrderSummary summary = new CouponDto.UsageOrderSummary();
    summary.setOrderId(order.getId());
    summary.setOrderCode(order.getOrderCode());
    if (order.getUser() != null) {
      summary.setCustomerName(order.getUser().getFullName());
      summary.setCustomerUsername(order.getUser().getUsername());
    }
    summary.setDiscountAmount(order.getDiscountAmount());
    summary.setTotalAmount(order.getTotalAmount());
    summary.setOrderStatus(order.getStatus() != null ? order.getStatus().name() : null);
    summary.setOrderDate(order.getOrderDate());
    return summary;
  }
}
