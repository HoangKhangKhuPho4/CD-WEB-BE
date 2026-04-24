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
import java.util.HashMap;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Tích hợp Momo Payment Gateway.
 *
 * <p>Flow: 1. Server gửi request tạo đơn thanh toán → Momo trả về payUrl 2. Redirect user tới
 * payUrl → user thanh toán trên Momo 3. Momo gọi IPN URL → server cập nhật trạng thái 4. User
 * redirect về return URL
 *
 * <p>Docs: https://developers.momo.vn/v3/vi/docs/payment/api/wallet/pay-with-token
 */
@Service
public class MomoService {

  private static final Logger log = LoggerFactory.getLogger(MomoService.class);

  @Autowired private PaymentConfig paymentConfig;

  private final ObjectMapper objectMapper = new ObjectMapper();
  private final HttpClient httpClient = HttpClient.newHttpClient();

  /** Tạo URL thanh toán Momo */
  public PaymentDto.GatewayCreateResult createPaymentUrl(Order order, String ipAddress) {
    try {
      PaymentConfig.Momo momoConfig = paymentConfig.getMomo();

      String transactionRef = generateTransactionRef(order.getOrderCode());
      String requestId = transactionRef;
      long amount = order.getTotalAmount().longValue();
      String orderInfo = "Thanh toan don hang " + order.getOrderCode();
      String extraData = ""; // Base64 encoded JSON if needed

      // Build raw signature
      String rawSignature =
          "accessKey="
              + momoConfig.getAccessKey()
              + "&amount="
              + amount
              + "&extraData="
              + extraData
              + "&ipnUrl="
              + momoConfig.getIpnUrl()
              + "&orderId="
              + transactionRef
              + "&orderInfo="
              + orderInfo
              + "&partnerCode="
              + momoConfig.getPartnerCode()
              + "&redirectUrl="
              + momoConfig.getReturnUrl()
              + "&requestId="
              + requestId
              + "&requestType="
              + momoConfig.getRequestType();

      String signature = hmacSHA256(momoConfig.getSecretKey(), rawSignature);

      // Build request body
      Map<String, Object> requestBody = new HashMap<>();
      requestBody.put("partnerCode", momoConfig.getPartnerCode());
      requestBody.put("partnerName", "Electro Store");
      requestBody.put("storeId", "ElectroStore");
      requestBody.put("requestId", requestId);
      requestBody.put("amount", amount);
      requestBody.put("orderId", transactionRef);
      requestBody.put("orderInfo", orderInfo);
      requestBody.put("redirectUrl", momoConfig.getReturnUrl());
      requestBody.put("ipnUrl", momoConfig.getIpnUrl());
      requestBody.put("lang", "vi");
      requestBody.put("requestType", momoConfig.getRequestType());
      requestBody.put("autoCapture", true);
      requestBody.put("extraData", extraData);
      requestBody.put("signature", signature);

      String jsonBody = objectMapper.writeValueAsString(requestBody);

      log.debug("Momo create payment request: {}", jsonBody);

      // Send HTTP request to Momo
      HttpRequest httpRequest =
          HttpRequest.newBuilder()
              .uri(URI.create(momoConfig.getApiUrl()))
              .header("Content-Type", "application/json")
              .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
              .build();

      HttpResponse<String> httpResponse =
          httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

      log.debug("Momo create payment response: {}", httpResponse.body());

      @SuppressWarnings("unchecked")
      Map<String, Object> responseMap = objectMapper.readValue(httpResponse.body(), Map.class);

      int resultCode = ((Number) responseMap.get("resultCode")).intValue();
      if (resultCode == 0) {
        String payUrl = (String) responseMap.get("payUrl");
        log.info(
            "Momo payment URL created for order: {}, txnRef: {}",
            order.getOrderCode(),
            transactionRef);

        return PaymentDto.GatewayCreateResult.builder()
            .success(true)
            .paymentUrl(payUrl)
            .transactionRef(transactionRef)
            .build();
      } else {
        String message = (String) responseMap.get("message");
        log.warn("Momo create payment failed. ResultCode: {}, Message: {}", resultCode, message);

        return PaymentDto.GatewayCreateResult.builder()
            .success(false)
            .errorMessage("Momo error: " + message)
            .build();
      }

    } catch (java.io.IOException | InterruptedException e) {
      log.error("Network error creating Momo payment URL for order: {}", order.getOrderCode(), e);
      if (e instanceof InterruptedException) {
        Thread.currentThread().interrupt();
      }
      return PaymentDto.GatewayCreateResult.builder()
          .success(false)
          .errorMessage("Lỗi kết nối tới Momo: " + e.getMessage())
          .build();
    } catch (Exception e) {
      log.error("Failed to create Momo payment URL for order: {}", order.getOrderCode(), e);
      return PaymentDto.GatewayCreateResult.builder()
          .success(false)
          .errorMessage("Không thể tạo URL thanh toán Momo: " + e.getMessage())
          .build();
    }
  }

