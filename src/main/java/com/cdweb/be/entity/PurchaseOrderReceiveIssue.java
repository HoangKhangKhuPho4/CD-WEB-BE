package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "purchase_order_receive_issues")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderReceiveIssue {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(name = "issue_type", nullable = false)
  private IssueType issueType = IssueType.DAMAGED;

  @Column(name = "serial_code", length = 100)
  private String serialCode;

  @Column(name = "quantity")
  private Integer quantity = 1;

  @Column(name = "reason", columnDefinition = "TEXT")
  private String reason;

  @Column(name = "evidence_url", length = 500)
  private String evidenceUrl;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "po_line_id")
  private PurchaseOrderItem poLine;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User createdBy;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  public enum IssueType {
    DAMAGED,
    DISCREPANCY
  }
}
