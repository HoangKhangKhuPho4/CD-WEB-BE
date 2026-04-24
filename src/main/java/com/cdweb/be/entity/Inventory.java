package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "inventory_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Inventory {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Enumerated(EnumType.STRING)
  @Column(name = "transaction_type", nullable = false)
  private TransactionType transactionType;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "reference_type", length = 50)
  private String referenceType;

  @Column(name = "reference_id")
  private Integer referenceId;

  @Column(name = "reason", columnDefinition = "TEXT")
  private String reason;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_item_id")
  private ProductItem productItem;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  public enum TransactionType {
    IMPORT,
    EXPORT,
    ADJUSTMENT,
    RETURN,
    TRANSFER
  }
}
