package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/system")
@PreAuthorize(
    "hasAuthority('SYSTEM_CONFIG_MANAGE')") // Khóa toàn bộ Class chỉ cho Admin có quyền này
public class SystemConfigController {

  // ─── GET /api/admin/system/ai-config — Xem cấu hình AI hiện tại ─────────
  @GetMapping("/ai-config")
  public ResponseEntity<ApiResponse<String>> getAiConfiguration() {
    // Trả về các tham số độ trễ, hệ số nội suy SVD (Singular Value Decomposition)
    return ResponseEntity.ok(ApiResponse.success("Tải cấu hình AI (SVD) thành công", null));
  }

  // ─── PUT /api/admin/system/ai-config — Cập nhật Thuật toán AI ──────────
  @PutMapping("/ai-config")
  public ResponseEntity<ApiResponse<String>> updateAiConfiguration(
      // TODO: @RequestBody AiConfigDto request
      ) {
    // Điều chỉnh hệ số gợi ý, ép retrain model, quản lý epochs
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật thông số AI Recommendation Engine thành công", null));
  }

  // ─── POST /api/admin/system/ai-retrain — Bắt đầu Huấn luyện AI ──────────
  @PostMapping("/ai-retrain")
  public ResponseEntity<ApiResponse<String>> triggerAiRetrain() {
    // Nút bấm ép Model học lại thông tin thao tác của người dùng
    return ResponseEntity.ok(
        ApiResponse.success("Đang tiến hành đào tạo (Retrain) Model SVD...", null));
  }

  // ─── GET /api/admin/system/general — Xem Cấu hình chung ─────────────────
  @GetMapping("/general")
  public ResponseEntity<ApiResponse<String>> getGeneralSettings() {
    // Cấu hình tính phí Ship, Cổng thanh toán, Ngôn ngữ nền tảng
    return ResponseEntity.ok(ApiResponse.success("Tải cấu hình Hệ thống thành công", null));
  }

  // ─── PUT /api/admin/system/general — Lưu Cấu hình chung ─────────────────
  @PutMapping("/general")
  public ResponseEntity<ApiResponse<String>> updateGeneralSettings(
      // TODO: @RequestBody SystemSettingDto request
      ) {
    // Cập nhật Database System_Settings
    return ResponseEntity.ok(ApiResponse.success("Lưu cấu hình hệ thống thành công", null));
  }
}
