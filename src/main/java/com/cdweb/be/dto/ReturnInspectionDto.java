package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class ReturnInspectionDto {

  private ReturnInspectionDto() {}

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class IntakeRequest {
    /** Serial/IMEI hoặc mã vận đơn GHN / mã đơn */
    @NotBlank(message = "Nhập Serial/IMEI hoặc mã vận đơn")
    private String code;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class IntakeResponse {
    private Integer redirectSheetId;
    private int createdCount;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProcessRequest {
    /** Quét lại để đối chiếu — bắt buộc khớp serial trên phiếu */
    private String scannedSerial;
    /** GOOD | DEFECTIVE */
    private String judgment;
    /** SHIPPING | MANUFACTURER — bắt buộc khi DEFECTIVE */
    private String defectCause;
    private String detailReason;
    private String warehouseNote;
    /** Từ chối nhận — IMEI không khớp / gian lận */
    private Boolean rejectMismatch;
    private String rejectReason;
    /** Bắt buộc khi DEFECTIVE */
    private String evidenceUrl;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DraftRequest {
    private String scannedSerial;
    private String judgment;
    private String defectCause;
    private String detailReason;
    private String warehouseNote;
    private String evidenceUrl;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class CancelRequest {
    @NotBlank(message = "Vui lòng nhập lý do hủy phiếu")
    private String cancelReason;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DefectLabelResponse {
    private String sheetCode;
    private String serialCode;
    private String productName;
    private String variantName;
    private String skuCode;
    private String orderCode;
    private String defectCause;
    private String detailReason;
    private String processedAt;
    private String zoneLabel;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SheetSummary {
    private Integer id;
    private String sheetCode;
    private String status;
    private String serialCode;
    private String orderCode;
    private String customerName;
    private String customerPhone;
    private String productName;
    private String variantName;
    private String skuCode;
    private String trackingCode;
    private String judgment;
    private String defectCause;
    private String createdAt;
    private String processedAt;
    private boolean warehouseConfirmed;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SheetDetail {
    private Integer id;
    private String sheetCode;
    private String status;
    private String serialCode;
    private String orderCode;
    private Integer orderId;
    private String customerName;
    private String customerPhone;
    private String productName;
    private String variantName;
    private String skuCode;
    private String trackingCode;
    private String judgment;
    private String defectCause;
    private String detailReason;
    private String warehouseNote;
    private String rejectReason;
    private String cancelReason;
    private String evidenceUrl;
    private String draftScannedSerial;
    private String createdAt;
    private String processedAt;
    private boolean orderWarehouseConfirmed;
  }
}
