package com.cdweb.be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "cms_contents")
@Getter
@Setter
public class CmsContent {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Enumerated(EnumType.STRING)
  @Column(name = "content_type", nullable = false, length = 20)
  private CmsContentType contentType;

  @Column(nullable = false, length = 500)
  private String title;

  @Column(length = 255)
  private String subtitle;

  @Column(name = "link_url", length = 1000)
  private String linkUrl;

  @Column(name = "image_url", length = 1000)
  private String imageUrl;

  @Column(columnDefinition = "TEXT")
  private String body;

  @Column(length = 120)
  private String author;

  @Column(name = "is_active")
  private Boolean active = true;

  @Column(name = "sort_order")
  private Integer sortOrder = 0;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
    if (active == null) active = true;
    if (sortOrder == null) sortOrder = 0;
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
