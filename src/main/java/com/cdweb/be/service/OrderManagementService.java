package com.cdweb.be.service;

import com.cdweb.be.dto.OrderManagementDto;
import com.cdweb.be.entity.Order;
import com.cdweb.be.entity.OrderStatusHistory;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.repository.OrderRepository;
import com.cdweb.be.repository.OrderStatusHistoryRepository;
import com.cdweb.be.repository.UserRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderManagementService {

    // 1. Đổi List<String> thành List<Order.OrderStatus>
    private static final List<Order.OrderStatus> VALID_TRANSITIONS = List.of(
            Order.OrderStatus.PENDING,
            Order.OrderStatus.CONFIRMED,
            Order.OrderStatus.SHIPPING,
            Order.OrderStatus.DELIVERED,
            Order.OrderStatus.CANCELLED
    );

    @Autowired private OrderRepository orderRepository;
    @Autowired private OrderStatusHistoryRepository historyRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private EmailService emailService;

    // ── Admin: danh sách đơn (có filter) ──
    @Transactional(readOnly = true)
    public Page<OrderManagementDto.OrderSummaryResponse> adminGetOrders(
            String keyword, String statusStr, Pageable pageable) {
        // Nếu có truyền status dạng String từ FE, thử convert sang Enum để tìm
        Order.OrderStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try { status = Order.OrderStatus.valueOf(statusStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        Page<Order> orders = (keyword != null && !keyword.isBlank())
                ? orderRepository.searchOrders(keyword, status, pageable)
                : orderRepository.findByStatusFilter(status, pageable);
        return orders.map(this::toSummary);
    }

    // ── Admin: chi tiết đơn ──
    public OrderManagementDto.OrderDetailResponse adminGetOrderDetail(Integer orderId) {
        Order order = findOrder(orderId);
        return toDetail(order);
    }

    // ── Admin: cập nhật trạng thái ──
    @Transactional
    public OrderManagementDto.OrderDetailResponse updateStatus(
            Integer orderId, OrderManagementDto.UpdateStatusRequest req, String adminUsername) {
        Order order = findOrder(orderId);

        // Convert status String từ Request sang Enum
        Order.OrderStatus toStatus;
        try { toStatus = Order.OrderStatus.valueOf(req.getStatus().toUpperCase()); }
        catch (IllegalArgumentException e) { throw new BadRequestException("Trạng thái không hợp lệ"); }

        validateTransition(order.getStatus(), toStatus);
        assertStatusChangeAllowed(toStatus);

        User admin = userRepository.findByUsernameOrEmail(adminUsername, adminUsername).orElse(null);

        if (req.getTrackingCode() != null && !req.getTrackingCode().isBlank()) {
            order.setTrackingCode(req.getTrackingCode().trim());
        }
        if (req.getGhnOrderCode() != null && !req.getGhnOrderCode().isBlank()) {
            order.setGhnOrderCode(req.getGhnOrderCode().trim());
        }

        // Lưu lịch sử
        saveHistory(order, toStatus, req.getNote(), admin);

        order.setStatus(toStatus);
        orderRepository.save(order);

        // Gửi email thông báo
        sendStatusEmail(order);

        return toDetail(order);
    }

    // ── Admin: bulk update ──
    @Transactional
    public OrderManagementDto.BulkUpdateResult bulkUpdateStatus(
            OrderManagementDto.BulkUpdateStatusRequest req, String adminUsername) {
        int success = 0, fail = 0;
        List<String> errors = new ArrayList<>();
        User admin = userRepository.findByUsernameOrEmail(adminUsername, adminUsername).orElse(null);

        Order.OrderStatus toStatus;
        try { toStatus = Order.OrderStatus.valueOf(req.getStatus().toUpperCase()); }
        catch (IllegalArgumentException e) {
            return new OrderManagementDto.BulkUpdateResult(0, req.getOrderIds().size(), List.of("Trạng thái chuyển đổi không hợp lệ"));
        }

        for (Integer orderId : req.getOrderIds()) {
            try {
                Order order = findOrder(orderId);
                validateTransition(order.getStatus(), toStatus);
                saveHistory(order, toStatus, req.getNote(), admin);
                order.setStatus(toStatus);
                orderRepository.save(order);
                sendStatusEmail(order);
                success++;
            } catch (Exception e) {
                fail++;
                errors.add("Đơn #" + orderId + ": " + e.getMessage());
            }
        }
        return new OrderManagementDto.BulkUpdateResult(success, fail, errors);
    }

    // ── Customer: lịch sử đơn hàng ──
    public Page<OrderManagementDto.OrderSummaryResponse> customerGetOrders(
            String username, String statusStr, Pageable pageable) {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new BadRequestException("User not found"));

        Order.OrderStatus status = null;
        if (statusStr != null && !statusStr.isBlank()) {
            try { status = Order.OrderStatus.valueOf(statusStr.toUpperCase()); }
            catch (IllegalArgumentException ignored) {}
        }

        Page<Order> orders = orderRepository.findByUserIdAndStatusFilter(user.getId(), status, pageable);
        return orders.map(this::toSummary);
    }

    // ── Customer: chi tiết đơn ──
    public OrderManagementDto.OrderDetailResponse customerGetOrderDetail(
            String username, Integer orderId) {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new BadRequestException("User not found"));
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BadRequestException("Đơn hàng không tồn tại"));
        return toDetail(order);
    }

    // ── Customer: hủy đơn ──
    @Transactional
    public OrderManagementDto.OrderDetailResponse cancelOrder(String username, Integer orderId, String reason) {
        User user = userRepository.findByUsernameOrEmail(username, username)
                .orElseThrow(() -> new BadRequestException("User not found"));
        Order order = orderRepository.findByIdAndUserId(orderId, user.getId())
                .orElseThrow(() -> new BadRequestException("Đơn hàng không tồn tại"));

        if (!List.of(Order.OrderStatus.PENDING, Order.OrderStatus.CONFIRMED).contains(order.getStatus())) {
            throw new BadRequestException("Chỉ có thể hủy đơn ở trạng thái chờ xác nhận hoặc đã xác nhận");
        }

        saveHistory(order, Order.OrderStatus.CANCELLED, "Khách hủy: " + reason, user);
        order.setStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);
        sendStatusEmail(order);

        return toDetail(order);
    }

    // ─── helpers ───
    private Order findOrder(Integer id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("Đơn hàng không tồn tại: #" + id));
    }

    private void assertStatusChangeAllowed(Order.OrderStatus toStatus) {
        if (hasAnyAuthority("ROLE_ADMIN", "ORDER_MANAGE")) {
            return;
        }
        switch (toStatus) {
            case CONFIRMED -> {
                if (!hasAnyAuthority("ORDER_CONFIRM")) {
                    throw new BadRequestException("Bạn không có quyền xác nhận đơn hàng (ORDER_CONFIRM)");
                }
            }
            case CANCELLED -> {
                if (!hasAnyAuthority("ORDER_CANCEL")) {
                    throw new BadRequestException("Bạn không có quyền hủy đơn hàng (ORDER_CANCEL)");
                }
            }
            case SHIPPING -> {
                if (!hasAnyAuthority("ORDER_ASSIGN_SHIPPING")) {
                    throw new BadRequestException(
                            "Bạn không có quyền chuyển sang giao hàng (ORDER_ASSIGN_SHIPPING)");
                }
            }
            case DELIVERED -> {
                if (!hasAnyAuthority("ORDER_TRACKING_UPDATE")) {
                    throw new BadRequestException(
                            "Bạn không có quyền cập nhật giao hàng (ORDER_TRACKING_UPDATE)");
                }
            }
            default ->
                    throw new BadRequestException(
                            "Bạn không có quyền chuyển đơn sang trạng thái: " + toStatus);
        }
    }

    private boolean hasAnyAuthority(String... codes) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null) {
            return false;
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        Set<String> required = Set.of(codes);
        return authorities.stream().map(GrantedAuthority::getAuthority).anyMatch(required::contains);
    }

    private void validateTransition(Order.OrderStatus from, Order.OrderStatus to) {
        int fromIdx = VALID_TRANSITIONS.indexOf(from);
        int toIdx = VALID_TRANSITIONS.indexOf(to);
        if (toIdx < 0) throw new BadRequestException("Trạng thái không hợp lệ: " + to);
        if (to == Order.OrderStatus.CANCELLED) return; // cho phép hủy từ bất kỳ trạng thái nào
        if (toIdx <= fromIdx) throw new BadRequestException(
                "Không thể chuyển từ '" + from + "' sang '" + to + "'");
    }

    private void saveHistory(Order order, Order.OrderStatus toStatus, String note, User changedBy) {
        OrderStatusHistory h = new OrderStatusHistory();
        h.setOrder(order);
        h.setFromStatus(order.getStatus().name()); // Lưu lịch sử dưới dạng String (name của Enum)
        h.setToStatus(toStatus.name());
        h.setNote(note);
        h.setChangedBy(changedBy);
        historyRepository.save(h);
    }

    private void sendStatusEmail(Order order) {
        try {
            if (order.getUser() != null && order.getUser().getEmail() != null) {
                // Email service đang nhận status dạng String nên ta gọi .name().toLowerCase()
                emailService.sendOrderStatusEmail(
                        order.getUser().getEmail(),
                        order.getUser().getFullName(),
                        order.getId().toString(),
                        order.getStatus().name().toLowerCase()
                );
            }
        } catch (Exception ignored) {
            // Không để lỗi email ảnh hưởng đến transaction chính
        }
    }

    private OrderManagementDto.OrderSummaryResponse toSummary(Order o) {
        OrderManagementDto.OrderSummaryResponse r = new OrderManagementDto.OrderSummaryResponse();
        r.setId(o.getId());
        r.setOrderCode(o.getOrderCode());
        r.setStatus(o.getStatus() != null ? o.getStatus().name().toLowerCase() : "unknown"); // Convert Enum to String for DTO
        r.setCustomerName(o.getUser() != null ? o.getUser().getFullName() : "");
        r.setCustomerEmail(o.getUser() != null ? o.getUser().getEmail() : "");
        r.setTotal(o.getTotalAmount());
        r.setPaymentMethod(o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "");
        r.setPaymentStatus(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "");
        r.setCreatedAt(o.getOrderDate());
        return r;
    }

    private OrderManagementDto.OrderDetailResponse toDetail(Order o) {
        OrderManagementDto.OrderDetailResponse r = new OrderManagementDto.OrderDetailResponse();
        r.setId(o.getId());
        r.setOrderCode(o.getOrderCode());
        r.setStatus(o.getStatus() != null ? o.getStatus().name().toLowerCase() : "unknown");
        if (o.getUser() != null) {
            r.setCustomerName(o.getUser().getFullName());
            r.setCustomerEmail(o.getUser().getEmail());
            r.setCustomerPhone(o.getUser().getPhone());
        }
        r.setShippingAddress(o.getShippingAddress());
        r.setSubtotal(o.getSubtotal());
        r.setShippingFee(o.getShippingFee());
        r.setDiscount(o.getDiscountAmount());
        r.setTotal(o.getTotalAmount());
        r.setPaymentMethod(o.getPaymentMethod() != null ? o.getPaymentMethod().name() : "");
        r.setPaymentStatus(o.getPaymentStatus() != null ? o.getPaymentStatus().name() : "");
        r.setGhnOrderCode(o.getGhnOrderCode());
        r.setTrackingCode(o.getTrackingCode());
        r.setCreatedAt(o.getOrderDate());

        // Items
        if (o.getOrderDetails() != null) {
            r.setItems(o.getOrderDetails().stream().map(item -> {
                OrderManagementDto.OrderItemResponse ir = new OrderManagementDto.OrderItemResponse();

                // Lấy Product ID thông qua Variant (dùng try-catch bọc lại cho an toàn tuyệt đối)
                Integer pId = null;
                try {
                    if (item.getVariant() != null && item.getVariant().getProduct() != null) {
                        pId = item.getVariant().getProduct().getId();
                    }
                } catch (Exception ignored) {}
                ir.setOrderDetailId(item.getId());
                ir.setProductId(pId);

                ir.setProductName(item.getProductName());
                ir.setQuantity(item.getQuantity());
                ir.setUnitPrice(item.getUnitPrice());
                ir.setSubtotal(item.getTotalPrice()); // Dùng luôn totalPrice có sẵn trong OrderDetail

                return ir;
            }).collect(Collectors.toList()));
        }

        // Timeline
        List<OrderStatusHistory> histories = historyRepository.findByOrderIdOrderByCreatedAtAsc(o.getId());
        r.setTimeline(histories.stream().map(h -> new OrderManagementDto.TimelineItem(
                h.getToStatus(),
                h.getNote(),
                h.getChangedBy() != null ? h.getChangedBy().getFullName() : "System",
                h.getCreatedAt()
        )).collect(Collectors.toList()));

        return r;
    }
}