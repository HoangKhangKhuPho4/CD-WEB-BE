package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "coupons")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Coupon {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "code", nullable = false, unique = true, length = 50)
  private String code;

  @Column(name = "name", length = 100)
  private String name;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Enumerated(EnumType.STRING)
  @Column(name = "discount_type")
  private DiscountType discountType = DiscountType.PERCENT;

  @Column(name = "discount_value", nullable = false, precision = 10, scale = 2)
  private BigDecimal discountValue;

  @Column(name = "min_order_value", precision = 15, scale = 2)
  private BigDecimal minOrderValue;

  @Column(name = "max_discount_amount", precision = 15, scale = 2)
  private BigDecimal maxDiscountAmount;

  @Column(name = "usage_limit")
  private Integer usageLimit;

  @Column(name = "used_count")
  private Integer usedCount = 0;

  @Column(name = "date_start", nullable = false)
  private LocalDateTime dateStart;

  @Column(name = "date_end", nullable = false)
  private LocalDateTime dateEnd;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "updated_by")
  private User updatedBy;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public enum DiscountType {
    PERCENT,
    FIXED
  }
}
