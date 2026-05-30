package com.cdweb.be.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public final class SystemConfigDto {

  private SystemConfigDto() {}

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GeneralSettingsResponse {
    private BigDecimal defaultShippingFee;
    private BigDecimal freeShippingThreshold;
    private Boolean codEnabled;
    private Boolean vnpayEnabled;
    private Boolean momoEnabled;
    private Boolean zalopayEnabled;
    private String supportEmail;
    private String supportHotline;
    private String siteFooterText;
    private String platformLanguage;
    private LocalDateTime updatedAt;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class GeneralSettingsUpdateRequest {
    @NotNull @DecimalMin("0") private BigDecimal defaultShippingFee;
    @NotNull @DecimalMin("0") private BigDecimal freeShippingThreshold;
    @NotNull private Boolean codEnabled;
    @NotNull private Boolean vnpayEnabled;
    @NotNull private Boolean momoEnabled;
    @NotNull private Boolean zalopayEnabled;
    @NotBlank @Email private String supportEmail;
    @NotBlank private String supportHotline;
    private String siteFooterText;
    @NotBlank private String platformLanguage;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AiConfigResponse {
    private Double recommendationWeight;
    private Integer svdRank;
    private Integer svdEpochs;
    private Integer cacheTtlSeconds;
    private String aiServiceBaseUrl;
    private String retrainStatus;
    private String retrainMessage;
    private LocalDateTime lastRetrainAt;
    private LocalDateTime updatedAt;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AiConfigUpdateRequest {
    @NotNull @Min(0) @Max(10) private Double recommendationWeight;
    @NotNull @Min(1) @Max(100) private Integer svdRank;
    @NotNull @Min(1) @Max(500) private Integer svdEpochs;
    @NotNull @Min(60) @Max(86400) private Integer cacheTtlSeconds;
    @NotBlank private String aiServiceBaseUrl;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class RetrainResponse {
    private String status;
    private String message;
    private LocalDateTime startedAt;
  }
}
