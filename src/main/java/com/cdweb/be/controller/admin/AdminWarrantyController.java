package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.WarrantyDto;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.service.WarrantyService;
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
@RequestMapping("/api/admin/warranty")
@CrossOrigin(origins = "*")
public class AdminWarrantyController {

  @Autowired private WarrantyService warrantyService;

  @PutMapping("/{code}/status")
  @PreAuthorize("hasAnyAuthority('WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarrantyDto.Response>> updateWarrantyStatus(
      @PathVariable("code") String code,
      @RequestParam("status") ProductItem.ProductItemStatus status) {

    WarrantyDto.Response response = warrantyService.updateWarrantyStatus(code, status);

    if (!response.isValid() && response.getProductName() == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage()));
    }

    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật trạng thái thiết bị thành công!", response));
  }

  // ─── POST /api/admin/warranty/tickets — Tạo phiếu bảo hành mới ────────────────
  @PostMapping("/tickets")
  @PreAuthorize("hasAnyAuthority('WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarrantyDto.TicketResponse>> createTicket(
      @RequestBody WarrantyDto.TicketRequest request) {

    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    WarrantyDto.TicketResponse ticket = warrantyService.createWarrantyTicket(username, request);
    return ResponseEntity.ok(ApiResponse.success("Đã tạo phiếu tiếp nhận bảo hành", ticket));
  }

  // ─── GET /api/admin/warranty/tickets — Lấy danh sách phiếu bảo hành ──────────
  @GetMapping("/tickets")
  @PreAuthorize("hasAnyAuthority('WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Page<WarrantyDto.TicketResponse>>> getAllTickets(
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "status", required = false) String status,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size, Sort.by("receivedAt").descending());
    Page<WarrantyDto.TicketResponse> tickets =
        warrantyService.getAllTickets(keyword, status, pageable);
    return ResponseEntity.ok(ApiResponse.success("Danh sách phiếu bảo hành", tickets));
  }

  // ─── GET /api/admin/warranty/tickets/{id} — Lấy chi tiết phiếu bảo hành ──────
  @GetMapping("/tickets/{id}")
  @PreAuthorize("hasAnyAuthority('WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarrantyDto.TicketResponse>> getTicketById(
      @PathVariable("id") Integer id) {

    WarrantyDto.TicketResponse ticket = warrantyService.getTicketById(id);
    return ResponseEntity.ok(ApiResponse.success("Chi tiết phiếu bảo hành", ticket));
  }

  // ─── PUT /api/admin/warranty/tickets/{id}/status — Cập nhật phiếu bảo hành ───
  @PutMapping("/tickets/{id}/status")
  @PreAuthorize("hasAnyAuthority('WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<WarrantyDto.TicketResponse>> updateTicketStatus(
      @PathVariable("id") Integer id, @RequestBody WarrantyDto.TicketUpdateAdminRequest request) {

    WarrantyDto.TicketResponse ticket = warrantyService.updateTicketStatus(id, request);
    return ResponseEntity.ok(ApiResponse.success("Đã cập nhật trạng thái phiếu bảo hành", ticket));
  }
}
