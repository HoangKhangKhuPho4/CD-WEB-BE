package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "qr_auth_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class QrAuthToken {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, unique = true)
  private String token;

  @Column(nullable = false)
  private String status;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @Column(nullable = false)
  private LocalDateTime expiryDate;

  @PrePersist
  public void prePersist() {
    this.token = UUID.randomUUID().toString();
    this.status = "PENDING";
    this.expiryDate = LocalDateTime.now().plusMinutes(2); // sống trong 2 phút
  }
}
