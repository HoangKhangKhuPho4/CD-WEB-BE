package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.WarrantyDto;
import com.cdweb.be.service.WarrantyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/warranty")
@CrossOrigin(origins = "*")
public class WarrantyController {

  @Autowired private WarrantyService warrantyService;

  /** Tra cứu đầy đủ: bảo hành, lịch sử mua, phiếu sửa chữa. */
  @GetMapping("/lookup/{code}")
  public ResponseEntity<ApiResponse<WarrantyDto.LookupResponse>> lookup(
      @PathVariable("code") String code) {
    WarrantyDto.LookupResponse response = warrantyService.lookupByCode(code);
    if (!response.isFound()) {
      return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage()));
    }
    return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
  }

  /** Giữ tương thích — trả thông tin bảo hành cơ bản. */
  @GetMapping("/check/{code}")
  public ResponseEntity<ApiResponse<WarrantyDto.Response>> checkWarranty(
      @PathVariable("code") String code) {
    WarrantyDto.Response response = warrantyService.checkWarranty(code);

    if (!response.isValid() && response.getProductName() == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage()));
    }

    return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
  }

  /** Khách gửi yêu cầu tiếp nhận bảo hành (không cần đăng nhập). */
  @PostMapping("/tickets")
  public ResponseEntity<ApiResponse<WarrantyDto.TicketResponse>> createPublicTicket(
      @Valid @RequestBody WarrantyDto.TicketRequest request) {
    WarrantyDto.TicketResponse ticket = warrantyService.createPublicWarrantyTicket(request);
    return ResponseEntity.ok(
        ApiResponse.success("Đã tiếp nhận yêu cầu bảo hành. Mã phiếu: " + ticket.getTicketCode(), ticket));
  }
}
