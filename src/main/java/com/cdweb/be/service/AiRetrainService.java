package com.cdweb.be.service;

import com.cdweb.be.entity.SystemConfiguration;
import com.cdweb.be.repository.SystemConfigurationRepository;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiRetrainService {

  private final RestTemplate restTemplate;
  private final SystemConfigurationRepository systemConfigurationRepository;

  @Value("${app.ai.base-url:http://localhost:5000}")
  private String defaultAiBaseUrl;

  @Async
  public void runRetrainAsync() {
    SystemConfiguration config =
        systemConfigurationRepository
            .findById(SystemConfiguration.SINGLETON_ID)
            .orElseGet(this::createDefaultConfig);

    String baseUrl = resolveBaseUrl(config);
    String url = baseUrl + "/retrain";
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      HttpEntity<Map<String, Object>> body =
          new HttpEntity<>(
              Map.of(
                  "epochs", config.getSvdEpochs() != null ? config.getSvdEpochs() : 20,
                  "rank", config.getSvdRank() != null ? config.getSvdRank() : 10),
              headers);
      restTemplate.postForEntity(url, body, Map.class);
      markStatus(config, "SUCCESS", "Huấn luyện model SVD thành công.");
      log.info("AI retrain completed via {}", url);
    } catch (RestClientException ex) {
      log.warn("AI retrain HTTP failed ({}): {}", url, ex.getMessage());
      markStatus(
          config,
          "FAILED",
          "Không kết nối được AI service tại "
              + baseUrl
              + ". Đảm bảo Flask service đang chạy và có endpoint POST /retrain.");
    } catch (Exception ex) {
      log.error("AI retrain unexpected error", ex);
      markStatus(config, "FAILED", ex.getMessage());
    }
  }

  private void markStatus(SystemConfiguration config, String status, String message) {
    config.setRetrainStatus(status);
    config.setRetrainMessage(message);
    if ("SUCCESS".equals(status)) {
      config.setLastRetrainAt(LocalDateTime.now());
    }
    systemConfigurationRepository.save(config);
  }

  private SystemConfiguration createDefaultConfig() {
    SystemConfiguration c = new SystemConfiguration();
    c.setId(SystemConfiguration.SINGLETON_ID);
    c.setAiServiceBaseUrl(defaultAiBaseUrl.replaceAll("/+$", ""));
    return systemConfigurationRepository.save(c);
  }

  private String resolveBaseUrl(SystemConfiguration config) {
    String fromDb = config.getAiServiceBaseUrl();
    if (fromDb != null && !fromDb.isBlank()) {
      return fromDb.trim().replaceAll("/+$", "");
    }
    return defaultAiBaseUrl.trim().replaceAll("/+$", "");
  }
}
