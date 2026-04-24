package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(
    name = "wishlists",
    uniqueConstraints =
        @UniqueConstraint(
            name = "wishlist_user_product_unique",
            columnNames = {"user_id", "product_id", "variant_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Wishlist {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "variant_id")
  private ProductVariant variant;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
