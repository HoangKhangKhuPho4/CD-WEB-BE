package com.cdweb.be.service.payment;

import com.cdweb.be.config.PaymentConfig;
import com.cdweb.be.dto.PaymentDto;
import com.cdweb.be.entity.Order;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Tích hợp ZaloPay Payment Gateway.
 *
 * <p>Flow: 1. Server gửi request tạo đơn → ZaloPay trả về order_url 2. Redirect user tới order_url
 * → user thanh toán trên ZaloPay 3. ZaloPay gọi callback URL → server cập nhật trạng thái 4.
 * (Optional) Server query trạng thái đơn từ ZaloPay
 *
 * <p>Docs: https://docs.zalopay.vn/v2/
 */
@Service
public class ZaloPayService {

  private static final Logger log = LoggerFactory.getLogger(ZaloPayService.class);

  @Autowired private PaymentConfig paymentConfig;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  /** Tạo đơn thanh toán ZaloPay */
  public PaymentDto.GatewayCreateResult createPaymentUrl(Order order, String ipAddress) {
    try {
      PaymentConfig.ZaloPay zpConfig = paymentConfig.getZalopay();

      String transactionRef = generateTransactionRef(order.getOrderCode());
      long amount = order.getTotalAmount().longValue();
      String appTime = String.valueOf(System.currentTimeMillis());
      String appTransId =
          LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd")) + "_" + transactionRef;

      // Embed data
      Map<String, String> embedData = new HashMap<>();
      embedData.put("redirecturl", zpConfig.getRedirectUrl());
      String embedDataStr = objectMapper.writeValueAsString(embedData);

      // Items (empty JSON array for simple case)
      String items = "[]";

      // Build MAC data: appid|app_trans_id|appuser|amount|apptime|embeddata|item
      String macData =
          zpConfig.getAppId()
              + "|"
              + appTransId
              + "|"
              + "user_"
              + order.getUser().getId()
              + "|"
              + amount
              + "|"
              + appTime
              + "|"
              + embedDataStr
              + "|"
              + items;

      String mac = hmacSHA256(zpConfig.getKey1(), macData);

      // Build form data
      Map<String, String> formData = new LinkedHashMap<>();
      formData.put("app_id", zpConfig.getAppId());
      formData.put("app_user", "user_" + order.getUser().getId());
      formData.put("app_time", appTime);
      formData.put("amount", String.valueOf(amount));
      formData.put("app_trans_id", appTransId);
      formData.put("embed_data", embedDataStr);
      formData.put("item", items);
      formData.put("description", "Thanh toan don hang " + order.getOrderCode());
      formData.put("bank_code", "");
      formData.put("callback_url", zpConfig.getCallbackUrl());
      formData.put("mac", mac);

      // Build form-urlencoded body
      StringBuilder formBody = new StringBuilder();
      for (Map.Entry<String, String> entry : formData.entrySet()) {
        if (formBody.length() > 0) formBody.append("&");
        formBody
            .append(entry.getKey())
            .append("=")
            .append(java.net.URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
      }

      log.debug("ZaloPay create order request: {}", formBody);

      HttpRequest httpRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(zpConfig.getCreateOrderUrl()))
              .header("Content-Type", "application/x-www-form-urlencoded")
              .POST(HttpRequest.BodyPublishers.ofString(formBody.toString()))
              .build();

      HttpResponse<String> httpResponse =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      log.debug("ZaloPay create order response: {}", httpResponse.body());

      @SuppressWarnings("unchecked")
      Map<String, Object> responseMap = objectMapper.readValue(httpResponse.body(), Map.class);

      int returnCode = ((Number) responseMap.get("return_code")).intValue();
      if (returnCode == 1) {
        String orderUrl = (String) responseMap.get("order_url");
        log.info(
            "ZaloPay payment URL created for order: {}, appTransId: {}",
            order.getOrderCode(),
            appTransId);

        return PaymentDto.GatewayCreateResult.builder()
            .success(true)
            .paymentUrl(orderUrl)
            .transactionRef(appTransId)
            .build();
      } else {
        String returnMessage = (String) responseMap.get("return_message");
        log.warn(
            "ZaloPay create order failed. ReturnCode: {}, Message: {}", returnCode, returnMessage);

        return PaymentDto.GatewayCreateResult.builder()
            .success(false)
            .errorMessage("ZaloPay error: " + returnMessage)
            .build();
      }

    } catch (java.io.IOException | InterruptedException e) {
      log.error(
          "Network error creating ZaloPay payment URL for order: {}", order.getOrderCode(), e);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return PaymentDto.GatewayCreateResult.builder()
          .success(false)
          .errorMessage("Lỗi kết nối tới ZaloPay: " + e.getMessage())
          .build();
    } catch (Exception e) {
      log.error("Failed to create ZaloPay payment URL for order: {}", order.getOrderCode(), e);
      return PaymentDto.GatewayCreateResult.builder()
          .success(false)
          .errorMessage("Không thể tạo URL thanh toán ZaloPay: " + e.getMessage())
          .build();
    }
  }

