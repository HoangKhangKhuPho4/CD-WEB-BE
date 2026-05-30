package com.cdweb.be.dto;

import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class WarrantyDto {

  /** Tra cứu công khai: bảo hành + lịch sử mua + phiếu sửa chữa. */
  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class LookupResponse {
    private Response warranty;
    private PurchaseInfo purchase;
    private List<TicketPublicSummary> repairTickets;
    private boolean found;
    private String message;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class PurchaseInfo {
    private String orderCode;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime orderDate;
    private String orderStatus;
    private String orderStatusDisplay;
    private String paymentMethod;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime deliveredAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime soldAt;
    private java.math.BigDecimal lineTotal;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TicketPublicSummary {
    private String ticketCode;
    private String status;
    private String statusDisplay;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime receivedAt;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime resolvedAt;
    private String issueDescription;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private String productName;
    private String variantName;
    private String imageUrl;
    private String imei;
    private String serialNumber;
    private ProductItemStatus status;
    private LocalDate warrantyStartDate;
    private LocalDate warrantyEndDate;
    private Integer warrantyMonths;
    private boolean isValid;
    private String message;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TicketRequest {
    @NotBlank private String imeiOrSerial;
    @NotBlank private String customerName;
    @NotBlank private String customerPhone;
    @NotBlank private String issueDescription;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TicketUpdateAdminRequest {
    private String status; // PENDING, IN_PROGRESS, COMPLETED, CANCELLED, RETURNED
    private String technicianNote;
    private java.math.BigDecimal repairCost;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class TicketResponse {
    private Integer id;
    private String ticketCode;
    private String imei;
    private String serialNumber;
    private String productName;
    private String variantName;
    private String customerName;
    private String customerPhone;
    private String issueDescription;
    private String technicianNote;
    private String status; // Ticket status
    private String statusDisplay; // Vietsub for status
    private java.math.BigDecimal repairCost;
    private java.time.LocalDateTime receivedAt;
    private java.time.LocalDateTime resolvedAt;
    private java.time.LocalDateTime returnedAt;
    private String createdBy;
  }
}
