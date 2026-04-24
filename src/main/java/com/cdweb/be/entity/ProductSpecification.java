package com.cdweb.be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
    name = "product_specifications",
    uniqueConstraints =
        @UniqueConstraint(
            name = "product_spec_unique",
            columnNames = {"product_id", "specification_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductSpecification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "value", nullable = false, length = 500)
  private String value;

  @Column(name = "display_order")
  private Integer displayOrder = 0;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "specification_id", nullable = false)
  private Specification specification;
}
