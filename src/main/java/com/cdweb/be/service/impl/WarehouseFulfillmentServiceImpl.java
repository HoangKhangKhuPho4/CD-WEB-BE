package com.cdweb.be.service.impl;

import com.cdweb.be.dto.OrderDto;
import com.cdweb.be.dto.OrderManagementDto;
import com.cdweb.be.dto.WarehouseFulfillmentDto;
import com.cdweb.be.entity.Order;
import com.cdweb.be.entity.OrderDetail;
import com.cdweb.be.entity.OrderItem;
import com.cdweb.be.entity.OrderStatusHistory;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.OrderDetailRepository;
import com.cdweb.be.repository.OrderItemRepository;
import com.cdweb.be.repository.OrderRepository;
import com.cdweb.be.repository.OrderStatusHistoryRepository;
import com.cdweb.be.repository.ProductItemRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.service.GhnService;
import com.cdweb.be.service.OrderManagementService;
import com.cdweb.be.service.OrderService;
import com.cdweb.be.service.WarehouseFulfillmentService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WarehouseFulfillmentServiceImpl implements WarehouseFulfillmentService {

  private static final Set<Order.OrderStatus> DEFAULT_QUEUE_STATUSES =
      EnumSet.of(
          Order.OrderStatus.CONFIRMED,
          Order.OrderStatus.PROCESSING,
          Order.OrderStatus.SHIPPING,
          Order.OrderStatus.DELIVERED);

  private final OrderRepository orderRepository;
  private final OrderDetailRepository orderDetailRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderStatusHistoryRepository historyRepository;
  private final ProductItemRepository productItemRepository;
  private final UserRepository userRepository;
  private final OrderService orderService;
  private final OrderManagementService orderManagementService;
  private final GhnService ghnService;

  @Override
  @Transactional(readOnly = true)
  public Page<WarehouseFulfillmentDto.QueueItem> fulfillmentQueue(
      String keyword, String statusFilter, Pageable pageable) {
    Set<Order.OrderStatus> statuses = resolveQueueStatuses(statusFilter);
    String kw = keyword != null && !keyword.isBlank() ? keyword.trim() : null;
    return orderRepository
        .findFulfillmentQueue(statuses, kw, pageable)
        .map(this::toQueueItem);
  }

  @Override
  @Transactional(readOnly = true)
  public WarehouseFulfillmentDto.FulfillmentDetail getFulfillmentDetail(Integer orderId) {
    Order order = findActiveOrder(orderId);
    assertFulfillmentVisible(order);
    User current = getCurrentUserOrNull();
    return buildFulfillmentDetail(order, current);
  }

  @Override
  @Transactional
  public WarehouseFulfillmentDto.FulfillmentDetail startPicking(
      Integer orderId, String username) {
    Order order = findActiveOrder(orderId);
    User picker = loadUser(username);
    assertCanPick(order, picker);

    if (order.getStatus() == Order.OrderStatus.CONFIRMED) {
      Order.OrderStatus fromStatus = order.getStatus();
      order.setPickedByUser(picker);
      order.setPickedAt(LocalDateTime.now());
      order.setStatus(Order.OrderStatus.PROCESSING);
      saveHistory(order, fromStatus, Order.OrderStatus.PROCESSING, "Bắt đầu gom hàng", picker);
      orderRepository.save(order);
    } else if (order.getStatus() == Order.OrderStatus.PROCESSING) {
      if (order.getPickedByUser() == null) {
        order.setPickedByUser(picker);
        order.setPickedAt(LocalDateTime.now());
        orderRepository.save(order);
      }
    }

    return buildFulfillmentDetail(order, picker);
  }

  @Override
  @Transactional(readOnly = true)
  public WarehouseFulfillmentDto.FifoSerialsResponse getFifoSerials(Integer variantId, int limit) {
    int take = Math.max(1, Math.min(limit, 20));
    List<ProductItem> items =
        productItemRepository.findByVariantIdAndStatusOrderByCreatedAtAsc(
            variantId, ProductItem.ProductItemStatus.AVAILABLE, PageRequest.of(0, take));
    return WarehouseFulfillmentDto.FifoSerialsResponse.builder()
        .variantId(variantId)
        .suggestedSerials(items.stream().map(this::toFifoHint).toList())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public WarehouseFulfillmentDto.ValidateScanResponse validateScan(
      Integer orderId, WarehouseFulfillmentDto.ValidateScanRequest request) {
    Order order = findActiveOrder(orderId);
    assertScanAllowed(order, getCurrentUserOrNull());
    OrderDetail line = loadOrderLine(orderId, request.getOrderDetailId());
    ProductItem scanned = resolveProductItem(request.getScannedCode());
    boolean overrideFifo = Boolean.TRUE.equals(request.getOverrideFifo());

    WarehouseFulfillmentDto.FifoSerialHint expected = nextFifoHint(line);
    String scannedLabel = displayCode(scanned);

    if (!scanned.getVariant().getId().equals(line.getVariant().getId())) {
      return invalid(
          "Serial không thuộc đúng biến thể đặt hàng",
          expected != null ? displayCodeFromHint(expected) : null,
          scannedLabel);
    }
    if (scanned.getStatus() != ProductItem.ProductItemStatus.AVAILABLE) {
      return invalid(
          "Serial không khả dụng (trạng thái: " + scanned.getStatus() + ")",
          expected != null ? displayCodeFromHint(expected) : null,
          scannedLabel);
    }
    if (!overrideFifo && expected != null && !Objects.equals(expected.getProductItemId(), scanned.getId())) {
      return invalid(
          "Không đúng serial FIFO. Cần lấy: "
              + displayCodeFromHint(expected)
              + (expected.getLocation() != null ? " tại " + expected.getLocation() : ""),
          displayCodeFromHint(expected),
          scannedLabel);
    }

    return WarehouseFulfillmentDto.ValidateScanResponse.builder()
        .valid(true)
        .message("Serial hợp lệ — sẵn sàng gán vào đơn")
        .expectedSerial(expected != null ? displayCodeFromHint(expected) : scannedLabel)
        .scannedSerial(scannedLabel)
        .matchedItem(toFifoHint(scanned))
        .build();
  }

  @Override
  @Transactional
  public WarehouseFulfillmentDto.PickingProgress assignSerial(
      Integer orderId, WarehouseFulfillmentDto.AssignSerialRequest request, String username) {
    WarehouseFulfillmentDto.ValidateScanResponse check =
        validateScan(
            orderId,
            new WarehouseFulfillmentDto.ValidateScanRequest(
                request.getOrderDetailId(),
                request.getScannedCode(),
                request.getOverrideFifo(),
                request.getOverrideReason()));

    if (!check.isValid()) {
      throw new BadRequestException(check.getMessage());
    }

    Order order = findActiveOrder(orderId);
    assertScanAllowed(order, loadUser(username));
    ProductItem scanned = resolveProductItem(request.getScannedCode());
    String code = primaryCode(scanned);

    OrderDto.AssignImeiRequest assignReq = new OrderDto.AssignImeiRequest();
    assignReq.setOrderDetailId(request.getOrderDetailId());
    assignReq.setImeis(List.of(code));
    orderService.assignImeiToOrder(orderId, assignReq);

    if (Boolean.TRUE.equals(request.getOverrideFifo()) && request.getOverrideReason() != null) {
      saveHistory(
          order,
          order.getStatus(),
          "Override FIFO: " + request.getOverrideReason().trim(),
          loadUser(username));
    }

    return buildProgress(orderId);
  }

  @Override
  @Transactional(readOnly = true)
  public WarehouseFulfillmentDto.PickingProgress getPickingProgress(Integer orderId) {
    findActiveOrder(orderId);
    return buildProgress(orderId);
  }

  @Override
  @Transactional
  public WarehouseFulfillmentDto.DispatchResponse dispatch(Integer orderId, String username) {
    Order order = findActiveOrder(orderId);
    User user = loadUser(username);
    assertScanAllowed(order, user);
    assertReadyForDispatch(orderId);

    if (order.getStatus() != Order.OrderStatus.PROCESSING) {
      throw new BadRequestException(
          "Chỉ bàn giao được khi đơn đang PROCESSING. Trạng thái hiện tại: " + order.getStatus());
    }

    OrderManagementDto.UpdateStatusRequest req = new OrderManagementDto.UpdateStatusRequest();
    req.setStatus(Order.OrderStatus.SHIPPING.name());
    req.setNote("Xác nhận xuất kho & bàn giao vận chuyển");
    OrderManagementDto.OrderDetailResponse updated =
        orderManagementService.updateStatus(orderId, req, username);

    // Gọi GHN trực tiếp (không qua OrderService @Transactional) — tránh rollback-only
    // khi nuốt exception từ method transactional lồng nhau.
    String printUrl = resolvePrintUrl(updated.getGhnOrderCode());

    return WarehouseFulfillmentDto.DispatchResponse.builder()
        .orderId(orderId)
        .orderCode(updated.getOrderCode())
        .status(updated.getStatus())
        .trackingCode(updated.getTrackingCode())
        .ghnOrderCode(updated.getGhnOrderCode())
        .printUrl(printUrl)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public void assertReadyForDispatch(Integer orderId) {
    WarehouseFulfillmentDto.PickingProgress progress = buildProgress(orderId);
    if (!progress.isComplete() && progress.getTotalRequired() > 0) {
      throw new BadRequestException(
          "Chưa quét đủ serial: "
              + progress.getTotalAssigned()
              + "/"
              + progress.getTotalRequired()
              + " — không thể bàn giao");
    }
  }

  // ─── helpers ─────────────────────────────────────────────────────────────

  private Set<Order.OrderStatus> resolveQueueStatuses(String statusFilter) {
    if (statusFilter == null || statusFilter.isBlank()) {
      return DEFAULT_QUEUE_STATUSES;
    }
    try {
      return EnumSet.of(Order.OrderStatus.valueOf(statusFilter.trim().toUpperCase(Locale.ROOT)));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Trạng thái lọc không hợp lệ: " + statusFilter);
    }
  }

  private void assertFulfillmentVisible(Order order) {
    if (order.getStatus() == Order.OrderStatus.PENDING) {
      throw new BadRequestException("Đơn chưa được Sales xác nhận — không thuộc hàng đợi kho");
    }
  }

  private void assertCanPick(Order order, User picker) {
    assertFulfillmentVisible(order);
    if (order.getStatus() != Order.OrderStatus.CONFIRMED
        && order.getStatus() != Order.OrderStatus.PROCESSING) {
      throw new BadRequestException(
          "Không thể bắt đầu gom hàng ở trạng thái: " + order.getStatus());
    }
    if (order.getPickedByUser() != null
        && !order.getPickedByUser().getId().equals(picker.getId())
        && order.getStatus() == Order.OrderStatus.PROCESSING) {
      throw new BadRequestException(
          "Đơn đang được soạn bởi "
              + order.getPickedByUser().getFullName()
              + " — vui lòng chọn đơn khác");
    }
  }

  private void assertScanAllowed(Order order, User current) {
    assertFulfillmentVisible(order);
    if (order.getStatus() != Order.OrderStatus.PROCESSING) {
      throw new BadRequestException(
          "Cần bấm 'Bắt đầu gom hàng' trước (đơn phải ở PROCESSING)");
    }
    if (order.getPickedByUser() != null
        && current != null
        && !order.getPickedByUser().getId().equals(current.getId())) {
      throw new BadRequestException(
          "Đơn đang được soạn bởi " + order.getPickedByUser().getFullName());
    }
  }

  private WarehouseFulfillmentDto.FulfillmentDetail buildFulfillmentDetail(
      Order order, User current) {
    WarehouseFulfillmentDto.PickingProgress progress = buildProgress(order.getId());
    boolean canStart =
        order.getStatus() == Order.OrderStatus.CONFIRMED
            || (order.getStatus() == Order.OrderStatus.PROCESSING
                && order.getPickedByUser() == null);
    boolean canScan =
        order.getStatus() == Order.OrderStatus.PROCESSING
            && (order.getPickedByUser() == null
                || current == null
                || order.getPickedByUser().getId().equals(current.getId()));
    boolean canDispatch = canScan && progress.isComplete();

    List<OrderStatusHistory> histories =
        historyRepository.findByOrderIdOrderByCreatedAtAsc(order.getId());

    return WarehouseFulfillmentDto.FulfillmentDetail.builder()
        .id(order.getId())
        .orderCode(order.getOrderCode())
        .status(order.getStatus().name())
        .customerName(order.getShippingName())
        .customerPhone(order.getShippingPhone())
        .shippingAddress(order.getShippingAddress())
        .total(order.getTotalAmount())
        .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "")
        .trackingCode(order.getTrackingCode())
        .ghnOrderCode(order.getGhnOrderCode())
        .orderDate(order.getOrderDate())
        .pickedByUserId(order.getPickedByUser() != null ? order.getPickedByUser().getId() : null)
        .pickedByName(
            order.getPickedByUser() != null ? order.getPickedByUser().getFullName() : null)
        .pickedAt(order.getPickedAt())
        .canStartPicking(canStart)
        .canScan(canScan)
        .canDispatch(canDispatch)
        .progress(progress)
        .timeline(
            histories.stream()
                .map(
                    h ->
                        new OrderManagementDto.TimelineItem(
                            h.getToStatus(),
                            h.getNote(),
                            h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System",
                            h.getCreatedAt()))
                .toList())
        .build();
  }

  private WarehouseFulfillmentDto.QueueItem toQueueItem(Order order) {
    WarehouseFulfillmentDto.PickingProgress progress = buildProgress(order.getId());
    return WarehouseFulfillmentDto.QueueItem.builder()
        .id(order.getId())
        .orderCode(order.getOrderCode())
        .status(order.getStatus().name())
        .customerName(order.getShippingName())
        .customerPhone(order.getShippingPhone())
        .total(order.getTotalAmount())
        .paymentMethod(order.getPaymentMethod() != null ? order.getPaymentMethod().name() : "")
        .orderDate(order.getOrderDate())
        .totalSerialRequired(progress.getTotalRequired())
        .totalSerialAssigned(progress.getTotalAssigned())
        .pickingComplete(progress.isComplete())
        .pickedByUserId(order.getPickedByUser() != null ? order.getPickedByUser().getId() : null)
        .pickedByName(
            order.getPickedByUser() != null ? order.getPickedByUser().getFullName() : null)
        .pickedAt(order.getPickedAt())
        .canStartPicking(
            order.getStatus() == Order.OrderStatus.CONFIRMED
                || (order.getStatus() == Order.OrderStatus.PROCESSING
                    && order.getPickedByUser() == null))
        .build();
  }

  private WarehouseFulfillmentDto.PickingProgress buildProgress(Integer orderId) {
    List<OrderDetail> lines = orderDetailRepository.findByOrderId(orderId);
    List<WarehouseFulfillmentDto.PickingLine> pickingLines = new ArrayList<>();
    int totalRequired = 0;
    int totalAssigned = 0;

    for (OrderDetail line : lines) {
      if (!isSerialTracked(line.getVariant().getId())) {
        continue;
      }
      int qty = line.getQuantity() != null ? line.getQuantity() : 0;
      List<OrderItem> assigned = orderItemRepository.findByOrderDetailId(line.getId());
      List<String> serials =
          assigned.stream()
              .map(OrderItem::getProductItem)
              .filter(Objects::nonNull)
              .map(this::primaryCode)
              .filter(s -> s != null && !s.isBlank())
              .toList();

      totalRequired += qty;
      totalAssigned += serials.size();

      pickingLines.add(
          WarehouseFulfillmentDto.PickingLine.builder()
              .orderDetailId(line.getId())
              .productName(line.getProductName())
              .variantName(line.getVariantName())
              .skuCode(line.getSkuCode())
              .quantity(qty)
              .assignedCount(serials.size())
              .assignedSerials(serials)
              .nextFifoHint(serials.size() < qty ? nextFifoHint(line) : null)
              .build());
    }

    return WarehouseFulfillmentDto.PickingProgress.builder()
        .totalRequired(totalRequired)
        .totalAssigned(totalAssigned)
        .complete(totalRequired == 0 || totalAssigned >= totalRequired)
        .lines(pickingLines)
        .build();
  }

  private boolean isSerialTracked(Integer variantId) {
    return productItemRepository.countByVariantId(variantId) > 0;
  }

  private WarehouseFulfillmentDto.FifoSerialHint nextFifoHint(OrderDetail line) {
    List<ProductItem> fifo =
        productItemRepository.findByVariantIdAndStatusOrderByCreatedAtAsc(
            line.getVariant().getId(),
            ProductItem.ProductItemStatus.AVAILABLE,
            PageRequest.of(0, 1));
    return fifo.isEmpty() ? null : toFifoHint(fifo.get(0));
  }

  private WarehouseFulfillmentDto.FifoSerialHint toFifoHint(ProductItem pi) {
    return WarehouseFulfillmentDto.FifoSerialHint.builder()
        .productItemId(pi.getId())
        .serialNumber(pi.getSerialNumber())
        .imei(pi.getImei())
        .location(pi.getLocation())
        .batchNumber(pi.getBatchNumber())
        .stockInDate(pi.getCreatedAt())
        .build();
  }

  private ProductItem resolveProductItem(String code) {
    if (code == null || code.isBlank()) {
      throw new BadRequestException("Mã quét trống");
    }
    return productItemRepository
        .findByImeiOrSerialNumber(code.trim(), code.trim())
        .orElseThrow(
            () -> new ResourceNotFoundException("ProductItem", "IMEI/Serial", code.trim()));
  }

  private OrderDetail loadOrderLine(Integer orderId, Integer orderDetailId) {
    OrderDetail line =
        orderDetailRepository
            .findById(orderDetailId)
            .orElseThrow(
                () -> new ResourceNotFoundException("OrderDetail", "id", orderDetailId));
    if (!line.getOrder().getId().equals(orderId)) {
      throw new BadRequestException("Dòng đơn không thuộc đơn hàng này");
    }
    return line;
  }

  private Order findActiveOrder(Integer orderId) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
    if (Boolean.TRUE.equals(order.getIsHidden())) {
      throw new BadRequestException("Đơn hàng đã bị ẩn");
    }
    return order;
  }

  private User loadUser(String username) {
    return userRepository
        .findByUsernameOrEmail(username, username)
        .orElseThrow(() -> new BadRequestException("Không tìm thấy user: " + username));
  }

  private User getCurrentUserOrNull() {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth == null || auth.getName() == null || "anonymousUser".equals(auth.getName())) {
      return null;
    }
    return userRepository.findByUsernameOrEmail(auth.getName(), auth.getName()).orElse(null);
  }

  private void saveHistory(
      Order order,
      Order.OrderStatus fromStatus,
      Order.OrderStatus toStatus,
      String note,
      User changedBy) {
    OrderStatusHistory h = new OrderStatusHistory();
    h.setOrder(order);
    h.setFromStatus(fromStatus.name());
    h.setToStatus(toStatus.name());
    h.setNote(note);
    h.setChangedBy(changedBy);
    historyRepository.save(h);
  }

  private void saveHistory(
      Order order, Order.OrderStatus toStatus, String note, User changedBy) {
    saveHistory(order, order.getStatus(), toStatus, note, changedBy);
  }

  private String primaryCode(ProductItem pi) {
    if (pi.getImei() != null && !pi.getImei().isBlank()) {
      return pi.getImei();
    }
    return pi.getSerialNumber();
  }

  private String displayCode(ProductItem pi) {
    String imei = pi.getImei();
    String serial = pi.getSerialNumber();
    if (imei != null && !imei.isBlank() && serial != null && !serial.isBlank()) {
      return imei + " / " + serial;
    }
    return primaryCode(pi);
  }

  private String displayCodeFromHint(WarehouseFulfillmentDto.FifoSerialHint hint) {
    if (hint.getImei() != null && !hint.getImei().isBlank()) {
      return hint.getImei();
    }
    return hint.getSerialNumber();
  }

  private WarehouseFulfillmentDto.ValidateScanResponse invalid(
      String message, String expected, String scanned) {
    return WarehouseFulfillmentDto.ValidateScanResponse.builder()
        .valid(false)
        .message(message)
        .expectedSerial(expected)
        .scannedSerial(scanned)
        .build();
  }

  private String resolvePrintUrl(String ghnOrderCode) {
    if (ghnOrderCode == null || ghnOrderCode.isBlank()) {
      return null;
    }
    try {
      return ghnService.generatePrintLabel(ghnOrderCode).getPrintUrl();
    } catch (Exception ignored) {
      return null;
    }
  }
}
