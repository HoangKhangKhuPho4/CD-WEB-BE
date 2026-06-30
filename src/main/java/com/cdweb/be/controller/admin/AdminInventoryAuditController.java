package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.InventoryAuditDto;
import com.cdweb.be.service.InventoryAuditService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/inventory-audit")
@RequiredArgsConstructor
public class AdminInventoryAuditController {

  private final InventoryAuditService inventoryAuditService;

  @GetMapping
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<InventoryAuditDto.SheetResponse>>> list() {
    return ResponseEntity.ok(
        ApiResponse.success("Danh sách phiếu kiểm kê", inventoryAuditService.listSheets()));
  }

  @GetMapping("/recent")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<InventoryAuditDto.SheetResponse>>> recent() {
    return ResponseEntity.ok(
        ApiResponse.success("Phiếu gần đây", inventoryAuditService.listRecentSheets()));
  }

  @GetMapping("/stats")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.StatsResponse>> stats() {
    return ResponseEntity.ok(
        ApiResponse.success("Thống kê kiểm kê", inventoryAuditService.getStats()));
  }

  @GetMapping("/pending")
  @PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<InventoryAuditDto.SheetResponse>>> pending() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Phiếu kiểm kê chờ duyệt", inventoryAuditService.listPendingApprovalSheets()));
  }

  @GetMapping("/processed")
  @PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<InventoryAuditDto.SheetResponse>>> processed() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Phiếu kiểm kê đã xử lý", inventoryAuditService.listProcessedSheets()));
  }

  @GetMapping("/{id}")
  @PreAuthorize(
      "hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'PRODUCT_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.SheetResponse>> get(@PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Chi tiết phiếu kiểm kê", inventoryAuditService.getSheet(id)));
  }

  @GetMapping("/{id}/scan-progress")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.ScanProgressResponse>> scanProgress(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tiến độ quét", inventoryAuditService.getScanProgress(id)));
  }

  @PostMapping("/start")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.SheetResponse>> start(
      @Valid @RequestBody InventoryAuditDto.StartRequest request, Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Bắt đầu kiểm kê",
            inventoryAuditService.startSheet(request, principal.getName())));
  }

  @PostMapping
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.SheetResponse>> create(
      @Valid @RequestBody InventoryAuditDto.CreateRequest request, Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tạo phiếu kiểm kê thành công",
            inventoryAuditService.createSheet(request, principal.getName())));
  }

  @PostMapping("/{id}/scan")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.ScanResponse>> scan(
      @PathVariable Integer id,
      @Valid @RequestBody InventoryAuditDto.ScanRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Kết quả quét", inventoryAuditService.scanCode(id, request)));
  }

  @PostMapping("/{id}/bulk-scan")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.BulkScanResponse>> bulkScan(
      @PathVariable Integer id,
      @Valid @RequestBody InventoryAuditDto.BulkScanRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Import quét hàng loạt", inventoryAuditService.bulkScan(id, request)));
  }

  @PostMapping("/{id}/complete")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.CompleteResponse>> complete(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Đối chiếu hoàn tất", inventoryAuditService.completeSheet(id)));
  }

  @PostMapping("/{id}/submit")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.SheetResponse>> submit(
      @PathVariable Integer id,
      @RequestBody(required = false) InventoryAuditDto.SubmitRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã gửi phiếu kiểm kê",
            inventoryAuditService.submitSheet(
                id, request != null ? request : new InventoryAuditDto.SubmitRequest())));
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.SheetResponse>> approve(
      @PathVariable Integer id, Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã duyệt và cân bằng kho",
            inventoryAuditService.approveSheet(id, principal.getName())));
  }

  @PostMapping("/{id}/reject")
  @PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.SheetResponse>> reject(
      @PathVariable Integer id, @RequestBody(required = false) InventoryAuditDto.RejectRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã từ chối phiếu kiểm kê",
            inventoryAuditService.rejectSheet(
                id, request != null ? request : new InventoryAuditDto.RejectRequest())));
  }

  @PatchMapping("/{id}/note")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryAuditDto.SheetResponse>> updateNote(
      @PathVariable Integer id, @RequestBody InventoryAuditDto.UpdateNoteRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Cập nhật ghi chú", inventoryAuditService.updateNote(id, request)));
  }
}
