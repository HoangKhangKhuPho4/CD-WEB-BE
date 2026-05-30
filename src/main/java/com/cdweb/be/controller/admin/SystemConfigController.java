package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.SystemConfigDto;
import com.cdweb.be.service.SystemConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/system")
@PreAuthorize("hasAnyAuthority('SYSTEM_CONFIG_MANAGE', 'ROLE_ADMIN')")
@RequiredArgsConstructor
public class SystemConfigController {

  private final SystemConfigService systemConfigService;

  @GetMapping("/ai-config")
  public ResponseEntity<ApiResponse<SystemConfigDto.AiConfigResponse>> getAiConfiguration() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tải cấu hình AI (SVD) thành công", systemConfigService.getAiConfiguration()));
  }

  @PutMapping("/ai-config")
  public ResponseEntity<ApiResponse<SystemConfigDto.AiConfigResponse>> updateAiConfiguration(
      @Valid @RequestBody SystemConfigDto.AiConfigUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Cập nhật thông số AI Recommendation Engine thành công",
            systemConfigService.updateAiConfiguration(request)));
  }

  @PostMapping("/ai-retrain")
  public ResponseEntity<ApiResponse<SystemConfigDto.RetrainResponse>> triggerAiRetrain() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Đang tiến hành đào tạo (Retrain) Model SVD...",
            systemConfigService.triggerAiRetrain()));
  }

  @GetMapping("/general")
  public ResponseEntity<ApiResponse<SystemConfigDto.GeneralSettingsResponse>> getGeneralSettings() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Tải cấu hình Hệ thống thành công", systemConfigService.getGeneralSettings()));
  }

  @PutMapping("/general")
  public ResponseEntity<ApiResponse<SystemConfigDto.GeneralSettingsResponse>> updateGeneralSettings(
      @Valid @RequestBody SystemConfigDto.GeneralSettingsUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Lưu cấu hình hệ thống thành công",
            systemConfigService.updateGeneralSettings(request)));
  }
}
