package com.cdweb.be.service.payment;

import com.cdweb.be.config.PaymentConfig;
import com.cdweb.be.dto.PaymentDto;
import com.cdweb.be.entity.Order;
import com.cdweb.be.entity.PaymentTransaction;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.OrderRepository;
import com.cdweb.be.repository.PaymentTransactionRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service trung tâm điều phối Payment Gateway. Quản lý flow: tạo payment → lưu transaction → xử lý
 * callback → cập nhật order.
 */
@Service
@Transactional
public class PaymentService {

  private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

  @Autowired private VNPayService vnPayService;

  @Autowired private MomoService momoService;

  @Autowired private ZaloPayService zaloPayService;

  @Autowired private OrderRepository orderRepository;

  @Autowired private PaymentTransactionRepository transactionRepository;

  @Autowired private PaymentConfig paymentConfig;

  // ═══════════════════════════════════════════════════════════════════════════
  // 1. TẠO URL THANH TOÁN
  // ═══════════════════════════════════════════════════════════════════════════

  /** Tạo URL thanh toán cho đơn hàng. Gọi từ checkout flow hoặc khi user muốn thanh toán lại. */
  @Transactional(noRollbackFor = BadRequestException.class)
  public PaymentDto.PaymentUrlResponse createPayment(
      Order order, String ipAddress, String bankCode, String language) {
    // Validate: chỉ tạo payment cho đơn chưa thanh toán
    if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
      throw new BadRequestException("Đơn hàng đã được thanh toán");
    }
    if (order.getStatus() == Order.OrderStatus.CANCELLED) {
      throw new BadRequestException("Đơn hàng đã bị hủy, không thể thanh toán");
    }

    // Hủy các transaction pending cũ
    cancelPendingTransactions(order.getOrderCode());

    // Gọi gateway tạo URL
    PaymentDto.GatewayCreateResult result;
    switch (order.getPaymentMethod()) {
      case VNPAY:
        result = vnPayService.createPaymentUrl(order, ipAddress, bankCode, language);
        break;
      case MOMO:
        result = momoService.createPaymentUrl(order, ipAddress);
        break;
      case ZALOPAY:
        result = zaloPayService.createPaymentUrl(order, ipAddress);
        break;
      default:
        throw new BadRequestException(
            "Phương thức " + order.getPaymentMethod() + " không hỗ trợ thanh toán online");
    }

    if (!result.isSuccess()) {
      throw new BadRequestException(result.getErrorMessage());
    }

    // Lưu transaction mới
    PaymentTransaction transaction =
        PaymentTransaction.builder()
            .order(order)
            .orderCode(order.getOrderCode())
            .transactionRef(result.getTransactionRef())
            .paymentMethod(order.getPaymentMethod())
            .status(PaymentTransaction.TransactionStatus.PENDING)
            .amount(order.getTotalAmount())
            .paymentUrl(result.getPaymentUrl())
            .ipAddress(ipAddress)
            .build();
    transactionRepository.save(transaction);

    // Cập nhật order
    order.setTransactionRef(result.getTransactionRef());
    order.setPaymentUrl(result.getPaymentUrl());
    orderRepository.save(order);

    log.info(
        "Payment created for order: {}, method: {}, txnRef: {}",
        order.getOrderCode(),
        order.getPaymentMethod(),
        result.getTransactionRef());

