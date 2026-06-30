package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "return_inspection_sheets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReturnInspectionSheet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "sheet_code", nullable = false, unique = true, length = 50)
  private String sheetCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 20)
  private SheetStatus status = SheetStatus.PENDING;

  @Column(name = "order_id")
  private Integer orderId;

  @Column(name = "product_item_id")
  private Integer productItemId;

  @Column(name = "serial_code", length = 100)
  private String serialCode;

  @Column(name = "order_code", length = 50)
  private String orderCode;

  @Column(name = "customer_name", length = 200)
  private String customerName;

  @Column(name = "customer_phone", length = 30)
  private String customerPhone;

  @Column(name = "tracking_code", length = 100)
  private String trackingCode;

  @Column(name = "product_name", length = 300)
  private String productName;

  @Column(name = "variant_name", length = 200)
  private String variantName;

  @Column(name = "sku_code", length = 80)
  private String skuCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "judgment", length = 20)
  private ReturnJudgment judgment;

  @Enumerated(EnumType.STRING)
  @Column(name = "defect_cause", length = 30)
  private DefectCause defectCause;

  @Column(name = "detail_reason", columnDefinition = "TEXT")
  private String detailReason;

  @Column(name = "warehouse_note", columnDefinition = "TEXT")
  private String warehouseNote;

  @Column(name = "reject_reason", columnDefinition = "TEXT")
  private String rejectReason;

  @Column(name = "cancel_reason", columnDefinition = "TEXT")
  private String cancelReason;

  /** Ảnh minh chứng hàng lỗi (URL upload). */
  @Column(name = "evidence_url", length = 500)
  private String evidenceUrl;

  /** Serial quét khi lưu tạm — khôi phục form. */
  @Column(name = "draft_scanned_serial", length = 100)
  private String draftScannedSerial;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "processed_by")
  private User processedBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "processed_at")
  private LocalDateTime processedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum SheetStatus {
    PENDING,
    DRAFT,
    PROCESSED,
    REJECTED,
    CANCELLED
  }

  public enum ReturnJudgment {
    GOOD,
    DEFECTIVE
  }

  public enum DefectCause {
    SHIPPING,
    MANUFACTURER
  }
}
