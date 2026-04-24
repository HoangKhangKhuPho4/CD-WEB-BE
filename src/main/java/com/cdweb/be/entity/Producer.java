package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "producers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Producer {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false, length = 100)
  private String name;

  @Column(name = "code", unique = true, length = 10)
  private String code;

  @Column(name = "logo_url", columnDefinition = "TEXT")
  private String logoUrl;

  @Column(name = "description", columnDefinition = "TEXT")
  private String description;

  @Column(name = "country", length = 100)
  private String country;

  @Column(name = "website")
  private String website;

  @Column(name = "is_active", nullable = false)
  private Boolean isActive = true;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  // Relationships
  @OneToMany(mappedBy = "producer", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<Product> products;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
