package com.cdweb.be.service.payment;

import com.cdweb.be.config.PaymentConfig;
import com.cdweb.be.dto.PaymentDto;
import com.cdweb.be.entity.Order;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

/**
 * Tích hợp VNPay Payment Gateway.
 *
 * <p>Flow: 1. Tạo URL thanh toán VNPay → redirect user tới URL đó 2. User thanh toán trên VNPay →
 * VNPay gọi IPN callback về server 3. User redirect về return URL → server verify và hiển thị kết
 * quả
 *
 * <p>Docs: https://sandbox.vnpayment.vn/apis/docs/thanh-toan-pay/pay.html
 */
@Service
public class VNPayService {

  private static final Logger log = LoggerFactory.getLogger(VNPayService.class);

  @Autowired private PaymentConfig paymentConfig;

  private final RestTemplate restTemplate = new RestTemplate();

  /** Tạo URL thanh toán VNPay */
  @SuppressWarnings("PMD.AvoidUsingHardCodedIP")
  public PaymentDto.GatewayCreateResult createPaymentUrl(
      Order order, String ipAddress, String bankCode, String language) {
    try {
      PaymentConfig.VnPay vnpayConfig = paymentConfig.getVnpay();

      // Tạo mã giao dịch nội bộ
      String transactionRef = generateTransactionRef(order.getOrderCode());

      // Số tiền (VNPay yêu cầu nhân 100, không có phần thập phân)
      long amount = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).longValue();

      // Thời gian tạo & hết hạn
      LocalDateTime now = LocalDateTime.now();
      String createDate = now.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      String expireDate = now.plusMinutes(15).format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

      // Build VNPay params
      Map<String, String> vnpParams = new TreeMap<>();
      vnpParams.put("vnp_Version", vnpayConfig.getVersion());
      vnpParams.put("vnp_Command", vnpayConfig.getCommand());
      vnpParams.put("vnp_TmnCode", vnpayConfig.getTmnCode());
      vnpParams.put("vnp_Amount", String.valueOf(amount));
      vnpParams.put("vnp_CurrCode", vnpayConfig.getCurrCode());
      vnpParams.put("vnp_TxnRef", transactionRef);
      vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + order.getOrderCode());
      vnpParams.put("vnp_OrderType", vnpayConfig.getOrderType());
      vnpParams.put("vnp_Locale", language != null ? language : vnpayConfig.getLocale());
      vnpParams.put("vnp_ReturnUrl", vnpayConfig.getReturnUrl());
      vnpParams.put("vnp_IpAddr", ipAddress != null ? ipAddress : "127.0.0.1");
      vnpParams.put("vnp_CreateDate", createDate);
      vnpParams.put("vnp_ExpireDate", expireDate);

      if (bankCode != null && !bankCode.isBlank()) {
        vnpParams.put("vnp_BankCode", bankCode);
      }

      // Build query string & sign
      String queryString = buildQueryString(vnpParams, true);
      String signData = buildQueryString(vnpParams, false);
      String secureHash = hmacSHA512(vnpayConfig.getHashSecret(), signData);

      String paymentUrl =
          vnpayConfig.getPayUrl() + "?" + queryString + "&vnp_SecureHash=" + secureHash;

      log.info(
          "VNPay payment URL created for order: {}, txnRef: {}",
          order.getOrderCode(),
          transactionRef);

