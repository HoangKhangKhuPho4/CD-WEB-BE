package com.cdweb.be.dto;

import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class WarrantyDto {

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
    private String imeiOrSerial;
    private String customerName;
    private String customerPhone;
    private String issueDescription;
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
