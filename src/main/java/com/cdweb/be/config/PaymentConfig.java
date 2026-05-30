package com.cdweb.be.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/** Cấu hình cho các Payment Gateway. Đọc từ application.properties với prefix "payment" */
@Configuration
@ConfigurationProperties(prefix = "payment")
@Data
public class PaymentConfig {

  /** URL frontend để redirect user sau khi thanh toán */
  private String frontendSuccessUrl = "http://localhost:5173/payment/success";

  private String frontendFailUrl = "http://localhost:5173/payment/fail";

  /** Base URL của server backend (dùng cho callback/return URL) */
  private String backendBaseUrl = "http://localhost:8080";

  // ═══════════════════════════════════════════════════════════════════════════
  // VNPay Configuration
  // ═══════════════════════════════════════════════════════════════════════════
  private VnPay vnpay = new VnPay();

  @Data
  public static class VnPay {
    private String tmnCode = "YOUR_TMN_CODE";
    private String hashSecret = "YOUR_HASH_SECRET";
    private String payUrl = "https://sandbox.vnpayment.vn/paymentv2/vpcpay.html";
    private String apiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
    private String returnUrl = "http://localhost:8080/api/payment/vnpay/return";
    private String version = "2.1.0";
    private String command = "pay";
    private String orderType = "other";
    private String locale = "vn";
    private String currCode = "VND";
    private String refundCommand = "refund";
    /** API hoàn tiền (cùng host merchant_webapi) */
    private String refundApiUrl = "https://sandbox.vnpayment.vn/merchant_webapi/api/transaction";
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Momo Configuration
  // ═══════════════════════════════════════════════════════════════════════════
  private Momo momo = new Momo();

  @Data
  public static class Momo {
    private String partnerCode = "YOUR_PARTNER_CODE";
    private String accessKey = "YOUR_ACCESS_KEY";
    private String secretKey = "YOUR_SECRET_KEY";
    private String apiUrl = "https://test-payment.momo.vn/v2/gateway/api/create";
    private String returnUrl = "http://localhost:8080/api/payment/momo/return";
    private String ipnUrl = "http://localhost:8080/api/payment/momo/ipn";
    private String requestType = "payWithMethod";
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ZaloPay Configuration
  // ═══════════════════════════════════════════════════════════════════════════
  private ZaloPay zalopay = new ZaloPay();

  @Data
  public static class ZaloPay {
    private String appId = "YOUR_APP_ID";
    private String key1 = "YOUR_KEY_1";
    private String key2 = "YOUR_KEY_2";
    private String createOrderUrl = "https://sb-openapi.zalopay.vn/v2/create";
    private String queryOrderUrl = "https://sb-openapi.zalopay.vn/v2/query";
    private String refundUrl = "https://sb-openapi.zalopay.vn/v2/refund";
    private String callbackUrl = "http://localhost:8080/api/payment/zalopay/callback";
    private String redirectUrl = "http://localhost:8080/api/payment/zalopay/return";
  }
}
