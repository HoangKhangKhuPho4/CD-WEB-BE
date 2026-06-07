package com.cdweb.be.dto;

import com.cdweb.be.entity.ProductItem.ProductItemCondition;
import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTOs cho API Quản lý IMEI / Serial — đủ scenario kiểm thử. */
public class ImeiDto {

  private ImeiDto() {}

  // ── Stats ───────────────────────────────────────────────────────────────

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StatsResponse {
    private long total;
    private long available;
    private long reserved;
    private long sold;
    private long inRepair;
    private long defective;
    private long returned;
    private long linkedToOrders;
  }

  // ── List item ───────────────────────────────────────────────────────────

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ListItem {
    private Integer id;
    private String imei;
    private String serialNumber;
    private String imei2;
    private String productName;
    private String variantName;
    private String skuCode;
    private Integer variantId;
    private ProductItemStatus status;
    private ProductItemCondition condition;
    private String orderCode;
    private Integer orderId;
    private String batchNumber;
    private String location;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate warrantyStartDate;

    private Integer warrantyMonths;
  }

  // ── Detail ────────────────────────────────────────────────────────────────

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DetailResponse {
    private Integer id;
    private String imei;
    private String serialNumber;
    private String imei2;
    private String macAddress;
    private String batchNumber;
    private String location;
    private String notes;
    private ProductItemStatus status;
    private ProductItemCondition condition;
    private Integer variantId;
    private String variantName;
    private String skuCode;
    private String productName;
    private Integer productId;
    private OrderLink order;
    private WarrantyInfo warranty;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate manufactureDate;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime soldAt;

    private List<TransactionItem> transactions;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class OrderLink {
    private Integer orderId;
    private String orderCode;
    private String orderStatus;
    private Integer orderDetailId;
    private Integer quantity;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class WarrantyInfo {
    private LocalDate startDate;
    private Integer months;
    private boolean active;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TransactionItem {
    private Integer id;
    private String transactionType;
    private Integer quantity;
    private String reason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
  }

  // ── Create / Update ─────────────────────────────────────────────────────

  @Data
  public static class CreateRequest {
    @NotNull(message = "Variant ID không được để trống")
    private Integer variantId;

    @NotEmpty(message = "Danh sách IMEI không được để trống")
    private List<String> imeis;

    private String batchNumber;
    private String note;
    private String imei2;
    private String macAddress;
  }

  @Data
  public static class UpdateRequest {
    private String imei2;
    private String macAddress;
    private String batchNumber;
    private String location;
    private String notes;
    private ProductItemCondition condition;
  }

  @Data
  public static class StatusUpdateRequest {
    @NotNull(message = "Trạng thái không được để trống")
    private ProductItemStatus status;

    private String reason;
    /** Bỏ qua kiểm tra chuyển trạng thái (chỉ ROLE_ADMIN). */
    private Boolean force;
  }

  @Data
  public static class BulkStatusRequest {
    @NotEmpty(message = "Danh sách ID không được để trống")
    private List<Integer> ids;

    @NotNull(message = "Trạng thái không được để trống")
    private ProductItemStatus status;

    private String reason;
    private Boolean force;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BulkStatusResult {
    private int successCount;
    private int failCount;
    private List<String> errors;
  }

  // ── Validate ────────────────────────────────────────────────────────────

  @Data
  public static class ValidateRequest {
    private Integer variantId;
    @NotEmpty(message = "Danh sách IMEI không được để trống")
    private List<String> imeis;
    private Integer excludeId;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateItemResult {
    private String imei;
    private boolean valid;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateResponse {
    private boolean allValid;
    private List<ValidateItemResult> results;
  }

  // ── Release ─────────────────────────────────────────────────────────────

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReleaseResponse {
    private Integer productItemId;
    private String imei;
    private ProductItemStatus previousStatus;
    private ProductItemStatus newStatus;
    private String orderCode;
    private String message;
  }

  // ── Return ──────────────────────────────────────────────────────────────

  @Data
  public static class ReturnRequest {
    @NotBlank(message = "Mã IMEI/Serial không được để trống")
    private String imei;

    private String reason;
    private Boolean isDefective = false;
  }

  // ── Import result ───────────────────────────────────────────────────────

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ImportResult {
    private int importedCount;
    private int skippedCount;
    private List<String> errors;
  }
}
