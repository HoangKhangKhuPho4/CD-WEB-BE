package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.SystemConfigDto;
import com.cdweb.be.service.SystemConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cấu hình công khai cho storefront (phí ship, cổng thanh toán bật/tắt). */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class PublicSettingsController {

  private final SystemConfigService systemConfigService;

  @GetMapping("/general")
  public ResponseEntity<ApiResponse<SystemConfigDto.GeneralSettingsResponse>> getGeneralSettings() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Lấy cấu hình cửa hàng thành công", systemConfigService.getGeneralSettings()));
  }
}
