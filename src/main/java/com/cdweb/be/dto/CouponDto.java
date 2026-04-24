package com.cdweb.be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
    @DecimalMin(value = "0.0", message = "Discount value must be positive")
    private BigDecimal discountValue;

    private BigDecimal minOrderValue;

    private BigDecimal maxDiscountAmount;

    private Integer usageLimit;

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
    private LocalDateTime dateStart;
    private LocalDateTime dateEnd;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Constructor to maintain compatibility with existing code
    public Response(Integer id, String code, BigDecimal discountValue) {
      this.id = id;
      this.code = code;
      this.discountValue = discountValue;
    }
  }
}
