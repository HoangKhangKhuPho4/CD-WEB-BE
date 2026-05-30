package com.cdweb.be.service;

import com.cdweb.be.dto.SystemConfigDto;
import com.cdweb.be.entity.SystemConfiguration;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.repository.SystemConfigurationRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class SystemConfigService {

  private final SystemConfigurationRepository repository;
  private final AiRetrainService aiRetrainService;

  @Value("${app.ai.base-url:http://localhost:5000}")
  private String defaultAiBaseUrl;

  @Transactional(readOnly = true)
  public SystemConfigDto.GeneralSettingsResponse getGeneralSettings() {
    return toGeneralResponse(getOrCreate());
  }

  public SystemConfigDto.GeneralSettingsResponse updateGeneralSettings(
      SystemConfigDto.GeneralSettingsUpdateRequest request) {
    SystemConfiguration config = getOrCreate();
    config.setDefaultShippingFee(request.getDefaultShippingFee());
    config.setFreeShippingThreshold(request.getFreeShippingThreshold());
    config.setCodEnabled(request.getCodEnabled());
    config.setVnpayEnabled(request.getVnpayEnabled());
    config.setMomoEnabled(request.getMomoEnabled());
    config.setZalopayEnabled(request.getZalopayEnabled());
    config.setSupportEmail(request.getSupportEmail());
    config.setSupportHotline(request.getSupportHotline());
    config.setSiteFooterText(request.getSiteFooterText());
    config.setPlatformLanguage(request.getPlatformLanguage());
    return toGeneralResponse(repository.save(config));
  }

  @Transactional(readOnly = true)
  public SystemConfigDto.AiConfigResponse getAiConfiguration() {
    return toAiResponse(getOrCreate());
  }

  public SystemConfigDto.AiConfigResponse updateAiConfiguration(
      SystemConfigDto.AiConfigUpdateRequest request) {
    SystemConfiguration config = getOrCreate();
    config.setRecommendationWeight(request.getRecommendationWeight());
    config.setSvdRank(request.getSvdRank());
    config.setSvdEpochs(request.getSvdEpochs());
    config.setCacheTtlSeconds(request.getCacheTtlSeconds());
    config.setAiServiceBaseUrl(normalizeBaseUrl(request.getAiServiceBaseUrl()));
    return toAiResponse(repository.save(config));
  }

  public SystemConfigDto.RetrainResponse triggerAiRetrain() {
    SystemConfiguration config = getOrCreate();
    if ("RUNNING".equalsIgnoreCase(config.getRetrainStatus())) {
      throw new BadRequestException("Model đang được huấn luyện, vui lòng đợi hoàn tất.");
    }
    config.setRetrainStatus("RUNNING");
    config.setRetrainMessage("Đang khởi chạy huấn luyện...");
    config.setLastRetrainAt(LocalDateTime.now());
    repository.save(config);
    aiRetrainService.runRetrainAsync();
    return SystemConfigDto.RetrainResponse.builder()
        .status("RUNNING")
        .message("Đã gửi yêu cầu huấn luyện lại model.")
        .startedAt(config.getLastRetrainAt())
        .build();
  }

  @Transactional(readOnly = true)
  public String resolveAiBaseUrl() {
    SystemConfiguration config = getOrCreate();
    String fromDb = config.getAiServiceBaseUrl();
    if (fromDb != null && !fromDb.isBlank()) {
      return normalizeBaseUrl(fromDb);
    }
    return normalizeBaseUrl(defaultAiBaseUrl);
  }

  public SystemConfiguration getOrCreate() {
    return repository
        .findById(SystemConfiguration.SINGLETON_ID)
        .orElseGet(
            () -> {
              SystemConfiguration created = new SystemConfiguration();
              created.setId(SystemConfiguration.SINGLETON_ID);
              if (created.getAiServiceBaseUrl() == null || created.getAiServiceBaseUrl().isBlank()) {
                created.setAiServiceBaseUrl(normalizeBaseUrl(defaultAiBaseUrl));
              }
              return repository.save(created);
            });
  }

  private static String normalizeBaseUrl(String url) {
    if (url == null || url.isBlank()) {
      return "http://localhost:5000";
    }
    return url.trim().replaceAll("/+$", "");
  }

  private static SystemConfigDto.GeneralSettingsResponse toGeneralResponse(SystemConfiguration c) {
    return SystemConfigDto.GeneralSettingsResponse.builder()
        .defaultShippingFee(c.getDefaultShippingFee())
        .freeShippingThreshold(c.getFreeShippingThreshold())
        .codEnabled(c.getCodEnabled())
        .vnpayEnabled(c.getVnpayEnabled())
        .momoEnabled(c.getMomoEnabled())
        .zalopayEnabled(c.getZalopayEnabled())
        .supportEmail(c.getSupportEmail())
        .supportHotline(c.getSupportHotline())
        .siteFooterText(c.getSiteFooterText())
        .platformLanguage(c.getPlatformLanguage())
        .updatedAt(c.getUpdatedAt())
        .build();
  }

  private static SystemConfigDto.AiConfigResponse toAiResponse(SystemConfiguration c) {
    return SystemConfigDto.AiConfigResponse.builder()
        .recommendationWeight(c.getRecommendationWeight())
        .svdRank(c.getSvdRank())
        .svdEpochs(c.getSvdEpochs())
        .cacheTtlSeconds(c.getCacheTtlSeconds())
        .aiServiceBaseUrl(c.getAiServiceBaseUrl())
        .retrainStatus(c.getRetrainStatus())
        .retrainMessage(c.getRetrainMessage())
        .lastRetrainAt(c.getLastRetrainAt())
        .updatedAt(c.getUpdatedAt())
        .build();
  }
}
