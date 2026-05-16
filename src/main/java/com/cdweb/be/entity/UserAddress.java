package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "user_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserAddress {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "receiver_name", nullable = false)
  private String receiverName;

  @Column(name = "phone", nullable = false, length = 20)
  private String phone;

  @Column(name = "province", length = 100)
  private String province;

  @Column(name = "district", length = 100)
  private String district;

  @Column(name = "ward", length = 100)
  private String ward;

  @Column(name = "address_detail", nullable = false, length = 500)
  private String addressDetail;

  @Column(name = "is_default")
  private Boolean isDefault = false;

  @Column(name = "label", length = 50)
  private String label;

  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
