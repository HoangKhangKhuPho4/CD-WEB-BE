package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.OrderDto;
import com.cdweb.be.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders")
@PreAuthorize("isAuthenticated()")
// RBAC: Class-level chỉ yêu cầu đăng nhập; từng action có @PreAuthorize riêng để kiểm tra RightCode
public class OrderController {

  @Autowired private OrderService orderService;

  // ─── POST /api/orders — Đặt hàng (checkout từ giỏ) ───────────────────────
  @PostMapping
  @PreAuthorize("hasAnyAuthority('ORDER_CREATE', 'CHECKOUT_PAYMENT')")
  public ResponseEntity<ApiResponse<OrderDto.OrderResponse>> checkout(
      @Valid @RequestBody OrderDto.CheckoutRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    OrderDto.OrderResponse order = orderService.checkout(username, request);
    return ResponseEntity.ok(ApiResponse.success("Đặt hàng thành công", order));
  }

  // ─── GET /api/orders — Danh sách đơn hàng của tôi ────────────────────────
  @GetMapping
  @PreAuthorize("hasAuthority('USER_ORDER_HISTORY')")
  public ResponseEntity<ApiResponse<Page<OrderDto.OrderSummaryResponse>>> getMyOrders(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestParam(value = "status", required = false) String status) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    Page<OrderDto.OrderSummaryResponse> orders =
        orderService.getMyOrders(username, status, pageable);
    return ResponseEntity.ok(ApiResponse.success("Orders retrieved successfully", orders));
  }

  // ─── GET /api/orders/{orderCode} — Chi tiết đơn hàng ─────────────────────
  @GetMapping("/{orderCode}")
  @PreAuthorize("hasAuthority('USER_ORDER_HISTORY')")
  public ResponseEntity<ApiResponse<OrderDto.OrderResponse>> getOrderDetail(
      @PathVariable("orderCode") String orderCode) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    OrderDto.OrderResponse order = orderService.getOrderByCode(username, orderCode);
    return ResponseEntity.ok(ApiResponse.success("Order detail retrieved successfully", order));
  }

  // ─── PUT /api/orders/{id}/cancel — Hủy đơn hàng ──────────────────────────
  @PutMapping("/{id}/cancel")
  @PreAuthorize("hasAnyAuthority('ORDER_CANCEL', 'CHECKOUT_PAYMENT')")
  public ResponseEntity<ApiResponse<OrderDto.OrderResponse>> cancelOrder(
      @PathVariable("id") Integer id,
      @RequestBody(required = false) OrderDto.CancelRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    OrderDto.OrderResponse order = orderService.cancelOrder(username, id, request);
    return ResponseEntity.ok(ApiResponse.success("Đơn hàng đã được hủy thành công", order));
  }

  // ─── GET /api/orders/preview-coupon?code=XXX — Xem trước giảm giá ────────
  @GetMapping("/preview-coupon")
  @PreAuthorize("hasAnyAuthority('ORDER_CREATE', 'CHECKOUT_PAYMENT')")
  public ResponseEntity<ApiResponse<OrderDto.ApplyCouponResponse>> previewCoupon(
      @RequestParam("code") String code) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    OrderDto.ApplyCouponResponse result = orderService.previewCoupon(username, code);
    return ResponseEntity.ok(ApiResponse.success("Coupon applied preview", result));
  }
}
