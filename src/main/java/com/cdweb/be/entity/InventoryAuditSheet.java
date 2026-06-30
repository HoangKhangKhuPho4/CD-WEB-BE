package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "inventory_audit_sheets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAuditSheet {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "sheet_code", nullable = false, unique = true, length = 50)
  private String sheetCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 30)
  private AuditStatus status = AuditStatus.IN_PROGRESS;

  @Column(name = "product_type_id")
  private Integer productTypeId;

  @Column(name = "category_name", length = 120)
  private String categoryName;

  @Column(name = "scanned_count", nullable = false)
  private Integer scannedCount = 0;

  @Column(name = "expected_count", nullable = false)
  private Integer expectedCount = 0;

  @Column(name = "matched_count", nullable = false)
  private Integer matchedCount = 0;

  @Column(name = "missing_count", nullable = false)
  private Integer missingCount = 0;

  @Column(name = "surplus_count", nullable = false)
  private Integer surplusCount = 0;

  /** legacy — giữ tương thích cột cũ */
  @Column(name = "variance", nullable = false)
  private Integer variance = 0;

  @Column(name = "retail_locked", nullable = false)
  private Boolean retailLocked = false;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  @Column(name = "reject_reason", columnDefinition = "TEXT")
  private String rejectReason;

  @Column(name = "reconciliation_json", columnDefinition = "TEXT")
  private String reconciliationJson;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "approved_by")
  private User approvedBy;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "reconciled_at")
  private LocalDateTime reconciledAt;

  @Column(name = "approved_at")
  private LocalDateTime approvedAt;

  @ElementCollection
  @CollectionTable(
      name = "inventory_audit_scans",
      joinColumns = @JoinColumn(name = "sheet_id"))
  @Column(name = "scan_code", length = 100)
  @Builder.Default
  private List<String> scannedCodes = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
      name = "inventory_audit_missing_codes",
      joinColumns = @JoinColumn(name = "sheet_id"))
  @Column(name = "serial_code", length = 100)
  @Builder.Default
  private List<String> missingCodes = new ArrayList<>();

  @ElementCollection
  @CollectionTable(
      name = "inventory_audit_surplus_codes",
      joinColumns = @JoinColumn(name = "sheet_id"))
  @Column(name = "serial_code", length = 100)
  @Builder.Default
  private List<String> surplusCodes = new ArrayList<>();

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = createdAt;
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum AuditStatus {
    /** @deprecated legacy */
    DRAFT,
    IN_PROGRESS,
    /** @deprecated legacy */
    SUBMITTED,
    RECONCILED,
    PENDING_APPROVAL,
    APPROVED,
    REJECTED
  }
}
