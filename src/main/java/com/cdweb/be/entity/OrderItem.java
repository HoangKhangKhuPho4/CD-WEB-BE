package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Maps to order_item_serials table. Links an order_detail record to a specific physical product
 * item (Serial/IMEI).
 */
@Entity
@Table(
    name = "order_item_serials",
    uniqueConstraints =
        @UniqueConstraint(
            name = "order_item_unique",
            columnNames = {"order_detail_id", "product_item_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_detail_id", nullable = false)
  private OrderDetail orderDetail;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_item_id", nullable = false)
  private ProductItem productItem;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
