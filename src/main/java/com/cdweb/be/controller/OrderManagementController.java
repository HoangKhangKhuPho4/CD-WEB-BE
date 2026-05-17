package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.OrderManagementDto;
import com.cdweb.be.service.OrderManagementService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@Tag(name = "Quản lý Đơn hàng", description = "API dành cho Admin và Customer quản lý đơn hàng")
public class OrderManagementController {

    @Autowired private OrderManagementService orderManagementService;

    // ═══════════════════════════════════════
    //  ADMIN ENDPOINTS
    // ═══════════════════════════════════════

    @GetMapping("/api/admin/orders")
    @PreAuthorize("hasAnyAuthority('ORDER_MANAGE', 'ADMIN')")
    @Operation(summary = "[Admin] Danh sách đơn hàng", description = "Tìm kiếm, lọc và phân trang đơn hàng")
    public ResponseEntity<ApiResponse<Page<OrderManagementDto.OrderSummaryResponse>>> adminGetOrders(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "15") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {

        Sort sort = sortDir.equalsIgnoreCase("asc") ? Sort.by(sortBy).ascending() : Sort.by(sortBy).descending();
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<OrderManagementDto.OrderSummaryResponse> result = orderManagementService.adminGetOrders(keyword, status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đơn hàng thành công", result));
    }

    @GetMapping("/api/admin/orders/{id}")
    @PreAuthorize("hasAnyAuthority('ORDER_MANAGE', 'ADMIN')")
    @Operation(summary = "[Admin] Chi tiết đơn hàng (có timeline)")
    public ResponseEntity<ApiResponse<OrderManagementDto.OrderDetailResponse>> adminGetOrderDetail(
            @PathVariable Integer id) {
        OrderManagementDto.OrderDetailResponse result = orderManagementService.adminGetOrderDetail(id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công", result));
    }

    @PatchMapping("/api/admin/orders/{id}/status")
    @PreAuthorize("hasAnyAuthority('ORDER_MANAGE', 'ADMIN')")
    @Operation(summary = "[Admin] Cập nhật trạng thái đơn")
    public ResponseEntity<ApiResponse<OrderManagementDto.OrderDetailResponse>> updateStatus(
            @PathVariable Integer id,
            @RequestBody OrderManagementDto.UpdateStatusRequest request,
            Principal principal) {
        OrderManagementDto.OrderDetailResponse result = orderManagementService.updateStatus(id, request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", result));
    }

    @PatchMapping("/api/admin/orders/bulk-status")
    @PreAuthorize("hasAnyAuthority('ORDER_MANAGE', 'ADMIN')")
    @Operation(summary = "[Admin] Cập nhật trạng thái hàng loạt")
    public ResponseEntity<ApiResponse<OrderManagementDto.BulkUpdateResult>> bulkUpdateStatus(
            @RequestBody OrderManagementDto.BulkUpdateStatusRequest request,
            Principal principal) {
        OrderManagementDto.BulkUpdateResult result = orderManagementService.bulkUpdateStatus(request, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Cập nhật hàng loạt hoàn tất", result));
    }

    // ═══════════════════════════════════════
    //  CUSTOMER ENDPOINTS
    // ═══════════════════════════════════════

    @GetMapping("/api/user/orders")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[Customer] Lịch sử đơn hàng", description = "Xem danh sách đơn, lọc theo trạng thái")
    public ResponseEntity<ApiResponse<Page<OrderManagementDto.OrderSummaryResponse>>> customerGetOrders(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Principal principal) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<OrderManagementDto.OrderSummaryResponse> result = orderManagementService.customerGetOrders(principal.getName(), status, pageable);
        return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử đơn hàng thành công", result));
    }

    @GetMapping("/api/user/orders/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[Customer] Chi tiết đơn hàng")
    public ResponseEntity<ApiResponse<OrderManagementDto.OrderDetailResponse>> customerGetOrderDetail(
            @PathVariable Integer id, Principal principal) {
        OrderManagementDto.OrderDetailResponse result = orderManagementService.customerGetOrderDetail(principal.getName(), id);
        return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết đơn hàng thành công", result));
    }

    @PatchMapping("/api/user/orders/{id}/cancel")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "[Customer] Hủy đơn hàng")
    public ResponseEntity<ApiResponse<OrderManagementDto.OrderDetailResponse>> cancelOrder(
            @PathVariable Integer id,
            @RequestParam(required = false, defaultValue = "Khách hàng yêu cầu hủy") String reason,
            Principal principal) {
        OrderManagementDto.OrderDetailResponse result = orderManagementService.cancelOrder(principal.getName(), id, reason);
        return ResponseEntity.ok(ApiResponse.success("Hủy đơn hàng thành công", result));
    }
}