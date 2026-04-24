package com.cdweb.be.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "roles")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Role {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false, unique = true)
  private String name;

  @Column(name = "description")
  private String description;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  // Cắt vòng lặp SQL: Role → User → Role → ... (Lombok hashCode/toString gây ra)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @JsonIgnore
  @ManyToMany(mappedBy = "roles")
  private Set<User> users;

  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @ManyToMany(fetch = FetchType.EAGER)
  @JoinTable(
      name = "role_permissions",
      joinColumns = @JoinColumn(name = "role_id"),
      inverseJoinColumns = @JoinColumn(name = "permission_id"))
  private Set<Permission> permissions;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
