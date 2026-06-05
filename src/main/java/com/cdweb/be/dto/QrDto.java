package com.cdweb.be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class QrDto {

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateRequest {
        private String qrType; // QR_LOGIN | QR_ORDER_CONFIRMATION
        private Integer orderId; // chỉ dùng khi qrType = QR_ORDER_CONFIRMATION
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GenerateResponse {
        private String sessionId;
        private String token;      // JWT ngắn hạn nhúng vào QR
        private String qrContent;  // URL / string để render QR
        private Long expiresInSeconds;
        private String status;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatusResponse {
        private String sessionId;
        private String status;     // PENDING | SCANNED | CONFIRMED | EXPIRED
        private String jwtToken;   // Trả về khi status = CONFIRMED (login QR)
        private UserDto.Response user;
        /** QR_LOGIN | QR_ORDER_CONFIRMATION */
        private String qrType;
        private Integer orderId;
        private String orderCode;
        private String orderStatus;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScanRequest {
        private String token; // JWT token đọc từ QR
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConfirmRequest {
        private String sessionId;
    }
}