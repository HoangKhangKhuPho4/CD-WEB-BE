package com.cdweb.be.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class OrderManagementDto {

    // ── Admin: cập nhật trạng thái đơn hàng ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateStatusRequest {
        private String status; // pending|confirmed|shipping|delivered|cancelled
        private String note;
    }

    // ── Admin: cập nhật hàng loạt ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUpdateStatusRequest {
        private List<Integer> orderIds;
        private String status;
        private String note;
    }

    // ── Timeline item ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TimelineItem {
        private String status;
        private String note;
        private String changedBy;
        private LocalDateTime createdAt;
    }

    // ── Chi tiết đơn hàng (có timeline) ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderDetailResponse {
        private Integer id;
        private String orderCode;
        private String status;
        private String customerName;
        private String customerEmail;
        private String customerPhone;
        private String shippingAddress;
        private BigDecimal subtotal;
        private BigDecimal shippingFee;
        private BigDecimal discount;
        private BigDecimal total;
        private String paymentMethod;
        private String paymentStatus;
        private String ghnOrderCode;    // mã vận đơn GHN
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private List<OrderItemResponse> items;
        private List<TimelineItem> timeline;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Integer productId;
        private String productName;
        private String variantInfo; // size/màu
        private String imageUrl;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal subtotal;
    }

    // ── Admin: danh sách đơn rút gọn ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderSummaryResponse {
        private Integer id;
        private String orderCode;
        private String status;
        private String customerName;
        private String customerEmail;
        private BigDecimal total;
        private String paymentMethod;
        private String paymentStatus;
        private LocalDateTime createdAt;
    }

    // ── Kết quả bulk update ──
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BulkUpdateResult {
        private int successCount;
        private int failCount;
        private List<String> errors;
    }
}