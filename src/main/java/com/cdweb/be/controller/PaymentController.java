package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.PaymentDto;
import com.cdweb.be.service.payment.PaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller thống nhất cho VNPay, Momo, ZaloPay — tạo URL, callback/IPN, kiểm tra trạng thái.
 */
@RestController
@RequestMapping("/api/payment")
@Tag(name = "Thanh toán", description = "Payment Gateway — VNPay, Momo, ZaloPay")
public class PaymentController {

  @Autowired private PaymentService paymentService;

  @Value("${app.frontend-url:http://localhost:3000}")
  private String frontendUrl;

  // ═══════════════════════════════════════════════════════════════════════════
  // Tạo / thử lại thanh toán (customer)
  // ═══════════════════════════════════════════════════════════════════════════

  @PostMapping("/create")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Tạo URL thanh toán online cho đơn hàng")
  public ResponseEntity<ApiResponse<PaymentDto.PaymentUrlResponse>> createPayment(
      @RequestBody PaymentDto.CreatePaymentRequest request,
      HttpServletRequest httpRequest,
      Principal principal) {
    String ip = getClientIp(httpRequest);
    PaymentDto.PaymentUrlResponse response =
        paymentService.retryPayment(
            principal.getName(),
            request.getOrderCode(),
            request.getBankCode(),
            request.getLanguage(),
            ip);
    return ResponseEntity.ok(ApiResponse.success("Tạo URL thanh toán thành công", response));
  }

  @PostMapping("/retry")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Thanh toán lại đơn hàng (alias create)")
  public ResponseEntity<ApiResponse<PaymentDto.PaymentUrlResponse>> retryPayment(
      @RequestBody PaymentDto.CreatePaymentRequest request,
      HttpServletRequest httpRequest,
      Principal principal) {
    return createPayment(request, httpRequest, principal);
  }

  @GetMapping("/status/{orderCode}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Kiểm tra trạng thái thanh toán đơn hàng")
  public ResponseEntity<ApiResponse<PaymentDto.PaymentStatusResponse>> getPaymentStatus(
      @PathVariable String orderCode, Principal principal) {
    PaymentDto.PaymentStatusResponse response =
        paymentService.getPaymentStatus(principal.getName(), orderCode);
    return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái thanh toán thành công", response));
  }

  @GetMapping("/history/{orderCode}")
  @PreAuthorize("isAuthenticated()")
  @Operation(summary = "Lịch sử giao dịch thanh toán của đơn")
  public ResponseEntity<ApiResponse<List<PaymentDto.PaymentTransactionResponse>>>
      getTransactionHistory(@PathVariable String orderCode, Principal principal) {
    List<PaymentDto.PaymentTransactionResponse> history =
        paymentService.getTransactionHistory(principal.getName(), orderCode);
    return ResponseEntity.ok(ApiResponse.success("Lấy lịch sử giao dịch thành công", history));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // VNPay callbacks
  // ═══════════════════════════════════════════════════════════════════════════

  @GetMapping("/vnpay/ipn")
  @Operation(summary = "VNPay IPN (server-to-server)")
  public ResponseEntity<String> vnpayIpn(@RequestParam Map<String, String> params) {
    return ResponseEntity.ok(paymentService.handleVNPayIPN(params));
  }

  @GetMapping("/vnpay/return")
  @Operation(summary = "VNPay return URL (redirect user)")
  public void vnpayReturn(
      @RequestParam Map<String, String> params, HttpServletResponse response) throws Exception {
    PaymentDto.PaymentReturnResponse result = paymentService.handleVNPayReturn(params);
    response.sendRedirect(buildCheckoutResultUrl(result));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Momo callbacks
  // ═══════════════════════════════════════════════════════════════════════════

  @PostMapping("/momo/ipn")
  @Operation(summary = "Momo IPN")
  public ResponseEntity<String> momoIpn(@RequestBody Map<String, String> params) {
    return ResponseEntity.ok(paymentService.handleMomoIPN(params));
  }

  @GetMapping("/momo/return")
  @Operation(summary = "Momo return URL")
  public void momoReturn(
      @RequestParam Map<String, String> params, HttpServletResponse response) throws Exception {
    PaymentDto.PaymentReturnResponse result = paymentService.handleMomoReturn(params);
    response.sendRedirect(buildCheckoutResultUrl(result));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ZaloPay callbacks
  // ═══════════════════════════════════════════════════════════════════════════

  @PostMapping("/zalopay/callback")
  @Operation(summary = "ZaloPay server callback")
  public ResponseEntity<String> zaloPayCallback(@RequestBody Map<String, String> body) {
    String data = body.get("data");
    String mac = body.get("mac");
    return ResponseEntity.ok(paymentService.handleZaloPayCallback(data, mac));
  }

  @GetMapping("/zalopay/return")
  @Operation(summary = "ZaloPay return URL")
  public void zaloPayReturn(
      @RequestParam Map<String, String> params, HttpServletResponse response) throws Exception {
    PaymentDto.PaymentReturnResponse result = paymentService.handleZaloPayReturn(params);
    response.sendRedirect(buildCheckoutResultUrl(result));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // Helpers
  // ═══════════════════════════════════════════════════════════════════════════

  private String buildCheckoutResultUrl(PaymentDto.PaymentReturnResponse result) {
    String base = frontendUrl.replaceAll("/$", "") + "/checkout/result";
    StringBuilder qs = new StringBuilder("?");
    qs.append("success=").append(result.isSuccess());
    if (result.getOrderCode() != null) {
      qs.append("&orderCode=").append(urlEncode(result.getOrderCode()));
    }
    if (result.getPaymentStatus() != null) {
      qs.append("&paymentStatus=").append(urlEncode(result.getPaymentStatus()));
    }
    if (result.getTransactionRef() != null) {
      qs.append("&transactionRef=").append(urlEncode(result.getTransactionRef()));
    }
    if (result.getMessage() != null) {
      qs.append("&message=").append(urlEncode(result.getMessage()));
    }
    return base + qs;
  }

  private String urlEncode(String value) {
    return URLEncoder.encode(value, StandardCharsets.UTF_8);
  }

  private String getClientIp(HttpServletRequest request) {
    String ip = request.getHeader("X-Forwarded-For");
    if (ip == null || ip.isEmpty()) {
      ip = request.getRemoteAddr();
    }
    if (ip != null && ip.contains(",")) {
      ip = ip.split(",")[0].trim();
    }
    return ip != null ? ip : "127.0.0.1";
  }
}
