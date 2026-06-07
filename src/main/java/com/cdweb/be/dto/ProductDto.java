package com.cdweb.be.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProductDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotBlank(message = "Product name is required")
    private String name;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Double price;

    @PositiveOrZero(message = "Quantity must be zero or positive")
    private Integer quantity;

    private String status;

    @NotBlank(message = "Detail is required")
    private String detail;

    private LocalDate importDate;

    @NotNull(message = "Product type ID is required")
    private Integer productTypeId;

    @NotNull(message = "Producer ID is required")
    private Integer producerId;

    @NotNull(message = "Coupon ID is required")
    private Integer couponId;

    private Boolean isFeatured;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String name;

    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    private Double price;

    @PositiveOrZero(message = "Quantity must be zero or positive")
    private Integer quantity;

    private String status;
    private String detail;
    private LocalDate importDate;
    private Integer productTypeId;
    private Integer producerId;
    private Integer couponId;
    private Boolean isFeatured;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Integer id;
    private String name;
    private Double price;
    private Integer quantity;
    private String status;
    private String detail;
    private LocalDate importDate;
    private Integer active;
    private Boolean isFeatured;
    private ProductTypeDto productType;
    private ProducerDto producer;
    private CouponDto coupon;
    private Double averageRating;
    private Integer reviewCount;
    private Integer soldQuantity; // Số lượng đã bán (mới bổ sung cho User)
    private java.util.List<ImageDto> images;
    private java.util.List<VariantDto> variants;
    private java.util.List<ProductOptionDto> options;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductOptionDto {
    private String name;
    private java.util.List<String> values;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ImageDto {
    private Integer id;
    private String linkImage;
    private Integer variantId;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductTypeDto {
    private Integer id;
    private String name;
    private String code;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProducerDto {
    private Integer id;
    private String name;
    private String code;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CouponDto {
    private Integer id;
    private String code;
    private Integer percentDiscount;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SearchRequest {
    private String keyword;
    private Integer productTypeId;
    private Double minPrice;
    private Double maxPrice;
    private Integer producerId;
    private Double minRating; // Added for rating filter
    private String color; // Added for color filter
    private String sortBy = "name"; // name, price, importDate
    private String sortDir = "asc"; // asc, desc
    private int page = 0;
    private int size = 10;
  }

  /** Gợi ý tìm kiếm nhanh (header autocomplete). */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SuggestResponse {
    private Integer id;
    private String name;
    private Double price;
    private String imageUrl;
    private Integer categoryId;
    private String categoryName;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class VariantDto {
    private Integer id;
    private Integer productId;
    private String productName;
    private String skuCode;
    private String variantName;
    private Double price;
    private Double originalPrice;
    private Integer stockQuantity;
    private Boolean isActive;
    private Boolean isDefault;
    private java.util.List<AttributeValueResponse> attributeValues;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AttributeValueResponse {
    private Integer id;
    private String attributeName;
    private String value;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminProductListResponse {
    private Integer id;
    private String name;
    private String imageUrl;
    private Double basePrice;
    private Integer totalQuantity;
    private String status;
    private Boolean isFeatured;
    private ProductTypeDto productType;
    private ProducerDto producer;
    private LocalDate createdAt;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminProductResponse {
    private Integer id;
    private String name;
    private Double basePrice;
    private String description;
    private String status;
    private Boolean isFeatured;
    private ProductTypeDto productType;
    private ProducerDto producer;
    private CouponDto coupon;
    private LocalDate createdAt;
    private java.util.List<VariantDto> variants;
    private java.util.List<ImageDto> images;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminCreateRequest {
    @NotBlank(message = "Tên sản phẩm không được trống")
    private String name;

    @NotNull(message = "Giá không được trống")
    @DecimalMin(value = "0.0", inclusive = false, message = "Giá sản phẩm phải lớn hơn 0")
    private Double price;

    @PositiveOrZero(message = "Số lượng không được nhỏ hơn 0")
    private Integer quantity;

    private String detail;
    private String status;

    @NotNull(message = "Danh mục không được trống")
    private Integer productTypeId;

    @NotNull(message = "Nhà sản xuất không được trống")
    private Integer producerId;

    private Integer couponId;
    private Boolean isFeatured;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminUpdateRequest {
    private String name;

    @PositiveOrZero(message = "Giá không được nhỏ hơn 0")
    private Double price;

    @PositiveOrZero(message = "Số lượng không được nhỏ hơn 0")
    private Integer quantity;

    private String detail;
    private String status;
    private Integer productTypeId;
    private Integer producerId;
    private Integer couponId;
    private Boolean isFeatured;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminVariantRequest {
    private String skuCode;
    private String variantName;

    @PositiveOrZero(message = "Giá không được nhỏ hơn 0")
    private Double price;

    private Double originalPrice;

    @PositiveOrZero(message = "Số lượng kho không được nhỏ hơn 0")
    private Integer stockQuantity;

    private Boolean isDefault;
    private Boolean isActive;
    private java.util.List<Integer> attributeValueIds;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminImageRequest {
    @NotBlank(message = "Image link cannot be empty")
    private String linkImage;

    private Integer variantId;
    private Boolean isDefault;
  }

  // ── Extended admin DTOs (full test coverage) ─────────────────────────────

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminStatsResponse {
    private long totalProducts;
    private long totalActive;
    private long totalInactive;
    private long totalFeatured;
    private long lowStockVariants;
    private long totalVariants;
  }

  @Data
  public static class ValidateSkuRequest {
    @NotBlank(message = "SKU không được trống")
    private String skuCode;
    private Integer excludeVariantId;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateSkuResponse {
    private String skuCode;
    private boolean available;
    private String message;
    private Integer existingVariantId;
    private Integer existingProductId;
  }

  @Data
  public static class BulkStatusRequest {
    @jakarta.validation.constraints.NotEmpty(message = "Danh sách ID không được trống")
    private java.util.List<Integer> ids;

    @NotNull(message = "Trạng thái isActive không được null")
    private Boolean isActive;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BulkStatusResult {
    private int successCount;
    private int failCount;
    private java.util.List<String> errors;
  }

  @Data
  public static class FeaturedRequest {
    @NotNull(message = "isFeatured không được null")
    private Boolean isFeatured;
  }

  @Data
  public static class VariantStockRequest {
    @NotNull(message = "Số lượng không được null")
    @PositiveOrZero(message = "Số lượng không được âm")
    private Integer stockQuantity;
  }
}
