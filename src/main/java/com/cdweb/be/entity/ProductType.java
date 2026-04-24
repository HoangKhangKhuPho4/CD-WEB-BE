package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "product_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductType {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "icon_url", columnDefinition = "TEXT")
  private String iconUrl;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "display_order")
  private Integer displayOrder = 0;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // Self-referencing parent
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private ProductType parent;

  @OneToMany(mappedBy = "parent", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<ProductType> children;

  // Relationships
  @OneToMany(mappedBy = "productType", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Product> products;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
