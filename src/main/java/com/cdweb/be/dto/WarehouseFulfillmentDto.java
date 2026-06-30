package com.cdweb.be.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class WarehouseFulfillmentDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class QueueItem {
    private Integer id;
    private String orderCode;
    private String status;
    private String customerName;
    private String customerPhone;
    private BigDecimal total;
    private String paymentMethod;
    private LocalDateTime orderDate;
    private Integer totalSerialRequired;
    private Integer totalSerialAssigned;
    private boolean pickingComplete;
    private Long pickedByUserId;
    private String pickedByName;
    private LocalDateTime pickedAt;
    private boolean canStartPicking;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FifoSerialHint {
    private Integer productItemId;
    private String serialNumber;
    private String imei;
    private String location;
    private String batchNumber;
    private LocalDateTime stockInDate;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PickingLine {
    private Integer orderDetailId;
    private String productName;
    private String variantName;
    private String skuCode;
    private Integer quantity;
    private Integer assignedCount;
    private List<String> assignedSerials;
    private FifoSerialHint nextFifoHint;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PickingProgress {
    private Integer totalRequired;
    private Integer totalAssigned;
    private boolean complete;
    private List<PickingLine> lines;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FulfillmentDetail {
    private Integer id;
    private String orderCode;
    private String status;
    private String customerName;
    private String customerPhone;
    private String shippingAddress;
    private BigDecimal total;
    private String paymentMethod;
    private String trackingCode;
    private String ghnOrderCode;
    private LocalDateTime orderDate;
    private Long pickedByUserId;
    private String pickedByName;
    private LocalDateTime pickedAt;
    private boolean canStartPicking;
    private boolean canScan;
    private boolean canDispatch;
    private PickingProgress progress;
    private List<OrderManagementDto.TimelineItem> timeline;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateScanRequest {
    private Integer orderDetailId;
    private String scannedCode;
    private Boolean overrideFifo;
    private String overrideReason;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateScanResponse {
    private boolean valid;
    private String message;
    private String expectedSerial;
    private String scannedSerial;
    private FifoSerialHint matchedItem;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AssignSerialRequest {
    private Integer orderDetailId;
    private String scannedCode;
    private Boolean overrideFifo;
    private String overrideReason;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class DispatchResponse {
    private Integer orderId;
    private String orderCode;
    private String status;
    private String trackingCode;
    private String ghnOrderCode;
    private String printUrl;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class FifoSerialsResponse {
    private Integer variantId;
    private List<FifoSerialHint> suggestedSerials;
  }
}