    return PaymentDto.PaymentUrlResponse.builder()
        .orderCode(order.getOrderCode())
        .paymentMethod(order.getPaymentMethod().name())
        .paymentUrl(result.getPaymentUrl())
        .transactionRef(result.getTransactionRef())
        .amount(order.getTotalAmount())
        .build();
  }

  /** Tạo URL thanh toán cho đơn đã tồn tại (retry payment) */
  public PaymentDto.PaymentUrlResponse retryPayment(
      String username, String orderCode, String bankCode, String language, String ipAddress) {
    Order order =
        orderRepository
            .findByOrderCode(orderCode)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", orderCode));

    // Kiểm tra quyền
    if (!order.getUser().getUsername().equals(username)) {
      throw new ResourceNotFoundException("Order", "orderCode", orderCode);
    }

    // Chỉ cho phép thanh toán online
    if (order.getPaymentMethod() == Order.PaymentMethod.COD
        || order.getPaymentMethod() == Order.PaymentMethod.BANK_TRANSFER) {
      throw new BadRequestException(
          "Đơn hàng sử dụng phương thức "
              + order.getPaymentMethod()
              + ", không cần thanh toán online");
    }

    return createPayment(order, ipAddress, bankCode, language);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 2. XỬ LÝ CALLBACK / IPN
  // ═══════════════════════════════════════════════════════════════════════════

  /** Xử lý VNPay IPN callback */
  public String handleVNPayIPN(Map<String, String> params) {
    PaymentDto.GatewayCallbackResult result = vnPayService.verifyCallback(params);
    return processCallbackResult(result, params.toString());
  }

  /** Xử lý VNPay return URL (user redirect về) */
  public PaymentDto.PaymentReturnResponse handleVNPayReturn(Map<String, String> params) {
    PaymentDto.GatewayCallbackResult result = vnPayService.verifyCallback(params);
    processCallbackResult(result, params.toString());

    return PaymentDto.PaymentReturnResponse.builder()
        .success(result.isSuccess())
        .orderCode(result.getOrderCode())
        .message(result.getMessage())
        .paymentStatus(result.isSuccess() ? "PAID" : "FAILED")
        .transactionRef(result.getTransactionRef())
        .amount(result.getAmount())
        .build();
  }

  /** Xử lý Momo IPN callback */
  public String handleMomoIPN(Map<String, String> params) {
    PaymentDto.GatewayCallbackResult result = momoService.verifyCallback(params);
    return processCallbackResult(result, params.toString());
  }

  /** Xử lý Momo return URL (user redirect về) */
  public PaymentDto.PaymentReturnResponse handleMomoReturn(Map<String, String> params) {
    PaymentDto.GatewayCallbackResult result = momoService.verifyCallback(params);
    processCallbackResult(result, params.toString());

    return PaymentDto.PaymentReturnResponse.builder()
        .success(result.isSuccess())
        .orderCode(result.getOrderCode())
        .message(result.getMessage())
        .paymentStatus(result.isSuccess() ? "PAID" : "FAILED")
        .transactionRef(result.getTransactionRef())
        .amount(result.getAmount())
        .build();
  }

  /** Xử lý ZaloPay callback */
  public String handleZaloPayCallback(String data, String mac) {
    PaymentDto.GatewayCallbackResult result = zaloPayService.verifyCallback(data, mac);
    return processCallbackResult(result, "data=" + data + ", mac=" + mac);
  }

  /** Xử lý ZaloPay redirect URL (user redirect về) */
  public PaymentDto.PaymentReturnResponse handleZaloPayReturn(Map<String, String> params) {
    String appTransId = params.get("apptransid");
    String status = params.get("status");

    // ZaloPay return URL không có đầy đủ thông tin để verify
    // Cần query trạng thái từ transaction đã lưu
    if (appTransId != null) {
      PaymentTransaction transaction =
          transactionRepository.findByTransactionRef(appTransId).orElse(null);
      if (transaction != null) {
        boolean success =
            transaction.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS
                || "1".equals(status);
        return PaymentDto.PaymentReturnResponse.builder()
            .success(success)
            .orderCode(transaction.getOrderCode())
            .message(success ? "Thanh toán ZaloPay thành công" : "Đang xử lý thanh toán")
            .paymentStatus(success ? "PAID" : transaction.getStatus().name())
            .transactionRef(appTransId)
            .amount(transaction.getAmount())
            .build();
      }
    }

    return PaymentDto.PaymentReturnResponse.builder()
        .success(false)
        .message("Không tìm thấy thông tin giao dịch")
        .build();
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 3. KIỂM TRA TRẠNG THÁI THANH TOÁN
  // ═══════════════════════════════════════════════════════════════════════════

  /** Kiểm tra trạng thái thanh toán của đơn hàng */
  @Transactional(readOnly = true)
  public PaymentDto.PaymentStatusResponse getPaymentStatus(String username, String orderCode) {
    Order order =
        orderRepository
            .findByOrderCode(orderCode)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", orderCode));

    if (!order.getUser().getUsername().equals(username)) {
      throw new ResourceNotFoundException("Order", "orderCode", orderCode);
    }

    PaymentTransaction latestTxn =
        transactionRepository.findByOrderCodeOrderByCreatedAtDesc(orderCode).stream()
            .findFirst()
            .orElse(null);

    return PaymentDto.PaymentStatusResponse.builder()
        .orderCode(orderCode)
        .paymentMethod(order.getPaymentMethod().name())
        .paymentStatus(order.getPaymentStatus().name())
        .transactionRef(order.getTransactionRef())
        .amount(order.getTotalAmount())
        .paidAt(order.getPaidAt())
        .gatewayTransactionId(latestTxn != null ? latestTxn.getGatewayTransactionId() : null)
        .message(getPaymentStatusMessage(order.getPaymentStatus()))
        .build();
  }

  /**
   * Admin hoàn tiền VNPay cho đơn đã thanh toán online. Cập nhật trạng thái đơn REFUNDED khi gateway
   * trả về thành công.
   */
  public PaymentDto.RefundResponse refundOrderVnpay(Integer orderId, String adminUsername) {
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));

    if (order.getPaymentMethod() != Order.PaymentMethod.VNPAY) {
      throw new BadRequestException("Đơn không thanh toán VNPay");
    }
    if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
      throw new BadRequestException("Đơn chưa thanh toán hoặc đã hoàn tiền");
    }

    PaymentTransaction paidTxn =
        transactionRepository.findByOrderCodeOrderByCreatedAtDesc(order.getOrderCode()).stream()
            .filter(t -> t.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS)
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Không tìm thấy giao dịch VNPay thành công"));

    if (paidTxn.getGatewayTransactionId() == null
        || paidTxn.getGatewayTransactionId().isBlank()) {
      throw new BadRequestException("Thiếu mã giao dịch VNPay (TransactionNo)");
    }

    PaymentDto.RefundResponse result =
        vnPayService.refundTransaction(
            paidTxn.getTransactionRef(),
            paidTxn.getGatewayTransactionId(),
            order.getTotalAmount(),
            paidTxn.getCreatedAt(),
            adminUsername,
            paidTxn.getIpAddress());

    result.setOrderCode(order.getOrderCode());

    if (!result.isSuccess()) {
      return result;
    }

    paidTxn.setStatus(PaymentTransaction.TransactionStatus.REFUNDED);
    paidTxn.setResponseMessage(result.getMessage());
    transactionRepository.save(paidTxn);

    PaymentTransaction refundTxn =
        PaymentTransaction.builder()
            .order(order)
            .orderCode(order.getOrderCode())
            .transactionRef(
                result.getRefundTransactionRef() != null
                    ? result.getRefundTransactionRef()
                    : "REFUND_" + order.getOrderCode())
            .gatewayTransactionId(result.getGatewayTransactionId())
            .paymentMethod(Order.PaymentMethod.VNPAY)
            .status(PaymentTransaction.TransactionStatus.REFUNDED)
            .amount(order.getTotalAmount())
            .responseMessage(result.getMessage())
            .build();
    transactionRepository.save(refundTxn);

    order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
    if (order.getStatus() != Order.OrderStatus.CANCELLED
        && order.getStatus() != Order.OrderStatus.REFUNDED) {
      order.setStatus(Order.OrderStatus.REFUNDED);
    }
    orderRepository.save(order);

    log.info("Order {} refunded via VNPay by {}", order.getOrderCode(), adminUsername);
    return result;
  }

  /** Lịch sử giao dịch thanh toán của đơn hàng */
  @Transactional(readOnly = true)
  public List<PaymentDto.PaymentTransactionResponse> getTransactionHistory(
      String username, String orderCode) {
    Order order =
        orderRepository
            .findByOrderCode(orderCode)
            .orElseThrow(() -> new ResourceNotFoundException("Order", "orderCode", orderCode));

    if (!order.getUser().getUsername().equals(username)) {
      throw new ResourceNotFoundException("Order", "orderCode", orderCode);
    }

    return transactionRepository.findByOrderCodeOrderByCreatedAtDesc(orderCode).stream()
        .map(this::mapToTransactionResponse)
        .collect(Collectors.toList());
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PRIVATE HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Xử lý kết quả callback từ gateway chung. Cập nhật transaction + order.
   *
   * @return Response string cho gateway (IPN response)
   */
  private String processCallbackResult(PaymentDto.GatewayCallbackResult result, String rawData) {
    if (!result.isVerified()) {
      log.warn("Callback verification failed: {}", result.getMessage());
      return buildIPNResponse(false, "INVALID_SIGNATURE");
    }

    // Tìm transaction
    PaymentTransaction transaction = null;
    if (result.getTransactionRef() != null) {
      transaction =
          transactionRepository.findByTransactionRef(result.getTransactionRef()).orElse(null);
    }

    if (transaction == null) {
      log.warn("Transaction not found for ref: {}", result.getTransactionRef());
      return buildIPNResponse(false, "TRANSACTION_NOT_FOUND");
    }

    // Tránh xử lý trùng lặp
    if (transaction.getStatus() == PaymentTransaction.TransactionStatus.SUCCESS) {
      log.info("Transaction already processed: {}", result.getTransactionRef());
      return buildIPNResponse(true, "ALREADY_PROCESSED");
    }

    // Cập nhật transaction
    transaction.setCallbackData(rawData);
    transaction.setGatewayTransactionId(result.getGatewayTransactionId());
    transaction.setResponseCode(result.getResponseCode());
    transaction.setResponseMessage(result.getMessage());

    if (result.isSuccess()) {
      transaction.setStatus(PaymentTransaction.TransactionStatus.SUCCESS);
    } else {
      transaction.setStatus(PaymentTransaction.TransactionStatus.FAILED);
    }
    transactionRepository.save(transaction);

    // Cập nhật Order
    Order order = transaction.getOrder();
    if (result.isSuccess()) {
      order.setPaymentStatus(Order.PaymentStatus.PAID);
      order.setPaidAt(LocalDateTime.now());
      order.setTransactionRef(result.getTransactionRef());
      log.info(
          "Order {} payment SUCCESS, gateway txnId: {}",
          order.getOrderCode(),
          result.getGatewayTransactionId());
    } else {
      order.setPaymentStatus(Order.PaymentStatus.FAILED);
      log.info("Order {} payment FAILED, code: {}", order.getOrderCode(), result.getResponseCode());
    }
    orderRepository.save(order);

    return buildIPNResponse(true, result.isSuccess() ? "SUCCESS" : "PAYMENT_FAILED");
  }

  /** Hủy các transaction PENDING cũ khi user retry payment */
  private void cancelPendingTransactions(String orderCode) {
    List<PaymentTransaction> pendingTxns =
        transactionRepository.findByOrderCodeAndStatus(
            orderCode, PaymentTransaction.TransactionStatus.PENDING);
    for (PaymentTransaction txn : pendingTxns) {
      txn.setStatus(PaymentTransaction.TransactionStatus.CANCELLED);
      txn.setResponseMessage("Cancelled due to new payment attempt");
    }
    if (!pendingTxns.isEmpty()) {
      transactionRepository.saveAll(pendingTxns);
      log.info("Cancelled {} pending transactions for order: {}", pendingTxns.size(), orderCode);
    }
  }

  /**
   * Build IPN response cho các gateway. VNPay expects: {"RspCode":"00","Message":"success"} or
   * {"RspCode":"99","Message":"..."} Momo expects: HTTP 204 or {"resultCode": 0} ZaloPay expects:
   * {"return_code": 1, "return_message": "success"} or {"return_code": 2, "return_message": "..."}
   */
  private String buildIPNResponse(boolean success, String message) {
    if (success) {
      return "{\"RspCode\":\"00\",\"Message\":\"" + message + "\"}";
    }
    return "{\"RspCode\":\"99\",\"Message\":\"" + message + "\"}";
  }

  private PaymentDto.PaymentTransactionResponse mapToTransactionResponse(PaymentTransaction txn) {
    return PaymentDto.PaymentTransactionResponse.builder()
        .id(txn.getId())
        .orderCode(txn.getOrderCode())
        .transactionRef(txn.getTransactionRef())
        .gatewayTransactionId(txn.getGatewayTransactionId())
        .paymentMethod(txn.getPaymentMethod().name())
        .status(txn.getStatus().name())
        .amount(txn.getAmount())
        .responseCode(txn.getResponseCode())
        .responseMessage(txn.getResponseMessage())
        .createdAt(txn.getCreatedAt())
        .updatedAt(txn.getUpdatedAt())
        .build();
  }

  private String getPaymentStatusMessage(Order.PaymentStatus status) {
    return switch (status) {
      case PENDING -> "Đang chờ thanh toán";
      case PAID -> "Đã thanh toán thành công";
      case FAILED -> "Thanh toán thất bại";
      case REFUNDED -> "Đã hoàn tiền";
    };
  }
}