  /** Verify IPN callback từ Momo */
  public PaymentDto.GatewayCallbackResult verifyCallback(Map<String, String> params) {
    try {
      PaymentConfig.Momo momoConfig = paymentConfig.getMomo();

      String orderId = params.get("orderId");
      String requestId = params.get("requestId");
      String amount = params.get("amount");
      String orderInfo = params.get("orderInfo");
      String orderType = params.get("orderType");
      String transId = params.get("transId");
      String resultCode = params.get("resultCode");
      String message = params.get("message");
      String extraData = params.get("extraData");
      String signature = params.get("signature");

      // Rebuild signature for verification
      String rawSignature =
          "accessKey="
              + momoConfig.getAccessKey()
              + "&amount="
              + amount
              + "&extraData="
              + (extraData != null ? extraData : "")
              + "&message="
              + message
              + "&orderId="
              + orderId
              + "&orderInfo="
              + orderInfo
              + "&orderType="
              + orderType
              + "&partnerCode="
              + momoConfig.getPartnerCode()
              + "&payType="
              + params.getOrDefault("payType", "")
              + "&requestId="
              + requestId
              + "&responseTime="
              + params.getOrDefault("responseTime", "")
              + "&resultCode="
              + resultCode
              + "&transId="
              + transId;

      String expectedSignature = hmacSHA256(momoConfig.getSecretKey(), rawSignature);
      boolean verified = expectedSignature.equals(signature);

      if (!verified) {
        log.warn("Momo callback signature verification failed. OrderId: {}", orderId);
        return PaymentDto.GatewayCallbackResult.builder()
            .success(false)
            .verified(false)
            .message("Invalid Momo signature")
            .build();
      }

      String orderCode = extractOrderCode(orderId);
      boolean paymentSuccess = "0".equals(resultCode);

      log.info(
          "Momo callback verified. OrderId: {}, ResultCode: {}, Success: {}",
          orderId,
          resultCode,
          paymentSuccess);

      return PaymentDto.GatewayCallbackResult.builder()
          .success(paymentSuccess)
          .verified(true)
          .orderCode(orderCode)
          .transactionRef(orderId)
          .gatewayTransactionId(transId)
          .amount(amount != null ? new BigDecimal(amount) : BigDecimal.ZERO)
          .responseCode(resultCode)
          .message(
              paymentSuccess
                  ? "Thanh toán Momo thành công"
                  : "Thanh toán Momo thất bại: " + message)
          .build();

    } catch (Exception e) {
      log.error("Error verifying Momo callback", e);
      return PaymentDto.GatewayCallbackResult.builder()
          .success(false)
          .verified(false)
          .message("Lỗi xác thực callback Momo: " + e.getMessage())
          .build();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helper methods
  // ═══════════════════════════════════════════════════════════════════════════

  private String generateTransactionRef(String orderCode) {
    return orderCode + "_" + System.currentTimeMillis();
  }

  private String extractOrderCode(String transactionRef) {
    if (transactionRef == null) return null;
    int idx = transactionRef.lastIndexOf("_");
    return idx > 0 ? transactionRef.substring(0, idx) : transactionRef;
  }

  /** HMAC-SHA256 signing (Momo sử dụng SHA256) */
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
