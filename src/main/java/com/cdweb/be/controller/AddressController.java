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

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<AddressDto.Response>>> getMyAddresses() {
    String username = getCurrentUsername();
    List<AddressDto.Response> addresses = addressService.getAddressesByUsername(username);
    return ResponseEntity.ok(ApiResponse.success("Addresses retrieved successfully", addresses));
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<AddressDto.Response>> createAddress(
          @Valid @RequestBody AddressDto.Request request) {
    String username = getCurrentUsername();
    AddressDto.Response response = addressService.createAddress(username, request);
    return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success("Address created successfully", response));
  }

  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  // Sửa Integer id -> Long id
  public ResponseEntity<ApiResponse<AddressDto.Response>> updateAddress(
          @PathVariable Long id, @Valid @RequestBody AddressDto.Request request) {
    String username = getCurrentUsername();
    AddressDto.Response response = addressService.updateAddress(username, id, request);
    return ResponseEntity.ok(ApiResponse.success("Address updated successfully", response));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  // Sửa Integer id -> Long id
  public ResponseEntity<ApiResponse<Void>> deleteAddress(@PathVariable Long id) {
    String username = getCurrentUsername();
    addressService.deleteAddress(username, id);
    return ResponseEntity.ok(ApiResponse.success("Address deleted successfully", null));
  }

  @PutMapping("/{id}/default")
  @PreAuthorize("isAuthenticated()")
  // Sửa Integer id -> Long id
  public ResponseEntity<ApiResponse<AddressDto.Response>> setDefaultAddress(
          @PathVariable Long id) {
    String username = getCurrentUsername();
    AddressDto.Response response = addressService.setDefaultAddress(username, id);
    return ResponseEntity.ok(ApiResponse.success("Address set as default successfully", response));
  }

  private String getCurrentUsername() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    return authentication.getName();
  }
}