package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "purchase_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrder {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "po_number", nullable = false, unique = true, length = 50)
  private String poNumber;

  @Enumerated(EnumType.STRING)
  @Column(name = "status")
  private PurchaseOrderStatus status = PurchaseOrderStatus.DRAFT;

  @Column(name = "total_amount", precision = 15, scale = 2)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "order_date")
  private LocalDateTime orderDate;

  @Column(name = "expected_date")
  private LocalDate expectedDate;

  @Column(name = "received_date")
  private LocalDateTime receivedDate;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id", nullable = false)
  private Supplier supplier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User createdByUser;

  @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<PurchaseOrderItem> items;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    orderDate = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum PurchaseOrderStatus {
    DRAFT,
    PENDING,
    APPROVED,
    RECEIVING,
    COMPLETED,
    CANCELLED
  }
}
