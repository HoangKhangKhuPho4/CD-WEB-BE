package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "shipping_methods")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingMethod {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "code", nullable = false, unique = true, length = 50)
  private String code;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "logo_url", columnDefinition = "TEXT")
  private String logoUrl;

  @Column(name = "base_fee", precision = 15, scale = 2)
  private BigDecimal baseFee = BigDecimal.ZERO;

  @Column(name = "fee_per_kg", precision = 15, scale = 2)
  private BigDecimal feePerKg = BigDecimal.ZERO;

  @Column(name = "free_shipping_threshold", precision = 15, scale = 2)
  private BigDecimal freeShippingThreshold;

  @Column(name = "estimated_days_min")
  private Integer estimatedDaysMin = 1;

  @Column(name = "estimated_days_max")
  private Integer estimatedDaysMax = 3;

  @Column(name = "is_active")
  private Boolean isActive = true;

  @Column(name = "display_order")
  private Integer displayOrder = 0;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