      return PaymentDto.GatewayCreateResult.builder()
          .success(true)
          .paymentUrl(paymentUrl)
          .transactionRef(transactionRef)
          .build();

    } catch (Exception e) {
      log.error("Failed to create VNPay payment URL for order: {}", order.getOrderCode(), e);
      return PaymentDto.GatewayCreateResult.builder()
          .success(false)
          .errorMessage("Không thể tạo URL thanh toán VNPay: " + e.getMessage())
          .build();
    }
  }

  /** Verify callback/return từ VNPay (dùng chung cho cả IPN và Return URL) */
  public PaymentDto.GatewayCallbackResult verifyCallback(Map<String, String> params) {
    try {
      PaymentConfig.VnPay vnpayConfig = paymentConfig.getVnpay();

      String vnpSecureHash = params.get("vnp_SecureHash");
      if (vnpSecureHash == null || vnpSecureHash.isEmpty()) {
        return PaymentDto.GatewayCallbackResult.builder()
            .success(false)
            .verified(false)
            .message("Missing secure hash")
            .build();
      }

      // Bỏ các field liên quan đến hash để verify
      Map<String, String> verifyParams = new TreeMap<>(params);
      verifyParams.remove("vnp_SecureHash");
      verifyParams.remove("vnp_SecureHashType");

      // Tạo lại hash
      String signData = buildQueryString(verifyParams, false);
      String checkHash = hmacSHA512(vnpayConfig.getHashSecret(), signData);

      boolean verified = checkHash.equalsIgnoreCase(vnpSecureHash);

      if (!verified) {
        log.warn("VNPay callback hash verification failed. TxnRef: {}", params.get("vnp_TxnRef"));
        return PaymentDto.GatewayCallbackResult.builder()
            .success(false)
            .verified(false)
            .message("Invalid secure hash")
            .build();
      }

      String responseCode = params.get("vnp_ResponseCode");
      String transactionNo = params.get("vnp_TransactionNo");
      String txnRef = params.get("vnp_TxnRef");
      String amountStr = params.get("vnp_Amount");
      BigDecimal amount =
          amountStr != null
              ? new BigDecimal(amountStr).divide(BigDecimal.valueOf(100))
              : BigDecimal.ZERO;

      // Tách orderCode từ transactionRef
      String orderCode = extractOrderCode(txnRef);

      boolean paymentSuccess = "00".equals(responseCode);
      String message =
          paymentSuccess
              ? "Thanh toán VNPay thành công"
              : "Thanh toán VNPay thất bại (code: " + responseCode + ")";

      log.info(
          "VNPay callback verified. TxnRef: {}, ResponseCode: {}, Success: {}",
          txnRef,
          responseCode,
          paymentSuccess);

      return PaymentDto.GatewayCallbackResult.builder()
          .success(paymentSuccess)
          .verified(true)
          .orderCode(orderCode)
          .transactionRef(txnRef)
          .gatewayTransactionId(transactionNo)
          .amount(amount)
          .responseCode(responseCode)
          .message(message)
          .build();

    } catch (Exception e) {
      log.error("Error verifying VNPay callback", e);
      return PaymentDto.GatewayCallbackResult.builder()
          .success(false)
          .verified(false)
          .message("Lỗi xác thực callback VNPay: " + e.getMessage())
          .build();
    }
  }

  /**
   * Hoàn tiền VNPay (full refund). Docs: merchant_webapi refund command.
   *
   * @param originalTxnRef mã giao dịch thanh toán gốc (transactionRef)
   * @param gatewayTransactionNo vnp_TransactionNo từ giao dịch thành công
   * @param originalTxnDate thời điểm tạo giao dịch gốc
   */
  public PaymentDto.RefundResponse refundTransaction(
      String originalTxnRef,
      String gatewayTransactionNo,
      BigDecimal amount,
      LocalDateTime originalTxnDate,
      String createBy,
      String ipAddress) {
    try {
      PaymentConfig.VnPay vnpayConfig = paymentConfig.getVnpay();
      String requestId = UUID.randomUUID().toString().replace("-", "").substring(0, 32);
      String createDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
      String txnDate =
          originalTxnDate != null
              ? originalTxnDate.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))
              : createDate;
      long amountVnp = amount.multiply(BigDecimal.valueOf(100)).longValue();

      Map<String, String> params = new TreeMap<>();
      params.put("vnp_Amount", String.valueOf(amountVnp));
      params.put("vnp_Command", vnpayConfig.getRefundCommand());
      params.put("vnp_CreateBy", createBy != null ? createBy : "admin");
      params.put("vnp_CreateDate", createDate);
      params.put("vnp_IpAddr", ipAddress != null ? ipAddress : "127.0.0.1");
      params.put("vnp_OrderInfo", "Hoan tien giao dich " + originalTxnRef);
      params.put("vnp_RequestId", requestId);
      params.put("vnp_TransactionDate", txnDate);
      params.put("vnp_TransactionNo", gatewayTransactionNo);
      params.put("vnp_TransactionType", "02");
      params.put("vnp_TxnRef", originalTxnRef);
      params.put("vnp_Version", vnpayConfig.getVersion());

      String signData = buildQueryString(params, false);
      params.put("vnp_SecureHash", hmacSHA512(vnpayConfig.getHashSecret(), signData));

      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      ResponseEntity<Map> response =
          restTemplate.postForEntity(
              vnpayConfig.getRefundApiUrl(),
              new HttpEntity<>(params, headers),
              Map.class);

      Map<?, ?> body = response.getBody();
      String responseCode =
          body != null && body.get("vnp_ResponseCode") != null
              ? String.valueOf(body.get("vnp_ResponseCode"))
              : null;
      boolean success = "00".equals(responseCode);

      log.info(
          "VNPay refund {} for txnRef {}, code={}",
          success ? "OK" : "FAIL",
          originalTxnRef,
          responseCode);

      return PaymentDto.RefundResponse.builder()
          .success(success)
          .refundTransactionRef(requestId)
          .gatewayTransactionId(
              body != null && body.get("vnp_TransactionNo") != null
                  ? String.valueOf(body.get("vnp_TransactionNo"))
                  : gatewayTransactionNo)
          .amount(amount)
          .message(
              success
                  ? "Hoàn tiền VNPay thành công"
                  : "Hoàn tiền VNPay thất bại (code: " + responseCode + ")")
          .build();
    } catch (Exception e) {
      log.error("VNPay refund error for {}", originalTxnRef, e);
      return PaymentDto.RefundResponse.builder()
          .success(false)
          .message("Lỗi hoàn tiền VNPay: " + e.getMessage())
          .build();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helper methods
  // ═══════════════════════════════════════════════════════════════════════════

  private String generateTransactionRef(String orderCode) {
    // Format: orderCode_timestamp (VNPay yêu cầu unique mỗi lần gửi)
    return orderCode + "_" + System.currentTimeMillis();
  }

  private String extractOrderCode(String transactionRef) {
    if (transactionRef == null) return null;
    int idx = transactionRef.lastIndexOf("_");
    return idx > 0 ? transactionRef.substring(0, idx) : transactionRef;
  }

  /**
   * Build query string từ params map (sorted by key).
   *
   * <p>QUAN TRỌNG — Đúng chuẩn VNPay (xem PHP demo chính thức): - CẢ HAI trường hợp đều URLEncoder
   * encode (như PHP urlencode()). - isQueryString = false (tính HMAC hash): encode value, GIỮ
   * NGUYÊN '+' (dấu cách = '+') - isQueryString = true (build URL cho browser): encode value, đổi
   * '+' → '%20' Sai điểm này sẽ dẫn đến lỗi "Sai chữ ký" vì VNPay tính hash với '+', ta tính với
   * '%20'.
   */
  private String buildQueryString(Map<String, String> params, boolean isQueryString) {
    StringBuilder sb = new StringBuilder();
    for (Map.Entry<String, String> entry : params.entrySet()) {
      if (entry.getValue() != null && !entry.getValue().isEmpty()) {
        if (sb.length() > 0) sb.append("&");
        String key = entry.getKey();
        String value = entry.getValue();
        try {
          String encodedKey = URLEncoder.encode(key, StandardCharsets.US_ASCII.toString());
          String encodedValue = URLEncoder.encode(value, StandardCharsets.US_ASCII.toString());
          // URLEncoder mặc định dùng '+' cho dấu cách — đúng chuẩn VNPay khi tính hash.
          // Chỉ đổi '+' → '%20' trên URL thật hiển thị cho browser.
          if (isQueryString) {
            encodedValue = encodedValue.replace("+", "%20");
            encodedKey = encodedKey.replace("+", "%20");
          }
          sb.append(encodedKey).append("=").append(encodedValue);
        } catch (Exception e) {
          sb.append(key).append("=").append(value);
        }
      }
    }
    return sb.toString();
  }

  /** HMAC-SHA512 signing */
  private String hmacSHA512(String key, String data) {
    try {
      Mac mac = Mac.getInstance("HmacSHA512");
      SecretKeySpec secretKeySpec =
          new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
      mac.init(secretKeySpec);
      byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
      StringBuilder sb = new StringBuilder();
      for (byte b : hash) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (Exception e) {
      throw new RuntimeException("Error computing HMAC-SHA512", e);
    }
  }
}
