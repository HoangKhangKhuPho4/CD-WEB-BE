package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Product {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "slug", unique = true)
  private String slug;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "short_description", length = 500)
  private String shortDescription;

  @Column(name = "base_price", precision = 15, scale = 2)
  private BigDecimal basePrice;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "is_featured")
  private Boolean isFeatured = false;

  @Column(name = "meta_title")
  private String metaTitle;

  @Column(name = "meta_description", columnDefinition = "TEXT")
  private String metaDescription;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_type_id", nullable = false)
  private ProductType productType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "producer_id", nullable = false)
  private Producer producer;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ProductVariant> variants;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Image> images;

  @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ProductSpecification> productSpecifications;

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
