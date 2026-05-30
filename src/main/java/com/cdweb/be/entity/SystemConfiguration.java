package com.cdweb.be.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/** Cấu hình hệ thống singleton (id = 1). */
@Entity
@Table(name = "system_configuration")
@Getter
@Setter
public class SystemConfiguration {

  public static final long SINGLETON_ID = 1L;

  @Id
  private Long id = SINGLETON_ID;

  // ── Cấu hình chung ───────────────────────────────────────────────────────
  @Column(name = "default_shipping_fee", precision = 15, scale = 2)
  private BigDecimal defaultShippingFee = new BigDecimal("30000");

  @Column(name = "free_shipping_threshold", precision = 15, scale = 2)
  private BigDecimal freeShippingThreshold = new BigDecimal("500000");

  @Column(name = "cod_enabled")
  private Boolean codEnabled = true;

  @Column(name = "vnpay_enabled")
  private Boolean vnpayEnabled = true;

  @Column(name = "momo_enabled")
  private Boolean momoEnabled = true;

  @Column(name = "zalopay_enabled")
  private Boolean zalopayEnabled = true;

  @Column(name = "support_email", length = 255)
  private String supportEmail = "support@cdweb.vn";

  @Column(name = "support_hotline", length = 50)
  private String supportHotline = "1900 1234";

  @Column(name = "site_footer_text", columnDefinition = "TEXT")
  private String siteFooterText = "© Bảo Khang Gadget — Cửa hàng công nghệ uy tín";

  @Column(name = "platform_language", length = 10)
  private String platformLanguage = "vi";

  // ── AI / Recommendation (SVD) ─────────────────────────────────────────────
  @Column(name = "recommendation_weight")
  private Double recommendationWeight = 1.0;

  @Column(name = "svd_rank")
  private Integer svdRank = 10;

  @Column(name = "svd_epochs")
  private Integer svdEpochs = 20;

  @Column(name = "cache_ttl_seconds")
  private Integer cacheTtlSeconds = 300;

  @Column(name = "ai_service_base_url", length = 500)
  private String aiServiceBaseUrl;

  @Column(name = "retrain_status", length = 30)
  private String retrainStatus = "IDLE";

  @Column(name = "retrain_message", columnDefinition = "TEXT")
  private String retrainMessage;

  @Column(name = "last_retrain_at")
  private LocalDateTime lastRetrainAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  void onCreate() {
    if (id == null) {
      id = SINGLETON_ID;
    }
    updatedAt = LocalDateTime.now();
    applyDefaults();
  }

  @PreUpdate
  void onUpdate() {
    updatedAt = LocalDateTime.now();
    applyDefaults();
  }

  private void applyDefaults() {
    if (defaultShippingFee == null) {
      defaultShippingFee = new BigDecimal("30000");
    }
    if (freeShippingThreshold == null) {
      freeShippingThreshold = new BigDecimal("500000");
    }
    if (codEnabled == null) codEnabled = true;
    if (vnpayEnabled == null) vnpayEnabled = true;
    if (momoEnabled == null) momoEnabled = true;
    if (zalopayEnabled == null) zalopayEnabled = true;
    if (recommendationWeight == null) recommendationWeight = 1.0;
    if (svdRank == null) svdRank = 10;
    if (svdEpochs == null) svdEpochs = 20;
    if (cacheTtlSeconds == null) cacheTtlSeconds = 300;
    if (retrainStatus == null) retrainStatus = "IDLE";
    if (platformLanguage == null) platformLanguage = "vi";
  }
}
