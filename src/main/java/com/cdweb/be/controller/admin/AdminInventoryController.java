package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ImeiRequest;
import com.cdweb.be.dto.ImportStockRequest;
import com.cdweb.be.dto.InventoryStatDto;
import com.cdweb.be.dto.ReturnStockRequest;
import com.cdweb.be.service.InventoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/inventory")
@RequiredArgsConstructor
public class AdminInventoryController {

  private final InventoryService inventoryService;

  // ─── POST /api/admin/inventory/import — Lập phiếu nhập kho ──────────────
  @PostMapping("/import")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<String>> importStock(
      @Valid @RequestBody ImportStockRequest request) {
    inventoryService.importStock(request);
    return ResponseEntity.ok(ApiResponse.success("Lập phiếu nhập kho thành công", null));
  }

  // ─── POST /api/admin/inventory/return — Xử lý hàng trả lại ──────────────
  @PostMapping("/return")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<String>> returnStock(
      @Valid @RequestBody ReturnStockRequest request) {
    inventoryService.returnStock(request);
    return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận kho cho hàng trả lại", null));
  }

  // ─── GET /api/admin/inventory/stats — Quản lý Báo cáo tồn kho ───────────
  @GetMapping("/stats")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<InventoryStatDto>>> getInventoryStats(
      @RequestParam(defaultValue = "10") int lowStockThreshold) {
    List<InventoryStatDto> stats = inventoryService.getInventoryStats(lowStockThreshold);
    return ResponseEntity.ok(ApiResponse.success("Báo cáo tồn kho và cảnh báo hết hàng", stats));
  }

  // ─── POST /api/admin/inventory/imei — Gán mã số IMEI cho kiện hàng ──────
  @PostMapping("/imei")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<String>> addImeiToProduct(
      @Valid @RequestBody ImeiRequest request) {
    inventoryService.addImeiToProduct(request);
    return ResponseEntity.ok(ApiResponse.success("Đã lưu danh sách IMEI vào hệ thống", null));
  }

  // ─── GET /api/admin/inventory/variants/search — Tra cứu variant cho Autocomplete ───────────
  @GetMapping("/variants/search")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<com.cdweb.be.dto.VariantAutocompleteDto>>> searchVariants(
      @RequestParam("q") String keyword) {
    List<com.cdweb.be.dto.VariantAutocompleteDto> variants =
        inventoryService.searchVariants(keyword);
    return ResponseEntity.ok(ApiResponse.success("Kết quả tra cứu", variants));
  }

  // ─── POST /api/admin/inventory/imei/upload-excel — Import IMEI hàng loạt từ Excel ───────────
  @PostMapping("/imei/upload-excel")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<String>> uploadImeiExcel(
      @RequestParam("file") MultipartFile file) {
    inventoryService.importImeiFromExcel(file);
    return ResponseEntity.ok(ApiResponse.success("Import IMEI từ file Excel thành công", null));
  }

  // ─── GET /api/admin/inventory/transactions — Xem lịch sử biến động kho ───────────
  @GetMapping("/transactions")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<com.cdweb.be.dto.InventoryResponseDto>>>
      getInventoryTransactions() {
    List<com.cdweb.be.dto.InventoryResponseDto> transactions =
        inventoryService.getInventoryTransactions();
    return ResponseEntity.ok(ApiResponse.success("Lịch sử biến động kho", transactions));
  }
}
