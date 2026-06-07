package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.AdjustStockRequest;
import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ImeiRequest;
import com.cdweb.be.dto.ImportStockRequest;
import com.cdweb.be.dto.InventoryDto;
import com.cdweb.be.dto.InventoryResponseDto;
import com.cdweb.be.dto.InventoryStatDto;
import com.cdweb.be.dto.ReturnQuantityRequest;
import com.cdweb.be.dto.ReturnStockRequest;
import com.cdweb.be.entity.Inventory.TransactionType;
import com.cdweb.be.service.InventoryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

  // ─── POST /api/admin/inventory/import/validate — Kiểm tra trước khi nhập ─
  @PostMapping("/import/validate")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryDto.ValidateImportResponse>> validateImport(
      @Valid @RequestBody ImportStockRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Kết quả kiểm tra phiếu nhập", inventoryService.validateImport(request)));
  }

  // ─── POST /api/admin/inventory/adjust — Điều chỉnh tồn kho (+/-) ─────────
  @PostMapping("/adjust")
  @PreAuthorize("hasAnyAuthority('STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<String>> adjustStock(
      @Valid @RequestBody AdjustStockRequest request) {
    inventoryService.adjustStock(request);
    return ResponseEntity.ok(ApiResponse.success("Điều chỉnh tồn kho thành công", null));
  }

  // ─── POST /api/admin/inventory/return — Xử lý hàng trả lại (IMEI) ────────
  @PostMapping("/return")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<String>> returnStock(
      @Valid @RequestBody ReturnStockRequest request) {
    inventoryService.returnStock(request);
    return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận kho cho hàng trả lại", null));
  }

  // ─── POST /api/admin/inventory/return-quantity — Trả hàng theo số lượng ──
  @PostMapping("/return-quantity")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<String>> returnQuantity(
      @Valid @RequestBody ReturnQuantityRequest request) {
    inventoryService.returnQuantity(request);
    return ResponseEntity.ok(
        ApiResponse.success("Đã ghi nhận trả hàng theo số lượng", null));
  }

  // ─── GET /api/admin/inventory/stats — Quản lý Báo cáo tồn kho ───────────
  @GetMapping("/stats")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<InventoryStatDto>>> getInventoryStats(
      @RequestParam(defaultValue = "10") int lowStockThreshold) {
    List<InventoryStatDto> stats = inventoryService.getInventoryStats(lowStockThreshold);
    return ResponseEntity.ok(ApiResponse.success("Báo cáo tồn kho và cảnh báo hết hàng", stats));
  }

  // ─── GET /api/admin/inventory/stats/export — Export CSV báo cáo tồn kho ──
  @GetMapping("/stats/export")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'ROLE_ADMIN')")
  public ResponseEntity<byte[]> exportStatsCsv(
      @RequestParam(defaultValue = "10") int lowStockThreshold) {
    byte[] csv = inventoryService.exportStatsCsv(lowStockThreshold);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-stats.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv);
  }

  // ─── POST /api/admin/inventory/imei — Gán mã số IMEI cho kiện hàng ──────
  @PostMapping("/imei")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<String>> addImeiToProduct(
      @Valid @RequestBody ImeiRequest request) {
    inventoryService.addImeiToProduct(request);
    return ResponseEntity.ok(ApiResponse.success("Đã lưu danh sách IMEI vào hệ thống", null));
  }

  // ─── GET /api/admin/inventory/variants/search — Tra cứu variant ──────────
  @GetMapping("/variants/search")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'STOCK_IMPORT', 'STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<com.cdweb.be.dto.VariantAutocompleteDto>>> searchVariants(
      @RequestParam("q") String keyword) {
    List<com.cdweb.be.dto.VariantAutocompleteDto> variants =
        inventoryService.searchVariants(keyword);
    return ResponseEntity.ok(ApiResponse.success("Kết quả tra cứu", variants));
  }

  // ─── POST /api/admin/inventory/imei/upload-excel — Import IMEI từ Excel ──
  @PostMapping("/imei/upload-excel")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<String>> uploadImeiExcel(
      @RequestParam("file") MultipartFile file) {
    inventoryService.importImeiFromExcel(file);
    return ResponseEntity.ok(ApiResponse.success("Import IMEI từ file Excel thành công", null));
  }

  // ─── GET /api/admin/inventory/transactions — Lịch sử biến động kho ───────
  @GetMapping("/transactions")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'ROLE_ADMIN')")
  public ResponseEntity<?> getInventoryTransactions(
      @RequestParam(required = false) Integer variantId,
      @RequestParam(required = false) TransactionType transactionType,
      @RequestParam(required = false) String referenceType,
      @RequestParam(required = false) Integer referenceId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate,
      @RequestParam(required = false) Integer page,
      @RequestParam(required = false) Integer size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    if (page != null || size != null) {
      int p = page != null ? page : 0;
      int s = size != null ? size : 15;
      Sort sort =
          "asc".equalsIgnoreCase(sortDir)
              ? Sort.by(sortBy).ascending()
              : Sort.by(sortBy).descending();
      Pageable pageable = PageRequest.of(p, s, sort);
      Page<InventoryResponseDto> result =
          inventoryService.getInventoryTransactionsPaged(
              variantId, transactionType, referenceType, referenceId, fromDate, toDate, pageable);
      return ResponseEntity.ok(ApiResponse.success("Lịch sử biến động kho", result));
    }
    List<InventoryResponseDto> transactions =
        inventoryService.getInventoryTransactions(
            variantId, transactionType, referenceType, referenceId, fromDate, toDate);
    return ResponseEntity.ok(ApiResponse.success("Lịch sử biến động kho", transactions));
  }

  // ─── GET /api/admin/inventory/transactions/{id} — Chi tiết 1 giao dịch ───
  @GetMapping("/transactions/{id}")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<InventoryResponseDto>> getTransactionById(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Chi tiết giao dịch kho", inventoryService.getTransactionById(id)));
  }

  // ─── GET /api/admin/inventory/transactions/export — Export CSV lịch sử ───
  @GetMapping("/transactions/export")
  @PreAuthorize("hasAnyAuthority('INVENTORY_STAT', 'ROLE_ADMIN')")
  public ResponseEntity<byte[]> exportTransactionsCsv(
      @RequestParam(required = false) Integer variantId,
      @RequestParam(required = false) TransactionType transactionType,
      @RequestParam(required = false) String referenceType,
      @RequestParam(required = false) Integer referenceId,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate) {
    byte[] csv =
        inventoryService.exportTransactionsCsv(
            variantId, transactionType, referenceType, referenceId, fromDate, toDate);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=inventory-transactions.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv);
  }

  // ─── GET /api/admin/inventory/product-items — Danh sách IMEI/Serial ──────
  @GetMapping("/product-items")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Page<com.cdweb.be.dto.ProductItemListDto>>> listProductItems(
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "15") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    return ResponseEntity.ok(
        ApiResponse.success("Danh sách IMEI", inventoryService.listProductItems(keyword, pageable)));
  }
}
