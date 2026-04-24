package com.cdweb.be.controller;

import com.cdweb.be.dto.AddressDto;
import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.service.AddressService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/addresses")
@CrossOrigin(origins = "*")
public class AddressController {

  @Autowired private AddressService addressService;

  // ─────────────────────────────────────────────────────────────────────────
  // GET /api/addresses  → Lấy danh sách tất cả địa chỉ của user
  // ─────────────────────────────────────────────────────────────────────────
  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<AddressDto.Response>>> getMyAddresses() {
    String username = getCurrentUsername();
    List<AddressDto.Response> addresses = addressService.getAddressesByUsername(username);
    return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", addresses));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // POST /api/addresses  → Thêm địa chỉ mới
  // ─────────────────────────────────────────────────────────────────────────
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<AddressDto.Response>> createAddress(
      @Valid @RequestBody AddressDto.Request request) {
    String username = getCurrentUsername();
    AddressDto.Response response = addressService.createAddress(username, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Address created successfully", response));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // PUT /api/addresses/{id}  → Cập nhật địa chỉ
  // ─────────────────────────────────────────────────────────────────────────
  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<AddressDto.Response>> updateAddress(
      @PathVariable Integer id, @Valid @RequestBody AddressDto.Request request) {
    String username = getCurrentUsername();
    AddressDto.Response response = addressService.updateAddress(username, id, request);
    return ResponseEntity.ok(ApiResponse.success("Address updated successfully", response));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // DELETE /api/addresses/{id}  → Xóa địa chỉ
  // ─────────────────────────────────────────────────────────────────────────
  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Integer id) {
    String username = getCurrentUsername();
    addressService.deleteAddress(username, id);
    return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
  }

  // ─────────────────────────────────────────────────────────────────────────
  // PUT /api/addresses/{id}/default  → Đặt địa chỉ làm mặc định nhanh
  // ─────────────────────────────────────────────────────────────────────────
  @PutMapping("/{id}/default")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<AddressDto.Response>> setDefaultAddress(
      @PathVariable Integer id) {
    String username = getCurrentUsername();
    AddressDto.Response response = addressService.setDefaultAddress(username, id);
    return ResponseEntity.ok(ApiResponse.success("Address set as default successfully", response));
  }

  // ─────────────────────────────────────────────────────────────────────────
  private String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }
}
