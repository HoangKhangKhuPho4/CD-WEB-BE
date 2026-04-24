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
@Table(name = "permissions")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Permission {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "code", nullable = false, unique = true)
  private String code;

  @Column(name = "description")
  private String description;

  @Column(name = "created_at")
  private LocalDateTime createdAt;

  // Cắt vòng lặp SQL: Permission → Role → Permission → ... (Lombok hashCode/toString gây ra)
  @EqualsAndHashCode.Exclude
  @ToString.Exclude
  @JsonIgnore
  @ManyToMany(mappedBy = "permissions")
  private Set<Role> roles;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }
}
