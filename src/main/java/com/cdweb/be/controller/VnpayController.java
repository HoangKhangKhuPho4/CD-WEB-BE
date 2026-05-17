package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.entity.Order;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.repository.OrderRepository;
import com.cdweb.be.service.VnpayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payment/vnpay")
@Tag(name = "Thanh toán VNPAY", description = "Tích hợp cổng thanh toán VNPAY")
public class VnpayController {

    @Autowired private VnpayService vnpayService;
    @Autowired private OrderRepository orderRepository;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    @PostMapping("/create")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Tạo URL thanh toán VNPAY")
    public ResponseEntity<ApiResponse<Map<String, String>>> createPayment(
            @RequestBody Map<String, Object> body,
            HttpServletRequest request,
            Principal principal) {

        Integer orderId = (Integer) body.get("orderId");
        Long amount = ((Number) body.get("amount")).longValue();

        // Xác nhận đơn hàng thuộc về user
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Đơn hàng không tồn tại"));

        if (!order.getUser().getUsername().equals(principal.getName())) {
            throw new BadRequestException("Không có quyền thanh toán đơn hàng này");
        }

        String ipAddr = getClientIp(request);
        String orderInfo = "Thanh toan don hang #" + orderId;
        String paymentUrl = vnpayService.createPaymentUrl(orderId, amount, orderInfo, ipAddr);

        Map<String, String> result = new HashMap<>();
        result.put("paymentUrl", paymentUrl);

        return ResponseEntity.ok(ApiResponse.success("Tạo URL thanh toán thành công", result));
    }

    /**
     * IPN (Instant Payment Notification) – VNPAY gọi về server
     * Cần public URL, không cần auth
     */
    @GetMapping("/ipn")
    @Operation(summary = "VNPAY IPN Callback", description = "Endpoint nhận thông báo thanh toán từ VNPAY server")
    public ResponseEntity<Map<String, String>> ipnCallback(@RequestParam Map<String, String> params) {
        Map<String, String> response = new HashMap<>();

        if (!vnpayService.verifyIpn(params)) {
            response.put("RspCode", "97");
            response.put("Message", "Invalid signature");
            return ResponseEntity.ok(response);
        }

        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");

        try {
            Integer orderId = Integer.parseInt(txnRef);
            Order order = orderRepository.findById(orderId).orElse(null);

            if (order == null) {
                response.put("RspCode", "01");
                response.put("Message", "Order not found");
                return ResponseEntity.ok(response);
            }

            // 1. Sửa đoạn kiểm tra (Dùng toán tử == cho Enum thay vì .equals chuỗi)
            if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                response.put("RspCode", "02");
                response.put("Message", "Order already confirmed");
                return ResponseEntity.ok(response);
            }

            if ("00".equals(responseCode)) {
                // 2. Sửa gán PaymentStatus
                order.setPaymentStatus(Order.PaymentStatus.PAID);

                // 3. Sửa kiểm tra và gán OrderStatus (Giả sử enum của bạn tên là PENDING và CONFIRMED)
                if (order.getStatus() == Order.OrderStatus.PENDING) {
                    order.setStatus(Order.OrderStatus.CONFIRMED);
                }
            } else {
                // 4. Sửa gán PaymentStatus
                order.setPaymentStatus(Order.PaymentStatus.FAILED);
            }

            orderRepository.save(order);
            response.put("RspCode", "00");
            response.put("Message", "Confirm success");

        } catch (NumberFormatException e) {
            response.put("RspCode", "01");
            response.put("Message", "Invalid order reference");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Return URL – trình duyệt user quay về sau thanh toán
     */
    @GetMapping("/return")
    @Operation(summary = "VNPAY Return URL")
    public void returnUrl(@RequestParam Map<String, String> params, jakarta.servlet.http.HttpServletResponse response) throws Exception {
        boolean valid = vnpayService.verifyReturnUrl(params);
        String responseCode = params.get("vnp_ResponseCode");
        String txnRef = params.get("vnp_TxnRef");

        String redirectUrl;
        if (valid && "00".equals(responseCode)) {
            redirectUrl = frontendUrl + "/checkout/result?vnp_ResponseCode=00&vnp_TxnRef=" + txnRef;
        } else {
            redirectUrl = frontendUrl + "/checkout/result?vnp_ResponseCode=" + responseCode;
        }

        response.sendRedirect(redirectUrl);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        if (ip != null && ip.contains(",")) ip = ip.split(",")[0].trim();
        return ip != null ? ip : "127.0.0.1";
    }
}