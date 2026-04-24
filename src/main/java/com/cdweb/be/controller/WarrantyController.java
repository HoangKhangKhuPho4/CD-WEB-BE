package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.WarrantyDto;
import com.cdweb.be.service.WarrantyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/warranty")
@CrossOrigin(origins = "*")
public class WarrantyController {

  @Autowired private WarrantyService warrantyService;

  @GetMapping("/check/{code}")
  public ResponseEntity<ApiResponse<WarrantyDto.Response>> checkWarranty(
      @PathVariable("code") String code) {
    WarrantyDto.Response response = warrantyService.checkWarranty(code);

    if (!response.isValid() && response.getProductName() == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error(response.getMessage()));
    }

    return ResponseEntity.ok(ApiResponse.success(response.getMessage(), response));
  }
}
