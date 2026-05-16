package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(unique = true, nullable = false)
  private String email;

  @Column(unique = true)
  private String username;

  private String password;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  private String phone;

  private LocalDate birth;

  private String gender;

  @Column(name = "avatar_url")
  private String avatarUrl;

  // ==========================================
  // OAUTH2 & SOCIAL LOGIN
  // ==========================================
  @Column(name = "provider")
  private String provider;

  @Column(name = "oauth_uid", columnDefinition = "TEXT")
  private String oauthUid;

  @Column(name = "oauth_token", columnDefinition = "TEXT")
  private String oauthToken;

  // ==========================================
  // STATUS & ROLES (Dùng cho Spring Security)
  // ==========================================
  @Builder.Default
  @Column(name = "enabled")
  private Boolean enabled = true;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
          name = "user_roles",
          joinColumns = @JoinColumn(name = "user_id"),
          inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles;

  // ==========================================
  // AUDIT (TRACKING THỜI GIAN)
  // ==========================================
  @Column(name = "created_at", updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  // ==========================================
  // LIFECYCLE HOOKS
  // ==========================================
  @PrePersist
  protected void onCreate() {
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
    if (this.enabled == null) {
      this.enabled = true;
    }
    if (this.provider == null) {
      this.provider = "LOCAL";
    }
  }

  @PreUpdate
  protected void onUpdate() {
    this.updatedAt = LocalDateTime.now();
  }
}