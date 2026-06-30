package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.PurchaseOrderDto;
import com.cdweb.be.service.PurchaseOrderService;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/purchase-orders")
@RequiredArgsConstructor
public class AdminPurchaseOrderController {

  private final PurchaseOrderService purchaseOrderService;

  /** Danh sách PO cho nhân viên kho (APPROVED / RECEIVING / COMPLETED). */
  @GetMapping
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'PRODUCT_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<?> list(
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String scope,
      @RequestParam(required = false) Integer page,
      @RequestParam(defaultValue = "15") int size) {
    if (page != null
        && (scope == null || scope.isBlank() || "warehouse".equalsIgnoreCase(scope))) {
      Pageable pageable =
          PageRequest.of(Math.max(0, page), Math.max(1, size), Sort.by("createdAt").descending());
      Page<PurchaseOrderDto.SummaryResponse> data =
          purchaseOrderService.listForWarehousePaged(status, pageable);
      return ResponseEntity.ok(ApiResponse.success("Danh sách đơn mua hàng", data));
    }
    List<PurchaseOrderDto.SummaryResponse> data;
    if ("procurement".equalsIgnoreCase(scope)) {
      data = purchaseOrderService.listForProcurement(status);
    } else if ("approval".equalsIgnoreCase(scope)) {
      data = purchaseOrderService.listForApproval();
    } else {
      data = purchaseOrderService.listForWarehouse(status);
    }
    return ResponseEntity.ok(ApiResponse.success("Danh sách đơn mua hàng", data));
  }

  /** Danh sách PO chờ quét Serial (APPROVED / RECEIVING). */
  @GetMapping("/imei-queue")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<PurchaseOrderDto.SummaryResponse>>> imeiQueue() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Danh sách PO chờ quét Serial", purchaseOrderService.listImeiQueue()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'PRODUCT_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.DetailResponse>> detail(
      @PathVariable Integer id,
      @RequestParam(defaultValue = "false") boolean unrestricted) {
    PurchaseOrderDto.DetailResponse detail =
        unrestricted
            ? purchaseOrderService.getDetailUnrestricted(id)
            : purchaseOrderService.getDetail(id);
    return ResponseEntity.ok(ApiResponse.success("Chi tiết đơn mua hàng", detail));
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.DetailResponse>> create(
      @RequestBody PurchaseOrderDto.CreateRequest request, Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tạo đơn mua hàng thành công",
            purchaseOrderService.create(request, principal.getName())));
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.SummaryResponse>> approve(
      @PathVariable Integer id, Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã duyệt đơn mua hàng",
            purchaseOrderService.approve(id, principal.getName())));
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.SummaryResponse>> reject(
      @PathVariable Integer id,
      @RequestBody PurchaseOrderDto.RejectRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã từ chối đơn mua hàng",
            purchaseOrderService.reject(id, request, principal.getName())));
  }

  /** Bắt đầu kiểm đếm: APPROVED → RECEIVING. */
  @PostMapping("/{id}/start-receiving")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.SummaryResponse>> startReceiving(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã chuyển PO sang trạng thái kiểm đếm",
            purchaseOrderService.startReceiving(id)));
  }

  @GetMapping("/{id}/receive-detail")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.ReceiveDetailResponse>> receiveDetail(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Chi tiết kiểm đếm nhập kho",
            purchaseOrderService.getReceiveDetail(id)));
  }

  @PostMapping("/{id}/validate-scan")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.ValidateScanResponse>> validateScan(
      @PathVariable Integer id, @RequestBody PurchaseOrderDto.ValidateScanRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Kiểm tra mã quét",
            purchaseOrderService.validateReceiveScan(id, request)));
  }

  @PostMapping("/{id}/receive-serial")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.ReceiveDetailResponse>> receiveSerial(
      @PathVariable Integer id,
      @RequestBody PurchaseOrderDto.ReceiveSerialRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã nhập serial vào kho",
            purchaseOrderService.receiveSerial(id, request, principal.getName())));
  }

  @PostMapping("/{id}/receive-serial-bulk")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.BulkReceiveSerialResponse>> receiveSerialBulk(
      @PathVariable Integer id,
      @RequestBody PurchaseOrderDto.BulkReceiveSerialRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã xử lý nhập serial hàng loạt",
            purchaseOrderService.receiveSerialBulk(id, request, principal.getName())));
  }

  @PostMapping("/{id}/receive-quantity")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.ReceiveDetailResponse>> receiveQuantity(
      @PathVariable Integer id,
      @RequestBody PurchaseOrderDto.ReceiveQuantityRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã nhập số lượng vào kho",
            purchaseOrderService.receiveQuantity(id, request, principal.getName())));
  }

  @PostMapping("/{id}/report-damaged")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.ReceiveDetailResponse>> reportDamaged(
      @PathVariable Integer id,
      @RequestBody PurchaseOrderDto.ReportDamagedRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã ghi nhận hàng lỗi",
            purchaseOrderService.reportDamaged(id, request, principal.getName())));
  }

  @PostMapping("/{id}/complete-receiving")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<PurchaseOrderDto.CompleteReceivingResponse>> completeReceiving(
      @PathVariable Integer id,
      @RequestBody(required = false) PurchaseOrderDto.CompleteReceivingRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã hoàn tất kiểm đếm & khóa đơn",
            purchaseOrderService.completeReceiving(id, request, principal.getName())));
  }
}
