package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class OrderDto {

  // ─── Request: Checkout từ giỏ hàng ────────────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CheckoutRequest {

    // Dùng địa chỉ đã lưu — hoặc nhập thẳng bên dưới
    private Long addressId;

    // Thông tin giao hàng inline (bắt buộc nếu không dùng addressId)
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;

    // GHN Address IDs — dùng để tính phí ship động
    // Lấy từ: GET /api/shipping/districts và GET /api/shipping/wards
    private Integer toDistrictId; // District ID theo GHN (VD: 3695)
    private String toWardCode; // Ward Code theo GHN (VD: "90737")

    // Phương thức thanh toán: COD, BANK_TRANSFER, MOMO, VNPAY, ZALOPAY
    @NotBlank(message = "Payment method is required")
    private String paymentMethod;

    // Mã giảm giá (không bắt buộc)
    private String couponCode;

    // Ghi chú
    private String note;
  }

  // ─── Request: Hủy đơn ─────────────────────────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CancelRequest {
    private String reason;
  }

  // ─── Response: Chi tiết 1 item trong đơn ──────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderItemResponse {
    private Integer id;
    private Integer variantId;
    private String productName;
    private String variantName;
    private String skuCode;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String imageUrl;
    private List<String> assignedImeis;
  }

  // ─── Response: Thông tin coupon đã áp dụng ────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CouponInfoResponse {
    private String code;
    private String discountType; // PERCENT / FIXED
    private BigDecimal discountValue;
    private BigDecimal discountAmount; // số tiền thực tế được giảm
  }

  // ─── Response: Đơn hàng đầy đủ ───────────────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderResponse {
    private Integer id;
    private String orderCode;

    // Thông tin giao hàng
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private BigDecimal shippingFee;
    private String trackingCode;
    private String ghnOrderCode;

    // Thanh toán & trạng thái
    private String paymentMethod;
    private String paymentStatus;
    private String status;
    private String statusDisplay; // tên tiếng Việt

    // ── Payment Gateway fields ──
    private String paymentUrl; // URL thanh toán online (null nếu COD)
    private String transactionRef; // mã giao dịch nội bộ

    // Tiền
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // Coupon
    private CouponInfoResponse couponInfo;

    // Ghi chú
    private String note;
    private String cancelReason;

    // Thời gian
    private LocalDateTime orderDate;
    private LocalDateTime confirmedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    // Sản phẩm
    private List<OrderItemResponse> items;

    /** Lịch sử trạng thái đơn hàng (khách hàng & chi tiết đơn) */
    private List<TimelineItem> timeline;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TimelineItem {
    private String status;
    private String note;
    private String changedBy;
    private LocalDateTime createdAt;
  }

  // ─── Response: Tóm tắt đơn (dùng cho danh sách) ──────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderSummaryResponse {
    private Integer id;
    private String orderCode;
    private String status;
    private String statusDisplay;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private String firstItemName; // tên sản phẩm đầu tiên
    private String firstItemImage; // ảnh sản phẩm đầu tiên
    private LocalDateTime orderDate;
  }

  // ─── Response: Preview giảm giá khi áp coupon ─────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ApplyCouponResponse {
    private String couponCode;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal originalSubtotal;
    private BigDecimal discountAmount;
    private BigDecimal finalAmount;
    private String message;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ADMIN — Request / Response cho quản lý đơn hàng
  // ═══════════════════════════════════════════════════════════════════════════

  // ─── Request: Admin cập nhật trạng thái đơn ───────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateStatusRequest {
    @NotBlank(message = "Status is required")
    private String
        status; // CONFIRMED, PROCESSING, SHIPPING, DELIVERED, COMPLETED, CANCELLED, REFUNDED

    private String adminNote; // ghi chú của admin
    private String trackingCode; // mã vận đơn (khi chuyển sang SHIPPING)
    private String cancelReason; // lý do hủy (khi admin hủy đơn)
  }

  // ─── Request: Admin cập nhật trạng thái thanh toán ────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdatePaymentStatusRequest {
    @NotBlank(message = "Payment status is required")
    private String paymentStatus; // PENDING, PAID, FAILED, REFUNDED
  }

  // ─── Request: Admin gán IMEI cho một đơn hàng (từng dòng chi tiết) ────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AssignImeiRequest {
    @jakarta.validation.constraints.NotNull(message = "Order detail ID is required")
    private Integer orderDetailId;

    @jakarta.validation.constraints.NotEmpty(message = "IMEI/Serial list cannot be empty")
    private List<String> imeis;
  }

  // ─── Request: Admin ẩn / hiện đơn hàng (Soft Delete) ──────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateVisibilityRequest {
    // true = ẩn đơn; false = hiện lại đơn
    private Boolean hidden;
    // Lý do ẩn (tùy chọn)
    private String reason;
  }

  // ─── Response: Đơn hàng cho admin (thêm thông tin user + adminNote) ──────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminOrderResponse {
    private Integer id;
    private String orderCode;

    // Thông tin khách hàng
    private Long userId;
    private String username;
    private String customerName;
    private String customerEmail;
    private String customerPhone;

    // Thông tin giao hàng
    private String shippingName;
    private String shippingPhone;
    private String shippingAddress;
    private String shippingProvince;
    private String shippingDistrict;
    private String shippingWard;
    private BigDecimal shippingFee;
    private String trackingCode;

    // Thanh toán & trạng thái
    private String paymentMethod;
    private String paymentStatus;
    private String status;
    private String statusDisplay;

    // Tiền
    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    // Coupon
    private CouponInfoResponse couponInfo;

    // Ghi chú
    private String note;
    private String adminNote;
    private String cancelReason;

    // Soft Delete
    private Boolean isHidden;
    private LocalDateTime hiddenAt;
    private String hiddenReason;

    // Thời gian
    private LocalDateTime orderDate;
    private LocalDateTime confirmedAt;
    private LocalDateTime shippedAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime cancelledAt;

    // Sản phẩm
    private List<OrderItemResponse> items;
  }

  // ─── Response: Tóm tắt đơn cho admin (thêm username) ────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminOrderSummaryResponse {
    private Integer id;
    private String orderCode;
    private String status;
    private String statusDisplay;
    private String paymentMethod;
    private String paymentStatus;
    private BigDecimal totalAmount;
    private Integer totalItems;
    private String firstItemName;
    private String firstItemImage;
    private LocalDateTime orderDate;

    // Thông tin khách hàng
    private Long userId;
    private String username;
    private String customerName;

    // Soft Delete flag
    private Boolean isHidden;
  }

  // ─── Response: Thống kê đơn hàng ─────────────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderStatsResponse {
    private Long totalOrders;
    private Long pendingOrders;
    private Long confirmedOrders;
    private Long processingOrders;
    private Long shippingOrders;
    private Long deliveredOrders;
    private Long completedOrders;
    private Long cancelledOrders;
    private Long refundedOrders;
    private Long hiddenOrders; // số đơn đang bị ẩn
  }
}
