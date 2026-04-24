package com.cdweb.be.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "specifications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Specification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "category_name", length = 100)
  private String categoryName;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "data_type")
  private DataType dataType = DataType.TEXT;

  @Column(name = "unit", length = 20)
  private String unit;

  @Column(name = "is_filterable")
  private Boolean isFilterable = false;

  @Column(name = "is_comparable")
  private Boolean isComparable = false;

  @Column(name = "display_order")
  private Integer displayOrder = 0;

  public enum DataType {
    TEXT,
    NUMBER,
    BOOLEAN,
    SELECT
  }
}
