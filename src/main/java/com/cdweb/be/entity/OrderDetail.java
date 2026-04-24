package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "order_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDetail {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "product_name", nullable = false)
  private String productName;

  @Column(name = "variant_name")
  private String variantName;

  @Column(name = "sku_code", length = 100)
  private String skuCode;

  @Column(name = "quantity", nullable = false)
  private Integer quantity;

  @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
  private BigDecimal unitPrice;

  @Column(name = "discount_amount", precision = 15, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(name = "total_price", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalPrice;

  @Column(name = "warranty_months")
  private Integer warrantyMonths = 12;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id", nullable = false)
  private ProductVariant variant;

  @OneToMany(mappedBy = "orderDetail", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private java.util.List<OrderItem> orderItemSerials;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
