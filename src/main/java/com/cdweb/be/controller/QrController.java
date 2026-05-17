package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.QrDto;
import com.cdweb.be.service.QrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.security.Principal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/qr")
@Tag(name = "QR Code", description = "Đăng nhập và xác nhận đơn hàng qua QR Code")
public class QrController {

    @Autowired private QrService qrService;

    /**
     * Bước 1: Web tạo QR → nhận sessionId + token để render QR
     * Public: không cần đăng nhập (cho luồng QR Login)
     */
    @PostMapping("/generate")
    @Operation(summary = "Tạo QR token", description = "Tạo mã QR ngắn hạn (TTL 2 phút) cho QR_LOGIN hoặc QR_ORDER_CONFIRMATION")
    public ResponseEntity<ApiResponse<QrDto.GenerateResponse>> generate(
            @RequestBody QrDto.GenerateRequest request) {
        QrDto.GenerateResponse response = qrService.generateQr(request);
        return ResponseEntity.ok(ApiResponse.success("Tạo QR thành công", response));
    }

    /**
     * Bước 2: Mobile quét QR → gửi token lên backend
     * Yêu cầu đăng nhập trên mobile
     */
    @PostMapping("/scan")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Quét QR", description = "Mobile gửi token QR để xác thực danh tính")
    public ResponseEntity<ApiResponse<QrDto.StatusResponse>> scan(
            @RequestBody QrDto.ScanRequest request, Principal principal) {
        QrDto.StatusResponse response = qrService.scanQr(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Quét QR thành công, chờ xác nhận", response));
    }

    /**
     * Bước 3: Mobile xác nhận → web nhận JWT để đăng nhập
     */
    @PostMapping("/confirm")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Xác nhận QR", description = "Mobile xác nhận đăng nhập, trả về JWT cho web")
    public ResponseEntity<ApiResponse<QrDto.StatusResponse>> confirm(
            @RequestBody QrDto.ConfirmRequest request, Principal principal) {
        QrDto.StatusResponse response = qrService.confirmQr(principal.getName(), request);
        return ResponseEntity.ok(ApiResponse.success("Xác nhận QR thành công", response));
    }

    /**
     * Polling: Web kiểm tra trạng thái QR (polling mỗi 2-3 giây)
     */
    @GetMapping("/status/{sessionId}")
    @Operation(summary = "Kiểm tra trạng thái QR", description = "Web polling để biết QR đã được scan/confirm chưa")
    public ResponseEntity<ApiResponse<QrDto.StatusResponse>> getStatus(@PathVariable String sessionId) {
        QrDto.StatusResponse response = qrService.getStatus(sessionId);
        return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái QR thành công", response));
    }
}