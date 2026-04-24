package com.cdweb.be.controller;

import com.cdweb.be.config.PaymentConfig;
import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.PaymentDto;
import com.cdweb.be.service.payment.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

/**
 * Payment Controller — Xử lý thanh toán online (VNPay, Momo, ZaloPay).
 *
 * <h3>Endpoints:</h3>
 *
 * <ul>
 *   <li>POST /api/payment/create — Tạo URL thanh toán (retry)
 *   <li>GET /api/payment/status/{code} — Kiểm tra trạng thái thanh toán
 *   <li>GET /api/payment/history/{code} — Lịch sử giao dịch
 *   <li>GET /api/payment/vnpay/ipn — VNPay IPN callback (public)
 *   <li>GET /api/payment/vnpay/return — VNPay redirect return (public)
 *   <li>POST /api/payment/momo/ipn — Momo IPN callback (public)
 *   <li>GET /api/payment/momo/return — Momo redirect return (public)
 *   <li>POST /api/payment/zalopay/callback — ZaloPay callback (public)
 *   <li>GET /api/payment/zalopay/return — ZaloPay redirect return (public)
 * </ul>
 */
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

  private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

  @Autowired private PaymentService paymentService;

  @Autowired private PaymentConfig paymentConfig;

  // ═══════════════════════════════════════════════════════════════════════════
  // USER ENDPOINTS (cần đăng nhập)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * POST /api/payment/create — Tạo URL thanh toán cho đơn hàng đã tồn tại (retry) Dùng khi user
   * chưa thanh toán hoặc thanh toán thất bại, muốn thử lại.
   */
  @PostMapping("/create")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<PaymentDto.PaymentUrlResponse>> createPayment(
      @RequestBody PaymentDto.CreatePaymentRequest request, HttpServletRequest httpRequest) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    String ipAddress = getClientIpAddress(httpRequest);

    PaymentDto.PaymentUrlResponse response =
        paymentService.retryPayment(
            username,
            request.getOrderCode(),
            request.getBankCode(),
            request.getLanguage(),
            ipAddress);

    return ResponseEntity.ok(ApiResponse.success("Tạo URL thanh toán thành công", response));
  }

  /** GET /api/payment/status/{orderCode} — Kiểm tra trạng thái thanh toán */
  @GetMapping("/status/{orderCode}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<PaymentDto.PaymentStatusResponse>> getPaymentStatus(
      @PathVariable String orderCode) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    PaymentDto.PaymentStatusResponse response =
        paymentService.getPaymentStatus(username, orderCode);
    return ResponseEntity.ok(ApiResponse.success("Payment status retrieved", response));
  }

  /** GET /api/payment/history/{orderCode} — Lịch sử giao dịch thanh toán */
  @GetMapping("/history/{orderCode}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<List<PaymentDto.PaymentTransactionResponse>>>
      getTransactionHistory(@PathVariable String orderCode) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    List<PaymentDto.PaymentTransactionResponse> transactions =
        paymentService.getTransactionHistory(username, orderCode);
    return ResponseEntity.ok(ApiResponse.success("Transaction history retrieved", transactions));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VNPAY CALLBACKS (public — gateway gọi trực tiếp)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * GET /api/payment/vnpay/ipn — VNPay IPN (Instant Payment Notification) VNPay gọi endpoint này để
   * thông báo kết quả thanh toán (server-to-server).
   */
  @GetMapping("/vnpay/ipn")
  public ResponseEntity<String> vnpayIPN(@RequestParam Map<String, String> params) {
    log.info("VNPay IPN received: {}", params);
    String response = paymentService.handleVNPayIPN(params);
    return ResponseEntity.ok(response);
  }

  /**
   * GET /api/payment/vnpay/return — VNPay Return URL User được redirect về URL này sau khi thanh
   * toán trên VNPay. Redirect user tới frontend success/fail page.
   */
  @GetMapping("/vnpay/return")
  public void vnpayReturn(@RequestParam Map<String, String> params, HttpServletResponse response)
      throws IOException {
    log.info("VNPay return received: {}", params);
    PaymentDto.PaymentReturnResponse result = paymentService.handleVNPayReturn(params);
    redirectToFrontend(response, result);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // MOMO CALLBACKS (public)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * POST /api/payment/momo/ipn — Momo IPN callback Momo gọi endpoint này để thông báo kết quả thanh
   * toán.
   */
  @PostMapping("/momo/ipn")
  public ResponseEntity<String> momoIPN(@RequestParam Map<String, String> params) {
    log.info("Momo IPN received: {}", params);
    String response = paymentService.handleMomoIPN(params);
    return ResponseEntity.ok(response);
  }

  /** GET /api/payment/momo/return — Momo Return URL User redirect về sau khi thanh toán. */
  @GetMapping("/momo/return")
  public void momoReturn(@RequestParam Map<String, String> params, HttpServletResponse response)
      throws IOException {
    log.info("Momo return received: {}", params);
    PaymentDto.PaymentReturnResponse result = paymentService.handleMomoReturn(params);
    redirectToFrontend(response, result);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ZALOPAY CALLBACKS (public)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * POST /api/payment/zalopay/callback — ZaloPay Callback ZaloPay gọi endpoint này khi có kết quả
   * thanh toán. Body: { "data": "...", "mac": "...", "type": 1 }
   */
  @PostMapping("/zalopay/callback")
  public ResponseEntity<String> zalopayCallback(@RequestBody Map<String, Object> body) {
    log.info("ZaloPay callback received");
    String data = (String) body.get("data");
    String mac = (String) body.get("mac");
    String response = paymentService.handleZaloPayCallback(data, mac);
    return ResponseEntity.ok(response);
  }

  /** GET /api/payment/zalopay/return — ZaloPay Return URL User redirect về sau khi thanh toán. */
  @GetMapping("/zalopay/return")
  public void zalopayReturn(@RequestParam Map<String, String> params, HttpServletResponse response)
      throws IOException {
    log.info("ZaloPay return received: {}", params);
    PaymentDto.PaymentReturnResponse result = paymentService.handleZaloPayReturn(params);
    redirectToFrontend(response, result);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPER METHODS
  // ═══════════════════════════════════════════════════════════════════════════

  /** Redirect user tới frontend page (success/fail) sau khi payment gateway trả về */
  private void redirectToFrontend(
      HttpServletResponse response, PaymentDto.PaymentReturnResponse result) throws IOException {
    String redirectUrl;
    if (result.isSuccess()) {
      redirectUrl =
          paymentConfig.getFrontendSuccessUrl()
              + "?orderCode="
              + (result.getOrderCode() != null ? result.getOrderCode() : "")
              + "&status=success"
              + "&amount="
              + (result.getAmount() != null ? result.getAmount() : "");
    } else {
      redirectUrl =
          paymentConfig.getFrontendFailUrl()
              + "?orderCode="
              + (result.getOrderCode() != null ? result.getOrderCode() : "")
              + "&status=fail"
              + "&message="
              + (result.getMessage() != null ? result.getMessage() : "");
    }
    response.sendRedirect(redirectUrl);
  }

  /** Lấy IP address thực của client (xử lý proxy/load balancer) */
  private String getClientIpAddress(HttpServletRequest request) {
    String[] headerNames = {
      "X-Forwarded-For",
      "Proxy-Client-IP",
      "WL-Proxy-Client-IP",
      "HTTP_X_FORWARDED_FOR",
      "HTTP_X_FORWARDED",
      "HTTP_X_CLUSTER_CLIENT_IP",
      "HTTP_CLIENT_IP",
      "HTTP_FORWARDED_FOR",
      "HTTP_FORWARDED",
      "HTTP_VIA",
      "REMOTE_ADDR"
    };

    for (String header : headerNames) {
      String ip = request.getHeader(header);
      if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
        // X-Forwarded-For có thể chứa nhiều IP, lấy IP đầu tiên
        return ip.contains(",") ? ip.split(",")[0].trim() : ip;
      }
    }

    return request.getRemoteAddr();
  }
}
