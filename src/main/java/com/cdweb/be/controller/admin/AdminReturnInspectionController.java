package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ReturnInspectionDto;
import com.cdweb.be.service.ReturnInspectionService;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/return-inspection")
@RequiredArgsConstructor
public class AdminReturnInspectionController {

  private final ReturnInspectionService returnInspectionService;

  @GetMapping("/pending")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ReturnInspectionDto.SheetSummary>>> pending() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Phiếu hoàn chờ kiểm định", returnInspectionService.listPending()));
  }

  @GetMapping("/processed")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ReturnInspectionDto.SheetSummary>>> processed() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Phiếu hoàn đã xử lý", returnInspectionService.listProcessed()));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReturnInspectionDto.SheetDetail>> detail(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Chi tiết phiếu hoàn", returnInspectionService.getDetail(id)));
  }

  @PostMapping("/intake")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReturnInspectionDto.IntakeResponse>> intake(
      @Valid @RequestBody ReturnInspectionDto.IntakeRequest request, Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tiếp nhận hàng hoàn",
            returnInspectionService.intake(request.getCode(), principal.getName())));
  }

  @GetMapping("/drafts")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ReturnInspectionDto.SheetSummary>>> drafts() {
    return ResponseEntity.ok(
        ApiResponse.success("Phiếu hoàn lưu tạm", returnInspectionService.listDrafts()));
  }

  @GetMapping("/{id}/defect-label")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReturnInspectionDto.DefectLabelResponse>> defectLabel(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Nhãn cách ly hàng lỗi", returnInspectionService.getDefectLabel(id)));
  }

  @PostMapping("/{id}/draft")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReturnInspectionDto.SheetDetail>> saveDraft(
      @PathVariable Integer id,
      @RequestBody ReturnInspectionDto.DraftRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã lưu tạm phiếu hoàn",
            returnInspectionService.saveDraft(id, request, principal.getName())));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReturnInspectionDto.SheetDetail>> cancel(
      @PathVariable Integer id,
      @Valid @RequestBody ReturnInspectionDto.CancelRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã hủy phiếu hoàn",
            returnInspectionService.cancel(id, request, principal.getName())));
  }

  @PostMapping("/{id}/process")
  @PreAuthorize("hasAnyAuthority('STOCK_RETURN', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReturnInspectionDto.SheetDetail>> process(
      @PathVariable Integer id,
      @RequestBody ReturnInspectionDto.ProcessRequest request,
      Principal principal) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đã xử lý phiếu hoàn",
            returnInspectionService.process(id, request, principal.getName())));
  }
}
