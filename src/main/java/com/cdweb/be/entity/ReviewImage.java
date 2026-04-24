package com.cdweb.be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "review_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReviewImage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "image_url", columnDefinition = "TEXT", nullable = false)
  private String imageUrl;

  @Column(name = "display_order")
  private Integer displayOrder = 0;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "review_id", nullable = false)
  private Review review;
}
