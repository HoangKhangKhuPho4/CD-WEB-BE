package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "product_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "serial_number", unique = true, length = 100)
  private String serialNumber;

  @Column(name = "imei", unique = true, length = 20)
  private String imei;

  @Column(name = "imei2", unique = true, length = 20)
  private String imei2;

  @Column(name = "mac_address", length = 20)
  private String macAddress;

  @Column(name = "batch_number", length = 50)
  private String batchNumber;

  @Column(name = "manufacture_date")
  private LocalDate manufactureDate;

  @Column(name = "warranty_start_date")
  private LocalDate warrantyStartDate;

  @Column(name = "warranty_months")
  private Integer warrantyMonths = 12;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private ProductItemStatus status = ProductItemStatus.AVAILABLE;

  @Enumerated(EnumType.STRING)
  @Column(name = "`condition`")
  private ProductItemCondition condition = ProductItemCondition.NEW;

  @Column(name = "location", length = 100)
  private String location;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "sold_at")
  private LocalDateTime soldAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id")
  private PurchaseOrder purchaseOrder;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Enums
  public enum ProductItemStatus {
    AVAILABLE,
    RESERVED,
    SOLD,
    DEFECTIVE,
    RETURNED,
    IN_REPAIR
  }

  public enum ProductItemCondition {
    NEW,
    LIKE_NEW,
    GOOD,
    FAIR,
    DAMAGED
  }
}