  /**
   * Verify callback từ ZaloPay ZaloPay gửi callback dạng JSON: { data: string, mac: string, type:
   * int }
   */
  public PaymentDto.GatewayCallbackResult verifyCallback(String dataStr, String macStr) {
    try {
      PaymentConfig.ZaloPay zpConfig = paymentConfig.getZalopay();

      // Verify MAC: mac = HMAC_SHA256(key2, data)
      String expectedMac = hmacSHA256(zpConfig.getKey2(), dataStr);
      boolean verified = expectedMac.equals(macStr);

      if (!verified) {
        log.warn("ZaloPay callback MAC verification failed");
        return PaymentDto.GatewayCallbackResult.builder()
            .success(false)
            .verified(false)
            .message("Invalid ZaloPay MAC")
            .build();
      }

      @SuppressWarnings("unchecked")
      Map<String, Object> dataMap = objectMapper.readValue(dataStr, Map.class);

      String appTransId = (String) dataMap.get("app_trans_id");
      String zpTransId =
          dataMap.get("zp_trans_id") != null ? dataMap.get("zp_trans_id").toString() : null;
      Number amountNum = (Number) dataMap.get("amount");
      BigDecimal amount =
          amountNum != null ? BigDecimal.valueOf(amountNum.longValue()) : BigDecimal.ZERO;

      // Tách orderCode từ appTransId (format: yyMMdd_orderCode_timestamp)
      String orderCode = extractOrderCode(appTransId);

      log.info("ZaloPay callback verified. AppTransId: {}, ZPTransId: {}", appTransId, zpTransId);

      return PaymentDto.GatewayCallbackResult.builder()
          .success(true)
          .verified(true)
          .orderCode(orderCode)
          .transactionRef(appTransId)
          .gatewayTransactionId(zpTransId)
          .amount(amount)
          .responseCode("1")
          .message("Thanh toán ZaloPay thành công")
          .build();

    } catch (Exception e) {
      log.error("Error verifying ZaloPay callback", e);
      return PaymentDto.GatewayCallbackResult.builder()
          .success(false)
          .verified(false)
          .message("Lỗi xác thực callback ZaloPay: " + e.getMessage())
          .build();
    }
  }

  /**
   * Extract orderCode from ZaloPay appTransId. appTransId format:
   * "yyMMdd_ORD-yyyyMMddHHmmssSSS_timestamp" We need to get "ORD-yyyyMMddHHmmssSSS"
   */
  private String extractOrderCode(String appTransId) {
    if (appTransId == null) return null;
    // Remove date prefix: "yyMMdd_"
    int firstUnderscore = appTransId.indexOf("_");
    if (firstUnderscore < 0) return appTransId;
    String afterDate = appTransId.substring(firstUnderscore + 1);
    // Remove timestamp suffix: "_timestamp"
    int lastUnderscore = afterDate.lastIndexOf("_");
    return lastUnderscore > 0 ? afterDate.substring(0, lastUnderscore) : afterDate;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helper methods
  // ═══════════════════════════════════════════════════════════════════════════

  private String generateTransactionRef(String orderCode) {
    return orderCode + "_" + System.currentTimeMillis();
  }

  /** HMAC-SHA256 signing (ZaloPay sử dụng SHA256) */
  private String hmacSHA256(String key, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
      mac.init(secretKeySpec);
      byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException("Error computing HMAC-SHA256", e);
    }
  }
}
