package com.cdweb.be.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class InventoryAuditDto {

  private InventoryAuditDto() {}

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SheetResponse {
    private Integer id;
    private String code;
    private String createdAt;
    private Integer productTypeId;
    private String categoryName;
    private int scanned;
    private int expected;
    private int matched;
    private int missing;
    private int surplus;
    private int variance;
    /** in_progress | reconciled | pending_approval | approved | rejected */
    private String status;
    private String note;
    private String rejectReason;
    private boolean retailLocked;
    private Integer wizardStep;
    private List<String> scannedCodes;
    private List<String> missingCodes;
    private List<String> surplusCodes;
    private List<ReconciliationLine> lines;
    private List<DiscrepancyDetail> discrepancies;
    private String createdByName;
    private String approvedByName;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ReconciliationLine {
    private Integer variantId;
    private String productName;
    private String variantName;
    private String skuCode;
    private int systemQty;
    private int actualQty;
    private int variance;
    /** MATCHED | SHORTAGE | SURPLUS */
    private String status;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DiscrepancyDetail {
    private String serial;
    /** MISSING | SURPLUS | MISPLACED */
    private String type;
    private String productName;
    private String skuCode;
    private String expectedLocation;
    private String scannedLocation;
    private String message;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StartRequest {
    @NotNull(message = "Danh mục kiểm kê không được để trống")
    private Integer productTypeId;

    private String note;
    private Boolean retailLocked;
  }

  /** @deprecated dùng start + scan */
  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CreateRequest {
    @NotEmpty(message = "Danh sách mã quét không được rỗng")
    private List<String> scannedCodes;

    private String note;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScanRequest {
    @NotEmpty(message = "Mã quét không được để trống")
    private String code;

    private String shelfLocation;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BulkScanRequest {
    @NotEmpty(message = "Danh sách mã không được rỗng")
    private List<String> codes;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScanResponse {
    private String code;
    /** MATCHED | SURPLUS | DUPLICATE | MISPLACED */
    private String resultType;
    private String message;
    private String productName;
    private String skuCode;
    private String expectedLocation;
    private String scannedLocation;
    private int totalScanned;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class BulkScanResponse {
    private int total;
    private int matched;
    private int surplus;
    private int duplicate;
    private int misplacement;
    private List<ScanResponse> results;
    private int totalScanned;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CompleteResponse {
    private SheetResponse sheet;
    private String summary;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SubmitRequest {
    private String note;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RejectRequest {
    private String reason;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateNoteRequest {
    private String note;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class StatsResponse {
    private long inProgressCount;
    private long pendingApprovalCount;
    private long approvedCount;
    private long rejectedCount;
    /** legacy */
    private long draftCount;
    private long submittedCount;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScanProgressResponse {
    private int totalScanned;
    private int expectedCount;
    private boolean hideSystemQty;
    private List<ScanProgressLine> lines;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ScanProgressLine {
    private Integer variantId;
    private String productName;
    private String variantName;
    private String skuCode;
    private int actualQty;
  }
}
