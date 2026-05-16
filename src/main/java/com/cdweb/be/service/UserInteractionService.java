package com.cdweb.be.service;

import com.cdweb.be.dto.UserInteractionDto;
import com.cdweb.be.entity.UserInteraction;
import com.cdweb.be.repository.UserInteractionRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInteractionService {

  private final UserInteractionRepository repository;
  private final RedisTemplate<String, String> redisTemplate;

  @Transactional
  public UserInteractionDto.Response trackInteraction(UserInteractionDto.Request request) {
    BigDecimal score = BigDecimal.ZERO;
    boolean isUniqueAction = false;

    // Logic tính điểm giữ nguyên
    switch (request.getActionType().toUpperCase()) {
      case "VIEW":
        score = BigDecimal.valueOf(1.0);
        break;
      case "ADD_TO_CART":
        score = BigDecimal.valueOf(3.0);
        isUniqueAction = true;
        break;
      case "PURCHASE":
        score = BigDecimal.valueOf(5.0);
        break;
      case "RATED":
        score = request.getRating() != null ? request.getRating() : BigDecimal.valueOf(4.0);
        isUniqueAction = true;
        break;
      default:
        score = BigDecimal.valueOf(1.0);
    }

    if (isUniqueAction) {
      // Repository hiện tại phải nhận Long userId
      Optional<UserInteraction> existing =
              repository.findByUserIdAndProductIdAndActionType(
                      request.getUserId(), request.getProductId(), request.getActionType());
      if (existing.isPresent()) {
        UserInteraction existingInt = existing.get();
        return UserInteractionDto.Response.builder()
                .id(existingInt.getId())
                .userId(existingInt.getUserId())
                .productId(existingInt.getProductId())
                .actionType(existingInt.getActionType())
                .interactionScore(existingInt.getInteractionScore())
                .message("Action already tracked.")
                .build();
      }
    }

    // 💡 Redis Buffer cho VIEW
    if ("VIEW".equalsIgnoreCase(request.getActionType())) {
      try {
        String logData =
                String.format(
                        "%s|%s|%s|%s",
                        request.getUserId(), request.getProductId(), "VIEW", System.currentTimeMillis());

        redisTemplate.opsForList().rightPush("analytics:interactions_queue", logData);

        return UserInteractionDto.Response.builder()
                .userId(request.getUserId())
                .productId(request.getProductId())
                .actionType("VIEW")
                .interactionScore(score)
                .message("Interaction logged on view queue buffer successfully.")
                .build();
      } catch (Exception e) {
        log.error("Redis fallback to MySQL: {}", e.getMessage());
      }
    }

    UserInteraction interaction =
            UserInteraction.builder()
                    .userId(request.getUserId()) // Builder đã nhận Long
                    .productId(request.getProductId())
                    .actionType(request.getActionType())
                    .rating(request.getRating())
                    .interactionScore(score)
                    .createdAt(LocalDateTime.now())
                    .build();

    UserInteraction saved = repository.save(interaction);

    return UserInteractionDto.Response.builder()
            .id(saved.getId())
            .userId(saved.getUserId())
            .productId(saved.getProductId())
            .actionType(saved.getActionType())
            .interactionScore(saved.getInteractionScore())
            .message("Interaction tracked successfully.")
            .build();
  }

  @Scheduled(fixedRate = 60000)
  @Transactional
  public void syncViewLogsFromRedisToMySQL() {
    String mainKey = "analytics:interactions_queue";
    String tempKey = "analytics:interactions_queue_sync_" + System.currentTimeMillis();

    if (Boolean.FALSE.equals(redisTemplate.hasKey(mainKey))) return;

    try {
      redisTemplate.rename(mainKey, tempKey);
    } catch (Exception e) { return; }

    List<String> listLogs = redisTemplate.opsForList().range(tempKey, 0, -1);
    if (listLogs == null || listLogs.isEmpty()) {
      redisTemplate.delete(tempKey);
      return;
    }

    List<UserInteraction> batchToSave = new java.util.ArrayList<>();

    for (String logStr : listLogs) {
      try {
        String[] parts = logStr.split("\\|");
        if (parts.length >= 3) {
          // ĐÃ SỬA: Integer.parseInt -> Long.parseLong
          Long userId = (parts[0] == null || parts[0].equals("null")) ? null : Long.parseLong(parts[0]);
          Integer productId = Integer.parseInt(parts[1]);
          String actionType = parts[2];
          BigDecimal score = BigDecimal.valueOf(1.0);

          batchToSave.add(
                  UserInteraction.builder()
                          .userId(userId) // Build với Long
                          .productId(productId)
                          .actionType(actionType)
                          .interactionScore(score)
                          .createdAt(LocalDateTime.now())
                          .build());
        }
      } catch (Exception e) {
        log.error("Error parsing Redis log: {}", logStr, e);
      }
    }

    if (!batchToSave.isEmpty()) {
      repository.saveAll(batchToSave);
    }
    redisTemplate.delete(tempKey);
  }
}