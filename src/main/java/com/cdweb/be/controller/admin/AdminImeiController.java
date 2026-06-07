package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ImeiDto;
import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import com.cdweb.be.service.ImeiService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/** Admin API — Quản lý IMEI / Serial (đủ scenario kiểm thử). */
@RestController
@RequestMapping("/api/admin/imei")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminImeiController {

  private final ImeiService imeiService;

  // ── Stats ─────────────────────────────────────────────────────────────────

  @GetMapping("/stats")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'INVENTORY_STAT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ImeiDto.StatsResponse>> getStats() {
    return ResponseEntity.ok(
        ApiResponse.success("Thống kê IMEI", imeiService.getStats()));
  }

  // ── List & filters ────────────────────────────────────────────────────────

  @GetMapping
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Page<ImeiDto.ListItem>>> list(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) ProductItemStatus status,
      @RequestParam(required = false) Integer variantId,
      @RequestParam(required = false) String orderCode,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    Sort sort =
        "asc".equalsIgnoreCase(sortDir)
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<ImeiDto.ListItem> result =
        imeiService.list(keyword, status, variantId, orderCode, fromDate, toDate, pageable);
    return ResponseEntity.ok(ApiResponse.success("Danh sách IMEI", result));
  }

  // ── Export CSV ────────────────────────────────────────────────────────────

  @GetMapping("/export")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<byte[]> exportCsv(
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) ProductItemStatus status,
      @RequestParam(required = false) Integer variantId,
      @RequestParam(required = false) String orderCode,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate fromDate,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
          LocalDate toDate) {
    byte[] csv = imeiService.exportCsv(keyword, status, variantId, orderCode, fromDate, toDate);
    return ResponseEntity.ok()
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=imei-export.csv")
        .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
        .body(csv);
  }

  // ── Lookup by code ────────────────────────────────────────────────────────

  @GetMapping("/lookup/{code}")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ImeiDto.DetailResponse>> lookup(@PathVariable String code) {
    return ResponseEntity.ok(
        ApiResponse.success("Chi tiết IMEI", imeiService.lookupByCode(code)));
  }

  // ── Validate before import ────────────────────────────────────────────────

  @PostMapping("/validate")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ImeiDto.ValidateResponse>> validate(
      @Valid @RequestBody ImeiDto.ValidateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Kiểm tra IMEI", imeiService.validate(request)));
  }

  // ── Create ────────────────────────────────────────────────────────────────

  @PostMapping
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> create(@Valid @RequestBody ImeiDto.CreateRequest request) {
    imeiService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Đã lưu danh sách IMEI", null));
  }

  // ── Excel import ──────────────────────────────────────────────────────────

  @PostMapping("/upload-excel")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ImeiDto.ImportResult>> uploadExcel(
      @RequestParam("file") MultipartFile file) {
    return ResponseEntity.ok(
        ApiResponse.success("Import Excel", imeiService.importFromExcel(file)));
  }

  // ── Return stock ──────────────────────────────────────────────────────────

  @PostMapping("/return")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> returnStock(
      @Valid @RequestBody ImeiDto.ReturnRequest request) {
    imeiService.returnStock(request);
    return ResponseEntity.ok(ApiResponse.success("Đã ghi nhận hàng trả lại", null));
  }

  // ── Bulk status ───────────────────────────────────────────────────────────

  @PatchMapping("/bulk-status")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ImeiDto.BulkStatusResult>> bulkStatus(
      @Valid @RequestBody ImeiDto.BulkStatusRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật hàng loạt", imeiService.bulkStatus(request)));
  }

  // ── Detail by ID ──────────────────────────────────────────────────────────

  @GetMapping("/{id:\\d+}")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ImeiDto.DetailResponse>> getById(@PathVariable Integer id) {
    return ResponseEntity.ok(ApiResponse.success("Chi tiết IMEI", imeiService.getById(id)));
  }

  // ── Update metadata ───────────────────────────────────────────────────────

  @PatchMapping("/{id:\\d+}")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ImeiDto.ListItem>> update(
      @PathVariable Integer id, @Valid @RequestBody ImeiDto.UpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật IMEI", imeiService.update(id, request)));
  }

  // ── Status change (validated) ─────────────────────────────────────────────

  @PutMapping("/{id:\\d+}/status")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ImeiDto.ListItem>> updateStatus(
      @PathVariable Integer id, @Valid @RequestBody ImeiDto.StatusUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật trạng thái", imeiService.updateStatus(id, request)));
  }

  // ── Release from order ────────────────────────────────────────────────────

  @PostMapping("/{id:\\d+}/release")
  @PreAuthorize("hasAnyAuthority('IMEI_MANAGE', 'ORDER_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ImeiDto.ReleaseResponse>> release(@PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Giải phóng IMEI", imeiService.releaseFromOrder(id)));
  }
}
