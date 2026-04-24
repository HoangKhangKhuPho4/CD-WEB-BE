package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Entity
@Table(name = "purchase_order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PurchaseOrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "quantity_ordered", nullable = false)
  private Integer quantityOrdered;

  @Column(name = "quantity_received")
  private Integer quantityReceived = 0;

  @Column(name = "unit_cost", nullable = false, precision = 15, scale = 2)
  private BigDecimal unitCost;

  @Column(name = "total_cost", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalCost;

  @Column(name = "notes", columnDefinition = "TEXT")
  private String notes;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;
}
