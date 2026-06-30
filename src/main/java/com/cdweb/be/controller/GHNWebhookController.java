package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.GHNDto;
import com.cdweb.be.entity.Order;
import com.cdweb.be.repository.OrderRepository;
import com.cdweb.be.service.GhnService;
import com.cdweb.be.service.ImeiService;
import com.cdweb.be.service.OrderService;
import com.cdweb.be.service.ReturnInspectionService;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GHN Webhook Controller — Nhận callback từ GHN khi trạng thái vận đơn thay đổi.
 *
 * <p>Đăng ký Webhook URL tại: https://khachhang.ghn.vn/ → Cài đặt → Webhook
 *
 * <p>URL đăng ký: https://your-domain.com/api/shipping/ghn/webhook
 *
 * <h3>Luồng hoạt động:</h3>
 *
 * <ol>
 *   <li>GHN giao hàng thành công → GHN gọi POST /api/shipping/ghn/webhook
 *   <li>Backend nhận webhook, check trạng thái = "delivered"
 *   <li>Tìm đơn hàng theo client_order_code (mã nội bộ ORD-xxx)
 *   <li>Tự động chuyển đơn sang DELIVERED + kích hoạt bảo hành
 * </ol>
 */
@RestController
@RequestMapping("/api/shipping/ghn")
public class GHNWebhookController {

  private static final Logger log = LoggerFactory.getLogger(GHNWebhookController.class);

  @Autowired private OrderRepository orderRepository;

  @Autowired private OrderService orderService;

  @Autowired private ImeiService imeiService;

  @Autowired private ReturnInspectionService returnInspectionService;

  @Autowired private GhnService ghnService;

  /**
   * POST /api/shipping/ghn/webhook GHN gọi endpoint này mỗi khi trạng thái vận đơn thay đổi.
   *
   * <p>Payload mẫu từ GHN:
   *
   * <pre>
   * {
   *   "CODAmount": 500000,
   *   "CODTransferDate": "",
   *   "ClientOrderCode": "ORD-20260315120000001",
   *   "ConvertedWeight": 600,
   *   "Description": "...",
   *   "Fee": { ... },
   *   "Height": 10,
   *   "IsPartialReturn": false,
   *   "Length": 20,
   *   "OrderCode": "GHN_ABC123",
   *   "PartialReturnItems": [],
   *   "PaymentType": 2,
   *   "Reason": "",
   *   "ReasonCode": "",
   *   "ShopID": 123456,
   *   "Status": "delivered",
   *   "Type": "switch_status",
   *   "Warehouse": "",
   *   "Weight": 500,
   *   "Width": 15
   * }
   * </pre>
   */
  @PostMapping("/webhook")
  public ResponseEntity<String> handleGHNWebhook(@RequestBody JsonNode payload) {
    try {
      String ghnStatus = payload.has("Status") ? payload.get("Status").asText() : "";
      String clientOrderCode =
          payload.has("ClientOrderCode") ? payload.get("ClientOrderCode").asText() : "";
      String ghnOrderCode = payload.has("OrderCode") ? payload.get("OrderCode").asText() : "";

      log.info(
          "GHN Webhook received: status={}, clientOrderCode={}, ghnOrderCode={}",
          ghnStatus,
          clientOrderCode,
          ghnOrderCode);

      if (clientOrderCode.isBlank()) {
        log.warn("GHN Webhook: Missing ClientOrderCode. Skipping.");
        return ResponseEntity.ok("OK");
      }

      // Tìm đơn hàng theo mã nội bộ
      Optional<Order> orderOpt = orderRepository.findByOrderCode(clientOrderCode);
      if (orderOpt.isEmpty()) {
        log.warn("GHN Webhook: Order not found for ClientOrderCode: {}", clientOrderCode);
        return ResponseEntity.ok("OK");
      }

      Order order = orderOpt.get();

      // Xử lý theo trạng thái GHN
      switch (ghnStatus.toLowerCase()) {
        case "delivered":
          if (order.getStatus() == Order.OrderStatus.SHIPPING
              || order.getStatus() == Order.OrderStatus.DELIVERED) {
            order.setStatus(Order.OrderStatus.DELIVERED);
            order.setDeliveredAt(LocalDateTime.now());

            if (order.getPaymentMethod() == Order.PaymentMethod.COD) {
              order.setPaymentStatus(Order.PaymentStatus.PAID);
            }

            orderRepository.save(order);
            orderService.activateWarrantyForOrder(order.getId());

            order.setStatus(Order.OrderStatus.COMPLETED);
            orderRepository.save(order);

            log.info(
                "GHN Webhook: Order {} auto-transitioned to COMPLETED (via delivered)",
                clientOrderCode);
          }
          break;

        case "delivery_fail":
          log.warn(
              "GHN Webhook: Delivery failed for order {}. Reason: {}",
              clientOrderCode,
              payload.has("Reason") ? payload.get("Reason").asText() : "N/A");
          // Có thể thêm logic gửi notification cho Admin ở đây
          break;

        case "return", "returned":
          log.warn(
              "GHN Webhook: Order {} returned — chờ kho kiểm định serial.",
              clientOrderCode);
          if (order.getStatus() == Order.OrderStatus.SHIPPING
              || order.getStatus() == Order.OrderStatus.DELIVERED
              || order.getStatus() == Order.OrderStatus.COMPLETED) {
            imeiService.markOrderReturnedForInspection(order.getId());
            returnInspectionService.createSheetsForOrder(order.getId(), "system-ghn");
            order.setStatus(Order.OrderStatus.REFUNDED);
            order.setPickedByUser(null);
            order.setPickedAt(null);
            orderRepository.save(order);
          }
          break;

        default:
          log.info(
              "GHN Webhook: Status '{}' for order {} — no action taken.",
              ghnStatus,
              clientOrderCode);
          break;
      }

    } catch (Exception e) {
      log.error("GHN Webhook processing error: {}", e.getMessage(), e);
    }

    // Phải luôn trả 200 OK cho GHN, nếu không GHN sẽ retry
    return ResponseEntity.ok("OK");
  }

  /**
   * GET /api/shipping/tracking/{trackingCode} Proxy tra cứu trạng thái vận đơn GHN cho Frontend.
   */
  @GetMapping("/tracking/{trackingCode}")
  public ResponseEntity<ApiResponse<GHNDto.TrackingResponse>> getTracking(
      @PathVariable("trackingCode") String trackingCode) {
    GHNDto.TrackingResponse tracking = ghnService.getTrackingInfo(trackingCode);
    return ResponseEntity.ok(ApiResponse.success("Thông tin vận đơn", tracking));
  }
}
