package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "attribute_values")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AttributeValue {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "attribute_id", nullable = false)
  private Attribute attribute;

  @Column(name = "value", nullable = false, length = 100)
  private String value;

  @ManyToMany(mappedBy = "attributeValues")
  private List<ProductVariant> variants;
}
