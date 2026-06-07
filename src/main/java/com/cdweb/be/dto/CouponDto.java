package com.cdweb.be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CouponDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotBlank(message = "Coupon code is required")
    private String code;

    private String name;
    private String description;

    @NotBlank(message = "Discount type is required (PERCENT or FIXED)")
    private String discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Discount value must be positive")
    private BigDecimal discountValue;

    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer perUserLimit;
    private Boolean firstOrderOnly = false;
    private String scopeType = "ALL";
    private Set<Integer> productIds;
    private Set<Integer> productTypeIds;

    @NotNull(message = "Start date is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateStart;

    @NotNull(message = "End date is required")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateEnd;

    private Boolean isActive = true;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String code;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer perUserLimit;
    private Boolean firstOrderOnly;
    private String scopeType;
    private Set<Integer> productIds;
    private Set<Integer> productTypeIds;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateStart;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateEnd;

    private Boolean isActive;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Integer id;
    private String code;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimit;
    private Integer usedCount;
    private Integer perUserLimit;
    private Boolean firstOrderOnly;
    private String scopeType;
    private Set<Integer> productIds;
    private Set<Integer> productTypeIds;
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String lifecycleStatus;

    public Response(Integer id, String code, BigDecimal discountValue) {
      this.id = id;
      this.code = code;
      this.discountValue = discountValue;
    }
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PublicResponse {
    private String code;
    private String name;
    private String description;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private LocalDateTime dateEnd;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminStatsResponse {
    private Long total;
    private Long active;
    private Long inactive;
    private Long expired;
    private Long upcoming;
    private Long exhausted;
    private Long totalUsedCount;
    private Long firstOrderOnlyCount;
  }

  @Data
  public static class BulkStatusRequest {
    @NotNull(message = "Danh sách ID không được trống")
    private List<Integer> ids;

    @NotNull(message = "Trạng thái active không được trống")
    private Boolean isActive;
  }

  @Data
  public static class CheckoutLineItem {
    private Integer productId;
    private Integer productTypeId;
    private BigDecimal lineTotal;
  }

  @Data
  public static class ValidateRequest {
    @NotBlank(message = "Mã coupon là bắt buộc")
    private String code;

    /** Dùng khi admin test hoặc chưa có giỏ hàng — tổng tiền giả định. */
    private BigDecimal subtotal;

    /** Khi có user + items → kiểm tra per-user, first-order, scope sản phẩm. */
    private Long userId;
    private List<CheckoutLineItem> items;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateResponse {
    private Boolean valid;
    private String code;
    private String message;
    private String discountType;
    private BigDecimal discountValue;
    private BigDecimal discountAmount;
    private BigDecimal originalSubtotal;
    private BigDecimal finalAmount;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UsageOrderSummary {
    private Integer orderId;
    private String orderCode;
    private String customerName;
    private String customerUsername;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String orderStatus;
    private LocalDateTime orderDate;
  }
}
