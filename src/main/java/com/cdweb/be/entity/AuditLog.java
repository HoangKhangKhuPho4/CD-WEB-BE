package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "audit_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditLog {
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  private String action;
  private String entityName;
  private String entityId;
  private String performedBy;
  private String details;
  private LocalDateTime timestamp;

  @PrePersist
  protected void onCreate() {
    timestamp = LocalDateTime.now();
  }
}
