package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Column(name = "email", nullable = false, unique = true)
  private String email;

  @Column(name = "password", nullable = false)
  private String password;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "phone")
  private String phone;

  @Column(name = "birth")
  private LocalDate birth;

  @Column(name = "gender")
  private String gender;

  @Column(name = "oauth_provider", length = 50)
  private String oauthProvider;

  @Column(name = "oauth_uid", columnDefinition = "TEXT")
  private String oauthUid;

  @Column(name = "oauth_token", columnDefinition = "TEXT")
  private String oauthToken;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @Column(name = "last_login_at")
  private LocalDateTime lastLoginAt;

  @Column(name = "status")
  private Integer status = 1; // 1 = active, 0 = inactive

  @EqualsAndHashCode.Exclude // Ngăn hashCode() kích hoạt load vòng lặp vô hạn
  @ToString.Exclude // Ngăn toString() kích hoạt load collection
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "user_roles",
      joinColumns = @JoinColumn(name = "user_id"),
      inverseJoinColumns = @JoinColumn(name = "role_id"))
  private Set<Role> roles;

  // Custom methods for compatibility
  public void setFullName(String fullName) {
    this.name = fullName;
  }

  public String getFullName() {
    return this.name;
  }

  public void setIsActive(boolean isActive) {
    this.status = isActive ? 1 : 0;
  }

  public boolean getIsActive() {
    return this.status != null && this.status == 1;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (status == null) {
      status = 1;
    }
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
