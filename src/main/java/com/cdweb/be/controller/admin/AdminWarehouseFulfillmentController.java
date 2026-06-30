package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.WarehouseFulfillmentDto;
import com.cdweb.be.service.WarehouseFulfillmentService;
import jakarta.validation.Valid;
import java.security.Principal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/warehouse")
@RequiredArgsConstructor
public class AdminWarehouseFulfillmentController {

  private final WarehouseFulfillmentService warehouseFulfillmentService;

  @GetMapping("/fulfillment-queue")
  @PreAuthorize("hasAnyAuthority('ORDER_VIEW_ALL', 'ORDER_ASSIGN_SHIPPING', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Page<WarehouseFulfillmentDto.QueueItem>>> fulfillmentQueue(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) String status,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "15") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "orderDate"));
    return ResponseEntity.ok(
        ApiResponse.success(
            "Hàng đợi xuất kho",
            warehouseFulfillmentService.fulfillmentQueue(keyword, status, pageable)));
  }

  @GetMapping("/orders/{id}/fulfillment")
  @PreAuthorize("hasAnyAuthority('ORDER_VIEW_ALL', 'ORDER_ASSIGN_SHIPPING', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarehouseFulfillmentDto.FulfillmentDetail>> fulfillmentDetail(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Chi tiết xuất kho", warehouseFulfillmentService.getFulfillmentDetail(id)));
  }

  @PostMapping("/orders/{id}/start-picking")
  @PreAuthorize("hasAnyAuthority('ORDER_ASSIGN_SHIPPING', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarehouseFulfillmentDto.FulfillmentDetail>> startPicking(
      @PathVariable Integer id, Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã bắt đầu gom hàng",
            warehouseFulfillmentService.startPicking(id, principal.getName())));
  }

  @GetMapping("/fifo-serials")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ORDER_ASSIGN_SHIPPING', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarehouseFulfillmentDto.FifoSerialsResponse>> fifoSerials(
      @RequestParam Integer variantId,
      @RequestParam(defaultValue = "1") int limit) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Gợi ý serial FIFO",
            warehouseFulfillmentService.getFifoSerials(variantId, limit)));
  }

  @PostMapping("/orders/{id}/validate-scan")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ORDER_ASSIGN_SHIPPING', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarehouseFulfillmentDto.ValidateScanResponse>> validateScan(
      @PathVariable Integer id, @Valid @RequestBody WarehouseFulfillmentDto.ValidateScanRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Kết quả kiểm tra mã quét",
            warehouseFulfillmentService.validateScan(id, request)));
  }

  @PostMapping("/orders/{id}/assign-serial")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ORDER_ASSIGN_SHIPPING', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarehouseFulfillmentDto.PickingProgress>> assignSerial(
      @PathVariable Integer id,
      @Valid @RequestBody WarehouseFulfillmentDto.AssignSerialRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã gán serial vào đơn",
            warehouseFulfillmentService.assignSerial(id, request, principal.getName())));
  }

  @GetMapping("/orders/{id}/picking-progress")
  @PreAuthorize("hasAnyAuthority('ORDER_VIEW_ALL', 'ORDER_ASSIGN_SHIPPING', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarehouseFulfillmentDto.PickingProgress>> pickingProgress(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tiến độ quét serial", warehouseFulfillmentService.getPickingProgress(id)));
  }

  @PostMapping("/orders/{id}/dispatch")
  @PreAuthorize("hasAnyAuthority('ORDER_ASSIGN_SHIPPING', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarehouseFulfillmentDto.DispatchResponse>> dispatch(
      @PathVariable Integer id, Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã bàn giao vận chuyển",
            warehouseFulfillmentService.dispatch(id, principal.getName())));
  }
}
