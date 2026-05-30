package com.cdweb.be.service;

import com.cdweb.be.dto.GHNDto;
import com.cdweb.be.dto.OrderDto;
import com.cdweb.be.entity.*;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.*;
import com.cdweb.be.service.payment.PaymentService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class OrderService {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OrderService.class);

  @Autowired private UserRepository userRepository;

  @Autowired private CartRepository cartRepository;

  @Autowired private CartItemRepository cartItemRepository;

  @Autowired private CouponRepository couponRepository;

  @Autowired private OrderRepository orderRepository;

  @Autowired private OrderDetailRepository orderDetailRepository;

  @Autowired private ProductVariantRepository productVariantRepository;

  @Autowired private AddressRepository addressRepository;

  @Autowired @Lazy private PaymentService paymentService;

  @Autowired private GhnService ghnService;

  @Autowired private OrderItemRepository orderItemRepository;

  @Autowired private ProductItemRepository productItemRepository;

  @Autowired private UserInteractionRepository userInteractionRepository;

  @Autowired private OrderStatusHistoryRepository orderStatusHistoryRepository;

  @Autowired private EmailService emailService;

  @Autowired private CartService cartService;

  @Value("${app.server.url:http://localhost:8080}")
  private String serverUrl;

  // Fallback khi GHN API không có địa chỉ GHN (districtId/wardCode)
  private static final BigDecimal DEFAULT_SHIPPING_FEE = new BigDecimal("30000");
  // Giữ lại cho previewCoupon (không cần địa chỉ GHN)
  private static final BigDecimal FREE_SHIPPING_THRESHOLD_FALLBACK = new BigDecimal("500000");

  // ═══════════════════════════════════════════════════════════════════════════
  // CHECKOUT — tạo đơn hàng từ giỏ hàng
  // ═══════════════════════════════════════════════════════════════════════════
  @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
  public OrderDto.OrderResponse checkout(String username, OrderDto.CheckoutRequest request) {
    User user = findUser(username);

    // 1. Lấy giỏ hàng
    Cart cart =
        cartRepository
            .findByUserId(user.getId())
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Giỏ hàng trống, vui lòng thêm sản phẩm trước khi đặt hàng"));

    List<CartItem> items = cart.getCartItems();
    if (items == null || items.isEmpty()) {
      throw new BadRequestException("Giỏ hàng trống, vui lòng thêm sản phẩm trước khi đặt hàng");
    }

    // 2. Kiểm tra tồn kho
    for (CartItem item : items) {
      ProductVariant variant = item.getVariant();
      if (variant.getStockQuantity() == null || variant.getStockQuantity() < item.getQuantity()) {
        int available = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
        throw new BadRequestException(
            "Sản phẩm \""
                + variant.getVariantName()
                + "\" không đủ hàng. Còn lại: "
                + available
                + ", yêu cầu: "
                + item.getQuantity());
      }
    }

    // 3. Xác định địa chỉ giao hàng
    String shippingName,
        shippingPhone,
        shippingAddress,
        shippingProvince,
        shippingDistrict,
        shippingWard;
    UserAddress savedAddress = null;

    if (request.getAddressId() != null) {
      savedAddress =
          addressRepository
              .findByIdAndUserId(request.getAddressId(), user.getId())
              .orElseThrow(
                  () -> new ResourceNotFoundException("UserAddress", "id", request.getAddressId()));
      shippingName = savedAddress.getReceiverName();
      shippingPhone = savedAddress.getPhone();
      shippingAddress = savedAddress.getAddressDetail();
      shippingProvince = savedAddress.getProvince();
      shippingDistrict = savedAddress.getDistrict();
      shippingWard = savedAddress.getWard();
    } else {
      if (request.getShippingName() == null
          || request.getShippingName().isBlank()
          || request.getShippingPhone() == null
          || request.getShippingPhone().isBlank()
          || request.getShippingAddress() == null
          || request.getShippingAddress().isBlank()) {
        throw new BadRequestException(
            "Vui lòng cung cấp addressId hoặc thông tin giao hàng (shippingName, shippingPhone, shippingAddress)");
      }
      shippingName = request.getShippingName();
      shippingPhone = request.getShippingPhone();
      shippingAddress = request.getShippingAddress();
      shippingProvince = request.getShippingProvince();
      shippingDistrict = request.getShippingDistrict();
      shippingWard = request.getShippingWard();
    }

    // 4. Validate phương thức thanh toán
    Order.PaymentMethod paymentMethod;
    try {
      paymentMethod = Order.PaymentMethod.valueOf(request.getPaymentMethod().toUpperCase());
    } catch (Exception e) {
      throw new BadRequestException(
          "Phương thức thanh toán không hợp lệ. Chấp nhận: COD, BANK_TRANSFER, MOMO, VNPAY, ZALOPAY");
    }

    // 5. Tính subtotal
    BigDecimal subtotal = BigDecimal.ZERO;
    for (CartItem item : items) {
      BigDecimal price =
          item.getUnitPrice() != null ? item.getUnitPrice() : item.getVariant().getPrice();
      subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    // 6. Áp mã giảm giá
    BigDecimal discountAmount = BigDecimal.ZERO;
    Coupon coupon = null;
    String couponCodeApplied = null;

    if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
      String code = request.getCouponCode().trim().toUpperCase();
      coupon =
          couponRepository
              .findActiveCouponByCode(code, LocalDateTime.now())
              .orElseThrow(
                  () -> new BadRequestException("Mã giảm giá không hợp lệ hoặc đã hết hạn"));

      if (!Boolean.TRUE.equals(coupon.getIsActive())) {
        throw new BadRequestException("Mã giảm giá đã bị vô hiệu hóa");
      }
      if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
        throw new BadRequestException("Mã giảm giá đã đạt giới hạn sử dụng");
      }
      if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
        throw new BadRequestException(
            "Đơn hàng tối thiểu "
                + coupon.getMinOrderValue().toPlainString()
                + " VNĐ để sử dụng mã này");
      }

      if (coupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
        discountAmount =
            subtotal
                .multiply(coupon.getDiscountValue())
                .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
        if (coupon.getMaxDiscountAmount() != null
            && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
          discountAmount = coupon.getMaxDiscountAmount();
        }
      } else { // FIXED
        discountAmount = coupon.getDiscountValue();
        if (discountAmount.compareTo(subtotal) > 0) {
          discountAmount = subtotal;
        }
      }
      couponCodeApplied = coupon.getCode();
    }

    // 7. Phí vận chuyển — tính động từ GHN nếu có địa chỉ GHN
    BigDecimal shippingFee =
        ghnService.calculateShippingFeeForOrder(
            request.getToDistrictId(), request.getToWardCode(), subtotal);

    // 8. Tổng tiền
    BigDecimal totalAmount = subtotal.subtract(discountAmount).add(shippingFee);

    // 9. Sinh mã đơn hàng
    String orderCode =
        "ORD-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

    // 10. Tạo Order
    Order order = new Order();
    order.setOrderCode(orderCode);
    order.setUser(user);
    order.setShippingName(shippingName);
    order.setShippingPhone(shippingPhone);
    order.setShippingAddress(shippingAddress);
    order.setShippingProvince(shippingProvince);
    order.setShippingDistrict(shippingDistrict);
    order.setShippingWard(shippingWard);
    order.setShippingFee(shippingFee);
    order.setPaymentMethod(paymentMethod);
    order.setPaymentStatus(Order.PaymentStatus.PENDING);
    order.setStatus(Order.OrderStatus.PENDING);
    order.setSubtotal(subtotal);
    order.setDiscountAmount(discountAmount);
    order.setTotalAmount(totalAmount);
    order.setCouponCode(couponCodeApplied);
    order.setCoupon(coupon);
    order.setNote(request.getNote());
    // Lưu GHN Address IDs để dùng khi tạo vận đơn
    order.setToDistrictId(request.getToDistrictId());
    order.setToWardCode(request.getToWardCode());
    if (savedAddress != null) {
      order.setUserAddress(savedAddress);
    }

    Order savedOrder = orderRepository.save(order);

    // 11. Tạo OrderDetail cho mỗi CartItem
    List<OrderDetail> orderDetails = new ArrayList<>();
    for (CartItem cartItem : items) {
      ProductVariant variant = cartItem.getVariant();
      Product product = variant.getProduct();
      BigDecimal price =
          cartItem.getUnitPrice() != null ? cartItem.getUnitPrice() : variant.getPrice();

      OrderDetail detail = new OrderDetail();
      detail.setOrder(savedOrder);
      detail.setVariant(variant);
      detail.setProductName(product.getName());
      detail.setVariantName(variant.getVariantName());
      detail.setSkuCode(variant.getSkuCode());
      detail.setQuantity(cartItem.getQuantity());
      detail.setUnitPrice(price);
      detail.setDiscountAmount(BigDecimal.ZERO);
      detail.setTotalPrice(price.multiply(BigDecimal.valueOf(cartItem.getQuantity())));
      orderDetails.add(detail);
    }
    orderDetailRepository.saveAll(orderDetails);

    // 12. Trừ tồn kho
    for (CartItem cartItem : items) {
      ProductVariant variant = cartItem.getVariant();
      variant.setStockQuantity(variant.getStockQuantity() - cartItem.getQuantity());
      productVariantRepository.save(variant);
    }

    // 13. Cập nhật số lần dùng coupon
    if (coupon != null) {
      coupon.setUsedCount(coupon.getUsedCount() + 1);
      couponRepository.save(coupon);
    }

    // 14. Xóa giỏ hàng
    cartItemRepository.deleteAllByUserId(user.getId());

    // 💡 Tự động lưu phân mảnh PURCHASE interaction vào Analytics Insights
    try {
      for (CartItem cartItem : items) {
        userInteractionRepository.save(
            com.cdweb.be.entity.UserInteraction.builder()
                .userId(user.getId())
                .productId(cartItem.getVariant().getProduct().getId())
                .actionType("PURCHASE")
                .interactionScore(BigDecimal.valueOf(5.0))
                .createdAt(LocalDateTime.now())
                .build());
      }
    } catch (Exception e) {
      log.error("Lỗi khi lưu UserInteraction tracker cho đơn hàng: {}", e.getMessage());
    }

    // 15. Lịch sử trạng thái ban đầu + email xác nhận đặt hàng
    saveOrderHistory(savedOrder, Order.OrderStatus.PENDING, "Đặt hàng thành công", user);
    try {
      if (user.getEmail() != null && !user.getEmail().isBlank()) {
        String totalFormatted =
            savedOrder.getTotalAmount().toPlainString() + " VNĐ";
        emailService.sendOrderConfirmationEmail(
            user.getEmail(),
            user.getFullName() != null ? user.getFullName() : user.getUsername(),
            savedOrder.getOrderCode(),
            totalFormatted,
            paymentMethod.name());
      }
    } catch (Exception e) {
      log.warn("Gửi email xác nhận đơn {} thất bại: {}", savedOrder.getOrderCode(), e.getMessage());
    }

    // 16. Nếu thanh toán online → tạo Payment URL qua Payment Gateway
    if (paymentMethod == Order.PaymentMethod.VNPAY
        || paymentMethod == Order.PaymentMethod.MOMO
        || paymentMethod == Order.PaymentMethod.ZALOPAY) {
      try {
        paymentService.createPayment(savedOrder, "127.0.0.1", null, "vn");
        // Reload order từ DB vì PaymentService đã cập nhật order
        savedOrder = orderRepository.findById(savedOrder.getId()).orElse(savedOrder);
      } catch (Exception e) {
        // Nếu tạo payment URL thất bại → vẫn giữ đơn hàng, user có thể retry sau
        log.error("Lỗi khi tạo payment URL trong lúc checkout: {}", e.getMessage(), e);
        savedOrder.setPaymentStatus(Order.PaymentStatus.PENDING);
        orderRepository.save(savedOrder);
      }
    }

    return mapToOrderResponse(savedOrder, orderDetails);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PREVIEW COUPON — xem giảm giá mà chưa tạo đơn
  // ═══════════════════════════════════════════════════════════════════════════
  @Transactional(readOnly = true)
  public OrderDto.ApplyCouponResponse previewCoupon(String username, String couponCode) {
    User user = findUser(username);

    Cart cart =
        cartRepository
            .findByUserId(user.getId())
            .orElseThrow(() -> new BadRequestException("Giỏ hàng trống"));

    List<CartItem> items = cart.getCartItems();
    if (items == null || items.isEmpty()) {
      throw new BadRequestException("Giỏ hàng trống");
    }

    BigDecimal subtotal = BigDecimal.ZERO;
    for (CartItem item : items) {
      BigDecimal price =
          item.getUnitPrice() != null ? item.getUnitPrice() : item.getVariant().getPrice();
      subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));
    }

    String code = couponCode.trim().toUpperCase();
    Coupon coupon =
        couponRepository
            .findActiveCouponByCode(code, LocalDateTime.now())
            .orElseThrow(() -> new BadRequestException("Mã giảm giá không hợp lệ hoặc đã hết hạn"));

    if (!Boolean.TRUE.equals(coupon.getIsActive())) {
      throw new BadRequestException("Mã giảm giá đã bị vô hiệu hóa");
    }
    if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
      throw new BadRequestException("Mã giảm giá đã đạt giới hạn sử dụng");
    }
    if (coupon.getMinOrderValue() != null && subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
      throw new BadRequestException(
          "Đơn hàng tối thiểu "
              + coupon.getMinOrderValue().toPlainString()
              + " VNĐ để sử dụng mã này");
    }

    BigDecimal discountAmount;
    if (coupon.getDiscountType() == Coupon.DiscountType.PERCENT) {
      discountAmount =
          subtotal
              .multiply(coupon.getDiscountValue())
              .divide(BigDecimal.valueOf(100), 0, RoundingMode.HALF_UP);
      if (coupon.getMaxDiscountAmount() != null
          && discountAmount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
        discountAmount = coupon.getMaxDiscountAmount();
      }
    } else {
      discountAmount = coupon.getDiscountValue();
      if (discountAmount.compareTo(subtotal) > 0) {
        discountAmount = subtotal;
      }
    }

    // previewCoupon: không có địa chỉ GHN → dùng fallback cố định
    BigDecimal shippingFee =
        subtotal.compareTo(FREE_SHIPPING_THRESHOLD_FALLBACK) >= 0
            ? BigDecimal.ZERO
            : DEFAULT_SHIPPING_FEE;
    BigDecimal finalAmount = subtotal.subtract(discountAmount).add(shippingFee);

    OrderDto.ApplyCouponResponse response = new OrderDto.ApplyCouponResponse();
    response.setCouponCode(coupon.getCode());
    response.setDiscountType(coupon.getDiscountType().name());
    response.setDiscountValue(coupon.getDiscountValue());
    response.setOriginalSubtotal(subtotal);
    response.setDiscountAmount(discountAmount);
    response.setFinalAmount(finalAmount);
    response.setMessage(
        "Áp dụng mã \""
            + coupon.getCode()
            + "\" thành công, giảm "
            + discountAmount.toPlainString()
            + " VNĐ");
    return response;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GET MY ORDERS — danh sách đơn hàng của user
  // ═══════════════════════════════════════════════════════════════════════════
  @Transactional(readOnly = true)
  public Page<OrderDto.OrderSummaryResponse> getMyOrders(
      String username, String status, Pageable pageable) {
    User user = findUser(username);

    Page<Order> orders;
    if (status != null && !status.isBlank()) {
      try {
        Order.OrderStatus orderStatus = Order.OrderStatus.valueOf(status.toUpperCase());
        orders = orderRepository.findByUserIdAndStatus(user.getId(), orderStatus, pageable);
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Trạng thái không hợp lệ: " + status);
      }
    } else {
      orders = orderRepository.findByUserId(user.getId(), pageable);
    }

    return orders.map(this::mapToOrderSummary);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GET ORDER DETAIL — chi tiết đơn hàng theo orderCode
  // ═══════════════════════════════════════════════════════════════════════════
  @Transactional(readOnly = true)
  public OrderDto.OrderResponse getOrderByCode(String username, String orderCode) {
    User user = findUser(username);

    Order order =
        orderRepository
            .findByOrderCode(orderCode)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", orderCode));

    // Chỉ cho xem đơn hàng của mình
    if (!order.getUser().getId().equals(user.getId())) {
      throw new ResourceNotFoundException("Order", "orderCode", orderCode);
    }

    List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
    return mapToOrderResponse(order, details);
  }

  /** Thêm lại toàn bộ sản phẩm của đơn vào giỏ hàng. */
  public com.cdweb.be.dto.CartDto.CartResponse reorder(String username, String orderCode) {
    User user = findUser(username);
    Order order =
        orderRepository
            .findByOrderCode(orderCode)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", orderCode));
    if (!order.getUser().getId().equals(user.getId())) {
      throw new ResourceNotFoundException("Order", "orderCode", orderCode);
    }

    List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
    if (details.isEmpty()) {
      throw new BadRequestException("Đơn hàng không có sản phẩm để đặt lại");
    }

    for (OrderDetail d : details) {
      if (d.getVariant() == null) {
        continue;
      }
      com.cdweb.be.dto.CartDto.AddItemRequest req = new com.cdweb.be.dto.CartDto.AddItemRequest();
      req.setVariantId(d.getVariant().getId());
      req.setQuantity(d.getQuantity());
      cartService.addItem(username, req);
    }

    return cartService.getCart(username);
  }

  /** In nhãn vận đơn GHN (admin). */
  @Transactional(readOnly = true)
  public com.cdweb.be.dto.GHNDto.PrintLabelResponse getGhnPrintLabel(Integer orderId) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    if (order.getGhnOrderCode() == null || order.getGhnOrderCode().isBlank()) {
      throw new BadRequestException("Đơn chưa có mã vận đơn GHN");
    }
    return ghnService.generatePrintLabel(order.getGhnOrderCode());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CANCEL ORDER — hủy đơn hàng (chỉ PENDING / CONFIRMED)
  // ═══════════════════════════════════════════════════════════════════════════
  public OrderDto.OrderResponse cancelOrder(
      String username, Integer orderId, OrderDto.CancelRequest request) {
    User user = findUser(username);

    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

    if (!order.getUser().getId().equals(user.getId())) {
      throw new ResourceNotFoundException("Order", "id", orderId);
    }

    if (order.getStatus() != Order.OrderStatus.PENDING
        && order.getStatus() != Order.OrderStatus.CONFIRMED) {
      throw new BadRequestException(
          "Chỉ có thể hủy đơn hàng ở trạng thái PENDING hoặc CONFIRMED. Trạng thái hiện tại: "
              + order.getStatus());
    }

    // Hoàn lại tồn kho
    List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
    for (OrderDetail detail : details) {
      ProductVariant variant = detail.getVariant();
      variant.setStockQuantity(variant.getStockQuantity() + detail.getQuantity());
      productVariantRepository.save(variant);
    }

    // Hoàn lại coupon
    if (order.getCoupon() != null) {
      Coupon coupon = order.getCoupon();
      coupon.setUsedCount(Math.max(0, coupon.getUsedCount() - 1));
      couponRepository.save(coupon);
    }

    order.setStatus(Order.OrderStatus.CANCELLED);
    order.setCancelledAt(LocalDateTime.now());
    order.setCancelReason(request != null ? request.getReason() : null);
    orderRepository.save(order);

    return mapToOrderResponse(order, details);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  // ═══════════════════════════════════════════════════════════════════════════
  // ADMIN — Quản lý đơn hàng
  // ═══════════════════════════════════════════════════════════════════════════

  /** Admin xem tất cả đơn hàng — lọc theo status, tìm kiếm theo keyword */
  @Transactional(readOnly = true)
  public Page<OrderDto.AdminOrderSummaryResponse> adminGetAllOrders(
      String status, String keyword, Pageable pageable) {
    Page<Order> orders;

    if (keyword != null && !keyword.isBlank()) {
      orders = orderRepository.searchOrders(keyword.trim(), pageable);
      // Nếu có cả status thì lọc thêm
      if (status != null && !status.isBlank()) {
        Order.OrderStatus orderStatus = parseStatus(status);
        orders =
            orderRepository.searchOrdersByKeywordAndStatus(keyword.trim(), orderStatus, pageable);
      }
    } else if (status != null && !status.isBlank()) {
      Order.OrderStatus orderStatus = parseStatus(status);
      orders = orderRepository.findByStatus(orderStatus, pageable);
    } else {
      orders = orderRepository.findAllActive(pageable);
    }

    return orders.map(this::mapToAdminOrderSummary);
  }

  /** Admin xem chi tiết đơn hàng (bất kỳ đơn nào) */
  @Transactional(readOnly = true)
  public OrderDto.AdminOrderResponse adminGetOrderById(Integer orderId) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
    return mapToAdminOrderResponse(order, details);
  }

  /**
   * Admin cập nhật trạng thái đơn hàng Flow hợp lệ: PENDING → CONFIRMED → PROCESSING → SHIPPING →
   * DELIVERED → COMPLETED Bất kỳ (trừ COMPLETED) → CANCELLED CANCELLED / DELIVERED → REFUNDED
   */
  public OrderDto.AdminOrderResponse adminUpdateOrderStatus(
      Integer orderId, OrderDto.UpdateStatusRequest request) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

    Order.OrderStatus newStatus = parseStatus(request.getStatus());
    Order.OrderStatus currentStatus = order.getStatus();

    // Validate chuyển trạng thái hợp lệ
    validateStatusTransition(currentStatus, newStatus);

    // Xử lý logic theo trạng thái mới
    switch (newStatus) {
      case CONFIRMED:
        order.setConfirmedAt(LocalDateTime.now());
        break;
      case SHIPPING:
        order.setShippedAt(LocalDateTime.now());
        // Tự động tạo vận đơn GHN nếu có địa chỉ GHN
        if (order.getToDistrictId() != null && order.getToWardCode() != null) {
          try {
            // Tính COD: nếu COD và chưa thanh toán thì thu hộ, ngược lại = 0
            long codAmount = 0;
            if (order.getPaymentMethod() == Order.PaymentMethod.COD
                && order.getPaymentStatus() != Order.PaymentStatus.PAID) {
              codAmount = order.getTotalAmount().longValue();
            }
            // Tên hiển thị trên vận đơn
            List<OrderDetail> orderDetailsList = orderDetailRepository.findByOrderId(order.getId());
            String itemName =
                orderDetailsList.isEmpty()
                    ? "Đơn hàng Bảo Khang Gadget"
                    : orderDetailsList.get(0).getProductName();
            int totalQty = orderDetailsList.stream().mapToInt(OrderDetail::getQuantity).sum();

            GHNDto.CreateOrderResponse ghnResult =
                ghnService.createShippingOrder(
                    order.getOrderCode(),
                    order.getShippingName(),
                    order.getShippingPhone(),
                    order.getShippingAddress(),
                    order.getToWardCode(),
                    order.getToDistrictId(),
                    codAmount,
                    order.getTotalAmount().longValue(),
                    itemName,
                    totalQty);
            order.setTrackingCode(ghnResult.getOrderCode());
            order.setGhnOrderCode(ghnResult.getOrderCode());
            log.info(
                "Auto-created GHN order {} for internal order {}",
                ghnResult.getOrderCode(),
                order.getOrderCode());
          } catch (Exception e) {
            log.warn(
                "GHN auto-create failed for order {}: {}. Falling back to manual tracking.",
                order.getOrderCode(),
                e.getMessage());
            // Fallback: dùng tracking code thủ công nếu GHN tạo lỗi
            if (request.getTrackingCode() != null && !request.getTrackingCode().isBlank()) {
              order.setTrackingCode(request.getTrackingCode());
            }
          }
        } else {
          // Không có GHN address → dùng tracking code thủ công
          if (request.getTrackingCode() != null && !request.getTrackingCode().isBlank()) {
            order.setTrackingCode(request.getTrackingCode());
          }
        }
        break;
      case DELIVERED:
        order.setDeliveredAt(LocalDateTime.now());
        // COD: tự động đánh dấu đã thanh toán
        if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
          order.setPaymentStatus(Order.PaymentStatus.PAID);
        }
        // Kích hoạt bảo hành cho tất cả máy đã gán IMEI trong đơn
        activateWarrantyForOrder(order.getId());
        break;
      case CANCELLED:
        order.setCancelledAt(LocalDateTime.now());
        order.setCancelReason(
            request.getCancelReason() != null ? request.getCancelReason() : "Admin hủy đơn");
        // Hủy vận đơn GHN nếu đã tạo
        if (order.getGhnOrderCode() != null && !order.getGhnOrderCode().isBlank()) {
          try {
            ghnService.cancelShippingOrder(List.of(order.getGhnOrderCode()));
            log.info(
                "Auto-cancelled GHN order {} for internal order {}",
                order.getGhnOrderCode(),
                order.getOrderCode());
          } catch (Exception e) {
            log.warn(
                "GHN auto-cancel failed for order {}: {}", order.getOrderCode(), e.getMessage());
          }
        }
        // Hoàn kho
        restoreStock(order.getId());
        // Hoàn coupon
        restoreCoupon(order);
        break;
      case REFUNDED:
        order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        // Hoàn kho nếu chưa hoàn (chỉ khi từ DELIVERED)
        if (currentStatus == Order.OrderStatus.DELIVERED
            || currentStatus == Order.OrderStatus.COMPLETED) {
          restoreStock(order.getId());
        }
        break;
      default:
        break;
    }

    order.setStatus(newStatus);

    // Cập nhật admin note nếu có
    if (request.getAdminNote() != null) {
      order.setAdminNote(request.getAdminNote());
    }

    orderRepository.save(order);

    List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
    return mapToAdminOrderResponse(order, details);
  }

  /** Admin cập nhật trạng thái thanh toán */
  public OrderDto.AdminOrderResponse adminUpdatePaymentStatus(
      Integer orderId, OrderDto.UpdatePaymentStatusRequest request) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

    Order.PaymentStatus newPaymentStatus;
    try {
      newPaymentStatus = Order.PaymentStatus.valueOf(request.getPaymentStatus().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "Trạng thái thanh toán không hợp lệ: "
              + request.getPaymentStatus()
              + ". Chấp nhận: PENDING, PAID, FAILED, REFUNDED");
    }

    order.setPaymentStatus(newPaymentStatus);
    orderRepository.save(order);

    List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
    return mapToAdminOrderResponse(order, details);
  }

  /** Admin hủy đơn hàng (mạnh hơn user — có thể hủy ở nhiều trạng thái hơn) */
  public OrderDto.AdminOrderResponse adminCancelOrder(
      Integer orderId, OrderDto.CancelRequest request) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

    if (order.getStatus() == Order.OrderStatus.COMPLETED) {
      throw new BadRequestException(
          "Không thể hủy đơn hàng đã hoàn thành. Hãy dùng trạng thái REFUNDED.");
    }
    if (order.getStatus() == Order.OrderStatus.CANCELLED) {
      throw new BadRequestException("Đơn hàng đã bị hủy trước đó");
    }

    // Hoàn kho + coupon
    if (order.getStatus() != Order.OrderStatus.REFUNDED) {
      restoreStock(order.getId());
      restoreCoupon(order);
    }

    order.setStatus(Order.OrderStatus.CANCELLED);
    order.setCancelledAt(LocalDateTime.now());
    order.setCancelReason(
        request != null && request.getReason() != null ? request.getReason() : "Admin hủy đơn");
    orderRepository.save(order);

    List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
    return mapToAdminOrderResponse(order, details);
  }

  /** Thống kê đơn hàng theo trạng thái */
  @Transactional(readOnly = true)
  public OrderDto.OrderStatsResponse getOrderStats() {
    OrderDto.OrderStatsResponse stats = new OrderDto.OrderStatsResponse();
    stats.setTotalOrders(orderRepository.count());
    stats.setPendingOrders(orderRepository.countByStatus(Order.OrderStatus.PENDING));
    stats.setConfirmedOrders(orderRepository.countByStatus(Order.OrderStatus.CONFIRMED));
    stats.setProcessingOrders(orderRepository.countByStatus(Order.OrderStatus.PROCESSING));
    stats.setShippingOrders(orderRepository.countByStatus(Order.OrderStatus.SHIPPING));
    stats.setDeliveredOrders(orderRepository.countByStatus(Order.OrderStatus.DELIVERED));
    stats.setCompletedOrders(orderRepository.countByStatus(Order.OrderStatus.COMPLETED));
    stats.setCancelledOrders(orderRepository.countByStatus(Order.OrderStatus.CANCELLED));
    stats.setRefundedOrders(orderRepository.countByStatus(Order.OrderStatus.REFUNDED));
    stats.setHiddenOrders(orderRepository.countHiddenOrders()); // số đơn đang bị ẩn
    return stats;
  }

  /**
   * Admin ẩn / hiện đơn hàng (Soft Delete toggle) - hidden = true → ẩn khỏi danh sách - hidden =
   * false → hiện lại
   */
  public OrderDto.AdminOrderResponse adminToggleOrderVisibility(
      Integer orderId, OrderDto.UpdateVisibilityRequest request) {

    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

    boolean willHide = Boolean.TRUE.equals(request.getHidden());
    order.setIsHidden(willHide);

    if (willHide) {
      order.setHiddenAt(LocalDateTime.now());
      order.setHiddenReason(request.getReason() != null ? request.getReason() : null);
    } else {
      // Khi unhide, xóa thông tin ẩn
      order.setHiddenAt(null);
      order.setHiddenReason(null);
    }

    orderRepository.save(order);

    List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
    return mapToAdminOrderResponse(order, details);
  }

  /** Admin xem danh sách đơn hàng đã bị ẩn */
  @Transactional(readOnly = true)
  public Page<OrderDto.AdminOrderSummaryResponse> adminGetHiddenOrders(Pageable pageable) {
    return orderRepository.findHiddenOrders(pageable).map(this::mapToAdminOrderSummary);
  }

  // ─── Admin helper: validate chuyển trạng thái ─────────────────────────────
  private void validateStatusTransition(Order.OrderStatus current, Order.OrderStatus newStatus) {
    if (current == newStatus) {
      throw new BadRequestException("Đơn hàng đã ở trạng thái " + current);
    }

    boolean valid =
        switch (current) {
          case PENDING ->
              newStatus == Order.OrderStatus.CONFIRMED || newStatus == Order.OrderStatus.CANCELLED;
          case CONFIRMED ->
              newStatus == Order.OrderStatus.PROCESSING || newStatus == Order.OrderStatus.CANCELLED;
          case PROCESSING ->
              newStatus == Order.OrderStatus.SHIPPING || newStatus == Order.OrderStatus.CANCELLED;
          case SHIPPING ->
              newStatus == Order.OrderStatus.DELIVERED || newStatus == Order.OrderStatus.CANCELLED;
          case DELIVERED ->
              newStatus == Order.OrderStatus.COMPLETED || newStatus == Order.OrderStatus.REFUNDED;
          case COMPLETED -> newStatus == Order.OrderStatus.REFUNDED;
          case CANCELLED -> newStatus == Order.OrderStatus.REFUNDED;
          case REFUNDED -> false;
        };

    if (!valid) {
      throw new BadRequestException(
          "Không thể chuyển từ "
              + current
              + " sang "
              + newStatus
              + ". Flow: PENDING → CONFIRMED → PROCESSING → SHIPPING → DELIVERED → COMPLETED");
    }
  }

  // ─── Admin helper: parse status string ────────────────────────────────────
  private Order.OrderStatus parseStatus(String status) {
    try {
      return Order.OrderStatus.valueOf(status.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "Trạng thái không hợp lệ: "
              + status
              + ". Chấp nhận: PENDING, CONFIRMED, PROCESSING, SHIPPING, DELIVERED, COMPLETED, CANCELLED, REFUNDED");
    }
  }

  // ─── Admin helper: hoàn kho ───────────────────────────────────────────────
  private void restoreStock(Integer orderId) {
    List<OrderDetail> details = orderDetailRepository.findByOrderId(orderId);
    for (OrderDetail detail : details) {
      ProductVariant variant = detail.getVariant();
      if (variant != null) {
        variant.setStockQuantity(variant.getStockQuantity() + detail.getQuantity());
        productVariantRepository.save(variant);
      }
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GÁN IMEI / SERIAL CHO ĐƠN HÀNG (Xuất kho)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Admin gán mã IMEI/Serial thực tế cho một dòng chi tiết đơn hàng. Nghiệp vụ: - Kiểm tra đơn hàng
   * ở trạng thái cho phép gán (CONFIRMED / PROCESSING). - Kiểm tra số IMEI gán không vượt quá số
   * lượng đã đặt. - Mỗi IMEI phải đang AVAILABLE và thuộc đúng variant đã đặt. - Gán xong → cập
   * nhật ProductItem => RESERVED (chờ giao) hoặc SOLD. - Lưu vào bảng order_item_serials.
   */
  public OrderDto.AdminOrderResponse assignImeiToOrder(
      Integer orderId, OrderDto.AssignImeiRequest request) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

    // Chỉ cho gán khi đơn đang ở CONFIRMED hoặc PROCESSING
    if (order.getStatus() != Order.OrderStatus.CONFIRMED
        && order.getStatus() != Order.OrderStatus.PROCESSING) {
      throw new BadRequestException(
          "Chỉ có thể gán IMEI khi đơn hàng ở trạng thái CONFIRMED hoặc PROCESSING. "
              + "Trạng thái hiện tại: "
              + order.getStatus());
    }

    OrderDetail orderDetail =
        orderDetailRepository
            .findById(request.getOrderDetailId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("OrderDetail", "id", request.getOrderDetailId()));

    // Kiểm tra orderDetail thuộc đúng order này
    if (!orderDetail.getOrder().getId().equals(orderId)) {
      throw new BadRequestException("Chi tiết đơn hàng không thuộc đơn hàng #" + orderId);
    }

    // Kiểm tra số lượng: đã gán + đang gán mới <= quantity
    long alreadyAssigned = orderItemRepository.countByOrderDetailId(orderDetail.getId());
    if (alreadyAssigned + request.getImeis().size() > orderDetail.getQuantity()) {
      throw new BadRequestException(
          "Số lượng IMEI vượt quá số lượng đặt hàng. Đã gán: "
              + alreadyAssigned
              + ", đang gán thêm: "
              + request.getImeis().size()
              + ", tối đa: "
              + orderDetail.getQuantity());
    }

    Integer expectedVariantId = orderDetail.getVariant().getId();

    for (String imeiOrSerial : request.getImeis()) {
      // Tìm ProductItem theo IMEI hoặc Serial
      ProductItem productItem =
          productItemRepository
              .findByImeiOrSerialNumber(imeiOrSerial, imeiOrSerial)
              .orElseThrow(
                  () -> new ResourceNotFoundException("ProductItem", "IMEI/Serial", imeiOrSerial));

      // Kiểm tra máy thuộc đúng variant
      if (!productItem.getVariant().getId().equals(expectedVariantId)) {
        throw new BadRequestException(
            "IMEI/Serial \""
                + imeiOrSerial
                + "\" thuộc mã hàng khác ("
                + productItem.getVariant().getVariantName()
                + "), không khớp với dòng đặt hàng ("
                + orderDetail.getVariantName()
                + ")");
      }

      // Kiểm tra máy đang AVAILABLE
      if (productItem.getStatus() != ProductItem.ProductItemStatus.AVAILABLE) {
        throw new BadRequestException(
            "IMEI/Serial \""
                + imeiOrSerial
                + "\" không khả dụng. "
                + "Trạng thái hiện tại: "
                + productItem.getStatus());
      }

      // Tạo liên kết order_item_serials
      OrderItem orderItem = new OrderItem();
      orderItem.setOrderDetail(orderDetail);
      orderItem.setProductItem(productItem);
      orderItemRepository.save(orderItem);

      // Đánh dấu máy là RESERVED (đang giữ cho đơn, chờ giao)
      productItem.setStatus(ProductItem.ProductItemStatus.RESERVED);
      productItem.setSoldAt(LocalDateTime.now());
      productItemRepository.save(productItem);
    }

    log.info(
        "Đã gán {} IMEI/Serial cho OrderDetail #{} (Order #{})",
        request.getImeis().size(),
        orderDetail.getId(),
        orderId);

    List<OrderDetail> details = orderDetailRepository.findByOrderId(order.getId());
    return mapToAdminOrderResponse(order, details);
  }

  /**
   * Kích hoạt bảo hành cho tất cả máy đã gán IMEI khi đơn DELIVERED. - Chuyển trạng thái
   * ProductItem từ RESERVED → SOLD. - Gán warrantyStartDate = ngày giao hàng.
   */
  public void activateWarrantyForOrder(Integer orderId) {
    List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
    LocalDate today = LocalDate.now();

    for (OrderItem oi : orderItems) {
      ProductItem productItem = oi.getProductItem();
      if (productItem.getStatus() == ProductItem.ProductItemStatus.RESERVED
          || productItem.getStatus() == ProductItem.ProductItemStatus.AVAILABLE) {
        productItem.setStatus(ProductItem.ProductItemStatus.SOLD);
        productItem.setSoldAt(LocalDateTime.now());

        // Kích hoạt bảo hành
        if (productItem.getWarrantyStartDate() == null) {
          productItem.setWarrantyStartDate(today);
          // warrantyMonths mặc định đã là 12 trong Entity
          // Lấy từ OrderDetail nếu có giá trị riêng
          OrderDetail od = oi.getOrderDetail();
          if (od.getWarrantyMonths() != null) {
            productItem.setWarrantyMonths(od.getWarrantyMonths());
          }
        }
        productItemRepository.save(productItem);
      }
    }

    if (!orderItems.isEmpty()) {
      log.info("Đã kích hoạt bảo hành cho {} máy trong đơn hàng #{}", orderItems.size(), orderId);
    }
  }

  // ─── Admin helper: hoàn coupon ────────────────────────────────────────────
  private void restoreCoupon(Order order) {
    if (order.getCoupon() != null) {
      Coupon coupon = order.getCoupon();
      coupon.setUsedCount(Math.max(0, coupon.getUsedCount() - 1));
      couponRepository.save(coupon);
    }
  }

  // ─── Admin mapper: Order → AdminOrderResponse ─────────────────────────────
  private OrderDto.AdminOrderResponse mapToAdminOrderResponse(
      Order order, List<OrderDetail> details) {
    OrderDto.AdminOrderResponse r = new OrderDto.AdminOrderResponse();
    r.setId(order.getId());
    r.setOrderCode(order.getOrderCode());

    // Thông tin khách hàng
    User user = order.getUser();
    r.setUserId(user.getId());
    r.setUsername(user.getUsername());
    r.setCustomerName(user.getFullName());
    r.setCustomerEmail(user.getEmail());
    r.setCustomerPhone(user.getPhone());

    // Giao hàng
    r.setShippingName(order.getShippingName());
    r.setShippingPhone(order.getShippingPhone());
    r.setShippingAddress(order.getShippingAddress());
    r.setShippingProvince(order.getShippingProvince());
    r.setShippingDistrict(order.getShippingDistrict());
    r.setShippingWard(order.getShippingWard());
    r.setShippingFee(order.getShippingFee());
    r.setTrackingCode(order.getTrackingCode());

    // Thanh toán & trạng thái
    r.setPaymentMethod(order.getPaymentMethod().name());
    r.setPaymentStatus(order.getPaymentStatus().name());
    r.setStatus(order.getStatus().name());
    r.setStatusDisplay(getStatusDisplay(order.getStatus()));

    // Tiền
    r.setSubtotal(order.getSubtotal());
    r.setDiscountAmount(order.getDiscountAmount());
    r.setTotalAmount(order.getTotalAmount());

    // Coupon
    if (order.getCoupon() != null) {
      Coupon c = order.getCoupon();
      OrderDto.CouponInfoResponse ci = new OrderDto.CouponInfoResponse();
      ci.setCode(c.getCode());
      ci.setDiscountType(c.getDiscountType().name());
      ci.setDiscountValue(c.getDiscountValue());
      ci.setDiscountAmount(order.getDiscountAmount());
      r.setCouponInfo(ci);
    }

    // Ghi chú
    r.setNote(order.getNote());
    r.setAdminNote(order.getAdminNote());
    r.setCancelReason(order.getCancelReason());

    // Soft Delete
    r.setIsHidden(order.getIsHidden());
    r.setHiddenAt(order.getHiddenAt());
    r.setHiddenReason(order.getHiddenReason());

    // Thời gian
    r.setOrderDate(order.getOrderDate());
    r.setConfirmedAt(order.getConfirmedAt());
    r.setShippedAt(order.getShippedAt());
    r.setDeliveredAt(order.getDeliveredAt());
    r.setCancelledAt(order.getCancelledAt());

    // Items
    List<OrderDto.OrderItemResponse> itemResponses = new ArrayList<>();
    if (details != null) {
      for (OrderDetail d : details) {
        OrderDto.OrderItemResponse ir = mapOrderDetailToItemResponse(d);
        itemResponses.add(ir);
      }
    }
    r.setItems(itemResponses);
    return r;
  }

  // ─── Admin mapper: Order → AdminOrderSummary ──────────────────────────────
  private OrderDto.AdminOrderSummaryResponse mapToAdminOrderSummary(Order order) {
    OrderDto.AdminOrderSummaryResponse s = new OrderDto.AdminOrderSummaryResponse();
    s.setId(order.getId());
    s.setOrderCode(order.getOrderCode());
    s.setStatus(order.getStatus().name());
    s.setStatusDisplay(getStatusDisplay(order.getStatus()));
    s.setPaymentMethod(order.getPaymentMethod().name());
    s.setPaymentStatus(order.getPaymentStatus().name());
    s.setTotalAmount(order.getTotalAmount());
    s.setOrderDate(order.getOrderDate());
    s.setIsHidden(order.getIsHidden());

    // Thông tin khách hàng
    User user = order.getUser();
    s.setUserId(user.getId());
    s.setUsername(user.getUsername());
    s.setCustomerName(user.getFullName());

    List<OrderDetail> details = order.getOrderDetails();
    if (details != null && !details.isEmpty()) {
      s.setTotalItems(details.stream().mapToInt(OrderDetail::getQuantity).sum());
      OrderDetail first = details.get(0);
      s.setFirstItemName(first.getProductName());
      if (first.getVariant() != null) {
        s.setFirstItemImage(getVariantImageUrl(first.getVariant()));
      }
    } else {
      s.setTotalItems(0);
    }
    return s;
  }

  private User findUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
  }

  private String getStatusDisplay(Order.OrderStatus status) {
    return switch (status) {
      case PENDING -> "Chờ xác nhận";
      case CONFIRMED -> "Đã xác nhận";
      case PROCESSING -> "Đang xử lý";
      case SHIPPING -> "Đang giao hàng";
      case DELIVERED -> "Đã giao hàng";
      case COMPLETED -> "Hoàn thành";
      case CANCELLED -> "Đã hủy";
      case REFUNDED -> "Đã hoàn tiền";
    };
  }

  private String getVariantImageUrl(ProductVariant variant) {
    if (variant.getImages() != null && !variant.getImages().isEmpty()) {
      return serverUrl + "/img/" + variant.getImages().get(0).getId();
    }
    if (variant.getProduct() != null
        && variant.getProduct().getImages() != null
        && !variant.getProduct().getImages().isEmpty()) {
      return serverUrl + "/img/" + variant.getProduct().getImages().get(0).getId();
    }
    return null;
  }

  // ─── map Order + details → full response ──────────────────────────────────
  private OrderDto.OrderResponse mapToOrderResponse(Order order, List<OrderDetail> details) {
    OrderDto.OrderResponse r = new OrderDto.OrderResponse();
    r.setId(order.getId());
    r.setOrderCode(order.getOrderCode());
    r.setShippingName(order.getShippingName());
    r.setShippingPhone(order.getShippingPhone());
    r.setShippingAddress(order.getShippingAddress());
    r.setShippingProvince(order.getShippingProvince());
    r.setShippingDistrict(order.getShippingDistrict());
    r.setShippingWard(order.getShippingWard());
    r.setShippingFee(order.getShippingFee());
    r.setPaymentMethod(order.getPaymentMethod().name());
    r.setPaymentStatus(order.getPaymentStatus().name());
    r.setStatus(order.getStatus().name());
    r.setStatusDisplay(getStatusDisplay(order.getStatus()));
    r.setPaymentUrl(order.getPaymentUrl());
    r.setTransactionRef(order.getTransactionRef());
    r.setSubtotal(order.getSubtotal());
    r.setDiscountAmount(order.getDiscountAmount());
    r.setTotalAmount(order.getTotalAmount());
    r.setNote(order.getNote());
    r.setCancelReason(order.getCancelReason());
    r.setOrderDate(order.getOrderDate());
    r.setConfirmedAt(order.getConfirmedAt());
    r.setDeliveredAt(order.getDeliveredAt());
    r.setCancelledAt(order.getCancelledAt());

    // Coupon info
    if (order.getCoupon() != null) {
      Coupon c = order.getCoupon();
      OrderDto.CouponInfoResponse ci = new OrderDto.CouponInfoResponse();
      ci.setCode(c.getCode());
      ci.setDiscountType(c.getDiscountType().name());
      ci.setDiscountValue(c.getDiscountValue());
      ci.setDiscountAmount(order.getDiscountAmount());
      r.setCouponInfo(ci);
    }

    // Items
    List<OrderDto.OrderItemResponse> itemResponses = new ArrayList<>();
    if (details != null) {
      for (OrderDetail d : details) {
        OrderDto.OrderItemResponse ir = mapOrderDetailToItemResponse(d);
        itemResponses.add(ir);
      }
    }
    r.setItems(itemResponses);

    List<OrderStatusHistory> histories =
        orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());
    if (histories.isEmpty()) {
      OrderDto.TimelineItem initial = new OrderDto.TimelineItem();
      initial.setStatus(order.getStatus().name());
      initial.setNote("Đặt hàng");
      initial.setChangedBy("Hệ thống");
      initial.setCreatedAt(order.getOrderDate());
      r.setTimeline(List.of(initial));
    } else {
      r.setTimeline(
          histories.stream()
              .map(
                  h ->
                      new OrderDto.TimelineItem(
                          h.getToStatus(),
                          h.getNote(),
                          h.getChangedBy() != null ? h.getChangedBy().getFullName() : "Hệ thống",
                          h.getCreatedAt()))
              .collect(Collectors.toList()));
    }

    return r;
  }

  private void saveOrderHistory(Order order, Order.OrderStatus status, String note, User user) {
    OrderStatusHistory h = new OrderStatusHistory();
    h.setOrder(order);
    h.setFromStatus(order.getStatus() != null ? order.getStatus().name() : null);
    h.setToStatus(status.name());
    h.setNote(note);
    h.setChangedBy(user);
    orderStatusHistoryRepository.save(h);
  }

  // ─── map Order → summary (danh sách) ─────────────────────────────────────
  private OrderDto.OrderSummaryResponse mapToOrderSummary(Order order) {
    OrderDto.OrderSummaryResponse s = new OrderDto.OrderSummaryResponse();
    s.setId(order.getId());
    s.setOrderCode(order.getOrderCode());
    s.setStatus(order.getStatus().name());
    s.setStatusDisplay(getStatusDisplay(order.getStatus()));
    s.setPaymentMethod(order.getPaymentMethod().name());
    s.setPaymentStatus(order.getPaymentStatus().name());
    s.setTotalAmount(order.getTotalAmount());
    s.setOrderDate(order.getOrderDate());

    List<OrderDetail> details = order.getOrderDetails();
    if (details != null && !details.isEmpty()) {
      s.setTotalItems(details.stream().mapToInt(OrderDetail::getQuantity).sum());
      OrderDetail first = details.get(0);
      s.setFirstItemName(first.getProductName());
      if (first.getVariant() != null) {
        s.setFirstItemImage(getVariantImageUrl(first.getVariant()));
      }
    } else {
      s.setTotalItems(0);
    }
    return s;
  }

  /** Helper chung: map OrderDetail → OrderItemResponse (bao gồm danh sách IMEI đã gán) */
  private OrderDto.OrderItemResponse mapOrderDetailToItemResponse(OrderDetail d) {
    OrderDto.OrderItemResponse ir = new OrderDto.OrderItemResponse();
    ir.setId(d.getId());
    ir.setVariantId(d.getVariant() != null ? d.getVariant().getId() : null);
    ir.setProductName(d.getProductName());
    ir.setVariantName(d.getVariantName());
    ir.setSkuCode(d.getSkuCode());
    ir.setQuantity(d.getQuantity());
    ir.setUnitPrice(d.getUnitPrice());
    ir.setTotalPrice(d.getTotalPrice());
    if (d.getVariant() != null) {
      ir.setImageUrl(getVariantImageUrl(d.getVariant()));
    }

    // Gắn danh sách IMEI/Serial đã được assign cho dòng này
    List<OrderItem> serialItems = orderItemRepository.findByOrderDetailId(d.getId());
    if (serialItems != null && !serialItems.isEmpty()) {
      List<String> imeis =
          serialItems.stream()
              .map(
                  oi -> {
                    ProductItem pi = oi.getProductItem();
                    // Ưu tiên hiển thị IMEI, nếu không có thì hiển thị Serial
                    if (pi.getImei() != null && !pi.getImei().isBlank()) {
                      return pi.getImei();
                    }
                    return pi.getSerialNumber();
                  })
              .collect(Collectors.toList());
      ir.setAssignedImeis(imeis);
    }

    return ir;
  }
}
