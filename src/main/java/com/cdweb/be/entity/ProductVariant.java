package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "product_variants")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariant {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "sku_code", nullable = false, unique = true, length = 100)
  private String skuCode;

  @Column(name = "variant_name")
  private String variantName;

  @Column(name = "price", nullable = false, precision = 15, scale = 2)
  private BigDecimal price;

  @Column(name = "original_price", precision = 15, scale = 2)
  private BigDecimal originalPrice;

  @Column(name = "cost_price", precision = 15, scale = 2)
  private BigDecimal costPrice;

  @Column(name = "weight", precision = 10, scale = 2)
  private BigDecimal weight;

  @Column(name = "dimensions", length = 100)
  private String dimensions;

  @Column(name = "barcode", unique = true, length = 100)
  private String barcode;

  @Column(name = "is_default")
  private Boolean isDefault = false;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "stock_quantity", nullable = false)
  private Integer stockQuantity = 0;

  @Column(name = "low_stock_threshold")
  private Integer lowStockThreshold = 10;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ProductItem> productItems;

  @OneToMany(mappedBy = "variant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Image> images;

  @ManyToMany(fetch = FetchType.LAZY)
  @JoinTable(
      name = "variant_attribute_values",
      joinColumns = @JoinColumn(name = "variant_id"),
      inverseJoinColumns = @JoinColumn(name = "attribute_value_id"))
  private List<AttributeValue> attributeValues;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
