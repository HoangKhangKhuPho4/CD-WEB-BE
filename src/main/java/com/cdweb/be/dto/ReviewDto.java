package com.cdweb.be.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ReviewDto {

  // ─── Request: Tạo / cập nhật đánh giá ────────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotNull(message = "Product ID is required")
    private Integer productId;

    private Integer variantId; // biến thể đã mua (tùy chọn)
    private Integer orderId; // đơn hàng liên quan (tùy chọn)

    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    private String content;

    @Size(max = 1000, message = "Pros must not exceed 1000 characters")
    private String pros;

    @Size(max = 1000, message = "Cons must not exceed 1000 characters")
    private String cons;

    private List<String> images; // Hình ảnh đính kèm (mảng URL)
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    @NotNull(message = "Rating is required")
    @Min(value = 1, message = "Rating must be at least 1")
    @Max(value = 5, message = "Rating must be at most 5")
    private Integer rating;

    @Size(max = 200, message = "Title must not exceed 200 characters")
    private String title;

    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    private String content;

    @Size(max = 1000, message = "Pros must not exceed 1000 characters")
    private String pros;

    @Size(max = 1000, message = "Cons must not exceed 1000 characters")
    private String cons;

    private List<String> images; // Hình ảnh đính kèm (mảng URL)
  }

  // ─── Request: Admin phản hồi đánh giá ─────────────────────────────────────
  @Data
  public static class AdminReplyRequest {
    @NotNull(message = "Reply content is required")
    @Size(max = 2000, message = "Reply must not exceed 2000 characters")
    private String replyContent;
  }

  // ─── Request: Admin cập nhật trạng thái duyệt ──────────────────────────────
  @Data
  public static class AdminUpdateStatusRequest {
    @NotNull(message = "Status is required")
    private Boolean isApproved;
  }

  // ─── Response: Chi tiết 1 đánh giá ───────────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Integer id;
    private Integer productId;
    private String productName;
    private Integer variantId;
    private String variantName;
    private Integer orderId;
    private Integer rating;
    private String title;
    private String content;
    private String pros;
    private String cons;
    private Boolean isVerifiedPurchase;
    private Boolean isApproved;
    private Integer helpfulCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String replyContent;
    private LocalDateTime repliedAt;
    private ReviewUserDto user;
    private List<String> images;
  }

  // ─── Response: Thống kê đánh giá sản phẩm ───────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReviewSummary {
    private Integer productId;
    private Double averageRating;
    private Integer totalReviews;
    private Map<Integer, Integer> ratingDistribution; // key: 1-5 star, value: count
  }

  // ─── DTO: Thông tin user trong đánh giá ──────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReviewUserDto {
    private Long id;
    private String username;
    private String name;
  }

  // ─── Response: Kiểm tra quyền đánh giá ───────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class EligibilityResponse {
    private Integer productId;
    private Boolean canReview;
    private Boolean alreadyReviewed;
    private Integer existingReviewId;
    private Boolean isVerifiedEligible;
    private Integer eligibleOrderId;
    private Integer eligibleVariantId;
    private String reason;
  }

  // ─── Response: Sản phẩm chờ đánh giá (từ đơn đã giao) ────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReviewableItem {
    private Integer orderId;
    private Integer productId;
    private String productName;
    private Integer variantId;
    private String variantName;
    private String imageUrl;
    private LocalDateTime deliveredAt;
  }

  // ─── Response: Thống kê admin ────────────────────────────────────────────
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminStatsResponse {
    private Long total;
    private Long pending;
    private Long approved;
    private Long hidden;
    private Long verifiedCount;
    private Long unrepliedCount;
    private Double averageRating;
    private Map<Integer, Integer> ratingDistribution;
  }

  // ─── Request: Duyệt/ẩn hàng loạt ───────────────────────────────────────
  @Data
  public static class BulkStatusRequest {
    @NotNull(message = "Danh sách ID không được trống")
    private java.util.List<Integer> ids;

    @NotNull(message = "Trạng thái duyệt không được trống")
    private Boolean isApproved;
  }
}

