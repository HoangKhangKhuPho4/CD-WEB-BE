package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Supplier {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "code", unique = true, length = 50)
  private String code;

  @Column(name = "contact_person", length = 100)
  private String contactPerson;

  @Column(name = "phone", length = 20)
  private String phone;

  @Column(name = "email", length = 100)
  private String email;

  @Column(name = "address", columnDefinition = "TEXT")
  private String address;

  @Column(name = "tax_code", length = 50)
  private String taxCode;

  @Column(name = "is_active")
  private Boolean isActive = true;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
