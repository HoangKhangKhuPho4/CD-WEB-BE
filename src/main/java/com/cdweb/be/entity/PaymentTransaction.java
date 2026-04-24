package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

/**
 * Lưu lịch sử mỗi giao dịch thanh toán với Payment Gateway. Một Order có thể có nhiều
 * PaymentTransaction (khi user retry thanh toán).
 */
@Entity
@Table(name = "payment_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentTransaction {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "order_code", nullable = false, length = 50)
  private String orderCode;

  /** Mã giao dịch nội bộ — dùng để gửi sang gateway và đối soát */
  @Column(name = "transaction_ref", nullable = false, unique = true, length = 100)
  private String transactionRef;

  /**
   * Mã giao dịch từ phía gateway trả về (VNPay: vnp_TransactionNo, Momo: transId, ZaloPay:
   * zp_trans_id)
   */
  @Column(name = "gateway_transaction_id", length = 100)
  private String gatewayTransactionId;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  private Order.PaymentMethod paymentMethod;

  @Builder.Default
  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TransactionStatus status = TransactionStatus.PENDING;

  @Column(name = "amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal amount;

  /** URL thanh toán do gateway tạo ra */
  @Column(name = "payment_url", length = 2000)
  private String paymentUrl;

  /** Mã phản hồi từ gateway (VNPay: vnp_ResponseCode, Momo: resultCode, ...) */
  @Column(name = "response_code", length = 20)
  private String responseCode;

  /** Message phản hồi từ gateway */
  @Column(name = "response_message", length = 500)
  private String responseMessage;

  /** Raw data callback (JSON) từ gateway — để debug / đối soát */
  @Column(name = "callback_data", columnDefinition = "TEXT")
  private String callbackData;

  /** IP của user khi tạo giao dịch */
  @Column(name = "ip_address", length = 50)
  private String ipAddress;

  @Column(name = "created_at", nullable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at")
  private LocalDateTime updatedAt;

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  // Enums
  public enum TransactionStatus {
    PENDING, // đang chờ thanh toán
    SUCCESS, // thanh toán thành công
    FAILED, // thanh toán thất bại
    CANCELLED, // user hủy thanh toán
    EXPIRED // hết hạn thanh toán
  }
}
