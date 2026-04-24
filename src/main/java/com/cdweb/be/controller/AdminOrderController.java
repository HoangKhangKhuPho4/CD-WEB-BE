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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/orders")
@PreAuthorize("hasAuthority('ORDER_VIEW_ALL')")
public class AdminOrderController {

  @Autowired private OrderService orderService;

  // ─── GET /api/admin/orders — Danh sách tất cả đơn hàng (phân trang, lọc) ─
  @GetMapping
  public ResponseEntity<ApiResponse<Page<OrderDto.AdminOrderSummaryResponse>>> getAllOrders(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "sortBy", defaultValue = "orderDate") String sortBy,
      @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {

    Sort sort =
        sortDir.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<OrderDto.AdminOrderSummaryResponse> orders =
        orderService.adminGetAllOrders(status, keyword, pageable);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", orders));
  }

  // ─── GET /api/admin/orders/stats — Thống kê đơn hàng theo trạng thái ─────
  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<OrderDto.OrderStatsResponse>> getOrderStats() {
    OrderDto.OrderStatsResponse stats = orderService.getOrderStats();
    return ResponseEntity.ok(ApiResponse.success("Thống kê đơn hàng", stats));
  }

  // ─── GET /api/admin/orders/{id} — Chi tiết đơn hàng (admin view) ─────────
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<OrderDto.AdminOrderResponse>> getOrderDetail(
      @PathVariable("id") Integer id) {
    OrderDto.AdminOrderResponse order = orderService.adminGetOrderById(id);
    return ResponseEntity.ok(ApiResponse.success("Chi tiết đơn hàng", order));
  }

  // ─── PUT /api/admin/orders/{id}/status — Cập nhật trạng thái đơn hàng ────
  @PutMapping("/{id}/status")
  @PreAuthorize(
      "hasAnyAuthority('ORDER_CONFIRM', 'ORDER_ASSIGN_SHIPPING', 'ORDER_TRACKING_UPDATE')")
  public ResponseEntity<ApiResponse<OrderDto.AdminOrderResponse>> updateOrderStatus(
      @PathVariable("id") Integer id, @Valid @RequestBody OrderDto.UpdateStatusRequest request) {
    OrderDto.AdminOrderResponse order = orderService.adminUpdateOrderStatus(id, request);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", order));
  }

  // ─── PUT /api/admin/orders/{id}/payment-status — Cập nhật thanh toán ──────
  @PutMapping("/{id}/payment-status")
  @PreAuthorize("hasAuthority('ORDER_CONFIRM')")
  public ResponseEntity<ApiResponse<OrderDto.AdminOrderResponse>> updatePaymentStatus(
      @PathVariable("id") Integer id,
      @Valid @RequestBody OrderDto.UpdatePaymentStatusRequest request) {
    OrderDto.AdminOrderResponse order = orderService.adminUpdatePaymentStatus(id, request);
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật trạng thái thanh toán thành công", order));
  }

  // ─── PUT /api/admin/orders/{id}/cancel — Admin hủy đơn hàng ──────────────
  @PutMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('ORDER_CANCEL')")
  public ResponseEntity<ApiResponse<OrderDto.AdminOrderResponse>> cancelOrder(
      @PathVariable("id") Integer id,
      @RequestBody(required = false) OrderDto.CancelRequest request) {
    OrderDto.AdminOrderResponse order = orderService.adminCancelOrder(id, request);
    return ResponseEntity.ok(ApiResponse.success("Đã hủy đơn hàng", order));
  }

  // ─── PATCH /api/admin/orders/{id}/visibility — Ẩn / Hiện đơn hàng ────────
  @PatchMapping("/{id}/visibility")
  @PreAuthorize("hasAuthority('ORDER_CANCEL')")
  public ResponseEntity<ApiResponse<OrderDto.AdminOrderResponse>> toggleVisibility(
      @PathVariable("id") Integer id, @RequestBody OrderDto.UpdateVisibilityRequest request) {
    OrderDto.AdminOrderResponse order = orderService.adminToggleOrderVisibility(id, request);
    String msg =
        Boolean.TRUE.equals(request.getHidden()) ? "Đã ẩn đơn hàng" : "Đã hiện lại đơn hàng";
    return ResponseEntity.ok(ApiResponse.success(msg, order));
  }

  // ─── GET /api/admin/orders/hidden — Danh sách đơn hàng đang bị ẩn ────────
  @GetMapping("/hidden")
  public ResponseEntity<ApiResponse<Page<OrderDto.AdminOrderSummaryResponse>>> getHiddenOrders(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
    Page<OrderDto.AdminOrderSummaryResponse> orders = orderService.adminGetHiddenOrders(pageable);
    return ResponseEntity.ok(ApiResponse.success("Danh sách đơn hàng đang bị ẩn", orders));
  }

  // ─── POST /api/admin/orders/{id}/assign-imei — Gán IMEI/Serial cho đơn ───
  @PostMapping("/{id}/assign-imei")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ORDER_CONFIRM', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<OrderDto.AdminOrderResponse>> assignImei(
      @PathVariable("id") Integer id, @Valid @RequestBody OrderDto.AssignImeiRequest request) {
    OrderDto.AdminOrderResponse order = orderService.assignImeiToOrder(id, request);
    return ResponseEntity.ok(
        ApiResponse.success("Đã gán IMEI/Serial cho đơn hàng thành công", order));
  }
}
