package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.*;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "order_code", nullable = false, unique = true, length = 50)
  private String orderCode;

  @Column(name = "shipping_name", nullable = false)
  private String shippingName;

  @Column(name = "shipping_phone", nullable = false, length = 20)
  private String shippingPhone;

  @Column(name = "shipping_address", nullable = false, length = 500)
  private String shippingAddress;

  @Column(name = "shipping_province", length = 100)
  private String shippingProvince;

  @Column(name = "shipping_district", length = 100)
  private String shippingDistrict;

  @Column(name = "shipping_ward", length = 100)
  private String shippingWard;

  @Column(name = "shipping_fee", precision = 15, scale = 2)
  private BigDecimal shippingFee = BigDecimal.ZERO;

  @Column(name = "tracking_code", length = 100)
  private String trackingCode;

  // GHN Shipping Integration
  @Column(name = "ghn_order_code", length = 50)
  private String ghnOrderCode;

  @Column(name = "to_district_id")
  private Integer toDistrictId;

  @Column(name = "to_ward_code", length = 20)
  private String toWardCode;

  @Column(name = "subtotal", nullable = false, precision = 15, scale = 2)
  private BigDecimal subtotal;

  @Column(name = "discount_amount", precision = 15, scale = 2)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(name = "total_amount", nullable = false, precision = 15, scale = 2)
  private BigDecimal totalAmount;

  @Column(name = "coupon_code", length = 50)
  private String couponCode;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private OrderStatus status = OrderStatus.PENDING;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_method", nullable = false)
  private PaymentMethod paymentMethod = PaymentMethod.COD;

  @Enumerated(EnumType.STRING)
  @Column(name = "payment_status")
  private PaymentStatus paymentStatus = PaymentStatus.PENDING;

  @Column(name = "note", columnDefinition = "TEXT")
  private String note;

  @Column(name = "admin_note", columnDefinition = "TEXT")
  private String adminNote;

  // ── Soft Delete — Admin ẩn đơn hàng ──────────────────────────────────────
  @Column(name = "is_hidden", nullable = false)
  private Boolean isHidden = false;

  @Column(name = "hidden_at")
  private LocalDateTime hiddenAt;

  @Column(name = "hidden_reason", columnDefinition = "TEXT")
  private String hiddenReason;

  @Column(name = "order_date", nullable = false)
  private LocalDateTime orderDate;

  @Column(name = "confirmed_at")
  private LocalDateTime confirmedAt;

  @Column(name = "shipped_at")
  private LocalDateTime shippedAt;

  @Column(name = "delivered_at")
  private LocalDateTime deliveredAt;

  @Column(name = "cancelled_at")
  private LocalDateTime cancelledAt;

  @Column(name = "cancel_reason", columnDefinition = "TEXT")
  private String cancelReason;

  // ── Payment Gateway fields ──────────────────────────────────────────────
  @Column(name = "transaction_ref", length = 100)
  private String transactionRef;

  @Column(name = "payment_url", length = 1000)
  private String paymentUrl;

  @Column(name = "paid_at")
  private LocalDateTime paidAt;

  // Relationships
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_address_id")
  private UserAddress userAddress;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shipping_method_id")
  private ShippingMethod shippingMethod;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "coupon_id")
  private Coupon coupon;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<OrderDetail> orderDetails;

  @PrePersist
  protected void onCreate() {
    orderDate = LocalDateTime.now();
  }

  // Enums
  public enum OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    SHIPPING,
    DELIVERED,
    COMPLETED,
    CANCELLED,
    REFUNDED
  }

  public enum PaymentMethod {
    COD,
    BANK_TRANSFER,
    MOMO,
    VNPAY,
    ZALOPAY
  }

  public enum PaymentStatus {
    PENDING,
    PAID,
    FAILED,
    REFUNDED
  }
}
