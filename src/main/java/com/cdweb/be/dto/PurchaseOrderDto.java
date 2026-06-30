package com.cdweb.be.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class PurchaseOrderDto {

  private PurchaseOrderDto() {}

  /** Trạng thái hiển thị cho FE kho: pending | receiving | completed */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SummaryResponse {
    private Integer id;
    private String code;
    private String supplier;
    private Integer supplierId;
    private int items;
    private String expectedDate;
    /** Trạng thái gọn cho màn kho: pending | receiving | completed */
    private String status;
    /** Trạng thái DB: DRAFT | PENDING | APPROVED | ... */
    private String rawStatus;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String notes;
    private String rejectReason;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RejectRequest {
    private String rejectReason;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateLineRequest {
    private Integer variantId;
    private Integer quantityOrdered;
    private BigDecimal unitCost;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    private Integer supplierId;
    private String expectedDate;
    private String notes;
    private List<CreateLineRequest> lines;
    /** true = gửi duyệt (PENDING), false = lưu nháp (DRAFT) */
    private Boolean submitForApproval;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SupplierResponse {
    private Integer id;
    private String name;
    private String code;
    private String phone;
    private String email;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LineItem {
    private Integer id;
    private Integer variantId;
    private String skuCode;
    private String productName;
    private int quantityOrdered;
    private int quantityReceived;
    private BigDecimal unitCost;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DetailResponse {
    private Integer id;
    private String code;
    private String supplier;
    private int items;
    private String expectedDate;
    private String status;
    private String rawStatus;
    private BigDecimal totalAmount;
    private Integer totalQuantity;
    private String notes;
    private String rejectReason;
    private String orderDate;
    private String receivedDate;
    private List<LineItem> lineItems;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReceiveLineProgress {
    private Integer poLineId;
    private Integer variantId;
    private String skuCode;
    private String productName;
    private String variantName;
    private int quantityOrdered;
    private int quantityReceived;
    private int quantityDamaged;
    private int remaining;
    private List<String> receivedSerials;
    private List<String> damagedSerials;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReceiveProgress {
    private int totalOrdered;
    private int totalReceived;
    private int totalDamaged;
    private int totalRemaining;
    private boolean complete;
    private List<ReceiveLineProgress> lines;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StockLotSummary {
    private String lotNumber;
    private int itemsScanned;
    private int itemsRequired;
    /** OPEN | CLOSED */
    private String status;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReceiveDetailResponse {
    private Integer id;
    private String code;
    private String supplier;
    private String status;
    private String rawStatus;
    private String expectedDate;
    private String notes;
    /** Mặc định sinh LOT = mã PO */
    private String defaultBatchNumber;
    private boolean canStartReceiving;
    private boolean canScan;
    private boolean canComplete;
    private boolean canLockOrder;
    private ReceiveProgress progress;
    private List<StockLotSummary> stockLots;
    /** PO vừa tự động chuyển COMPLETED sau lần quét cuối */
    private boolean autoCompleted;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BulkReceiveSerialRequest {
    private Integer poLineId;
    private List<String> serials;
    private String batchNumber;
    private String shelfLocation;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BulkReceiveItemResult {
    private String serial;
    private boolean success;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BulkReceiveSerialResponse {
    private ReceiveDetailResponse detail;
    private List<BulkReceiveItemResult> results;
    private int successCount;
    private int failCount;
    private boolean autoCompleted;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateScanRequest {
    private Integer poLineId;
    private String scannedCode;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateScanResponse {
    private boolean valid;
    private String message;
    private String scannedCode;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReceiveSerialRequest {
    private Integer poLineId;
    private String scannedCode;
    private String batchNumber;
    private String shelfLocation;
    private String imei2;
    private String macAddress;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReceiveQuantityRequest {
    private Integer poLineId;
    private Integer quantity;
    private String batchNumber;
    private String shelfLocation;
    private String note;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReportDamagedRequest {
    private Integer poLineId;
    /** Serial thiết bị lỗi (nếu có) */
    private String serialCode;
    private Integer quantity;
    private String reason;
    private String evidenceUrl;
    private String shelfLocation;
    private String batchNumber;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CompleteReceivingRequest {
    /** Bắt buộc khi còn thiếu so với SL đặt */
    private String discrepancyNote;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CompleteReceivingResponse {
    private Integer id;
    private String code;
    private String status;
    private int totalOrdered;
    private int totalReceived;
    private int totalDamaged;
    private int totalMissing;
    private String message;
  }
}
