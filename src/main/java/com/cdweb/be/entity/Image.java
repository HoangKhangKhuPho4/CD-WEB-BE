package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Image {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
  private String imageUrl;

  @Column(name = "alt_text")
  private String altText;

  @Column(name = "is_primary")
  private Boolean isPrimary = false;

  @Column(name = "display_order")
  private Integer displayOrder = 0;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id")
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id")
  private ProductVariant variant;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
