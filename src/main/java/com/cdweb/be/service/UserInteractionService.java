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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserInteractionService {

  private final UserInteractionRepository repository;
  private final org.springframework.data.redis.core.RedisTemplate<String, String>
      redisTemplate; // ✅ Tiêm RedisTemplate vào Service

  @Transactional
  public UserInteractionDto.Response trackInteraction(UserInteractionDto.Request request) {
    BigDecimal score = BigDecimal.ZERO;
    boolean isUniqueAction = false;

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

    // 💡 TỐI ƯU HIỆU NĂNG: Nếu Action là VIEW (Rất dồi dào/tần suất cao) $\rightarrow$ bắn vào
    // Redis Queue
    if ("VIEW".equalsIgnoreCase(request.getActionType())) {
      try {
        // Format Chuỗi: userId|productId|VIEW|timestamp
        String logData =
            String.format(
                "%s|%s|%s|%s",
                request.getUserId(), request.getProductId(), "VIEW", System.currentTimeMillis());

        redisTemplate.opsForList().rightPush("analytics:interactions_queue", logData);

        log.debug(
            "Logged VIEW interaction to Redis for User: {}, Product: {}",
            request.getUserId(),
            request.getProductId());

        return UserInteractionDto.Response.builder()
            .userId(request.getUserId())
            .productId(request.getProductId())
            .actionType("VIEW")
            .interactionScore(score)
            .message("Interaction logged on view queue buffer successfully.")
            .build();
      } catch (Exception e) {
        log.error("Failed to push view log to Redis, fallback to MySQL save: {}", e.getMessage());
        // Nếu Redis lỗi, code bên dưới sẽ chạy bình thường giúp lưu mượt cho MySQL
      }
    }

    UserInteraction interaction =
        UserInteraction.builder()
            .userId(request.getUserId())
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

  // ⏰ TÁC VỤ CHẠY NGẦM: Định kỳ xả hàng loạt View Log từ Redis về MySQL
  @org.springframework.scheduling.annotation.Scheduled(fixedRate = 60000)
  @Transactional
  public void syncViewLogsFromRedisToMySQL() {
    String mainKey = "analytics:interactions_queue";
    String tempKey = "analytics:interactions_queue_sync_" + System.currentTimeMillis();

    Boolean hasKey = redisTemplate.hasKey(mainKey);
    if (Boolean.FALSE.equals(hasKey)) {
      return;
    }

    // 💡 GIẢI PHÁP AN TOÀN: Rename key sang một key phụ TEMP để xử lý listLogs tách biệt
    // Việc này ngăn ngừa race-condition làm mất dữ liệu khi người dùng vừa view sản phẩm vào queue
    try {
      redisTemplate.rename(mainKey, tempKey);
    } catch (Exception e) {
      // Trường hợp key biến mất trong khoảnh khắc tranh chấp (rất hiếm)
      return;
    }

    List<String> listLogs = redisTemplate.opsForList().range(tempKey, 0, -1);
    if (listLogs == null || listLogs.isEmpty()) {
      redisTemplate.delete(tempKey);
      return;
    }

    log.info("CronJob: Syncying {} view logs from Redis List back to MySQL...", listLogs.size());
    List<UserInteraction> batchToSave = new java.util.ArrayList<>();

    for (String logStr : listLogs) {
      try {
        String[] parts = logStr.split("\\|");
        if (parts.length >= 3) {
          Integer userId = parts[0].equals("null") ? null : Integer.parseInt(parts[0]);
          Integer productId = Integer.parseInt(parts[1]);
          String actionType = parts[2];
          BigDecimal score = BigDecimal.valueOf(1.0); // Trọng số mặc định cho VIEW

          batchToSave.add(
              UserInteraction.builder()
                  .userId(userId)
                  .productId(productId)
                  .actionType(actionType)
                  .interactionScore(score)
                  .createdAt(LocalDateTime.now())
                  .build());
        }
      } catch (Exception e) {
        log.error("Error parsing view log string from Redis: {}", logStr, e);
      }
    }

    // 2. Batch Save vào MySQL
    if (!batchToSave.isEmpty()) {
      repository.saveAll(batchToSave);
      log.info("CronJob: Saved batch of {} view interact logs successfully.", batchToSave.size());
    }

    // 3. Xóa Key tạm khi sync hoàn tất
    redisTemplate.delete(tempKey);
  }
}
