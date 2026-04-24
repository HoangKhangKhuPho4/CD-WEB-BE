package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_interactions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteraction {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "user_id", nullable = false)
  private Integer userId;

  @Column(name = "product_id", nullable = false)
  private Integer productId;

  @Column(name = "action_type", nullable = false, length = 50)
  private String actionType;

  @Column(name = "rating", precision = 2, scale = 1)
  private BigDecimal rating;

  @Column(name = "interaction_score", nullable = false, precision = 3, scale = 1)
  private BigDecimal interactionScore;

  @Column(
      name = "created_at",
      columnDefinition = "DATETIME DEFAULT CURRENT_TIMESTAMP",
      insertable = false,
      updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = LocalDateTime.now();
    }
  }
}
