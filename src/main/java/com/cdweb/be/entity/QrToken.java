package com.cdweb.be.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "qr_tokens")
public class QrToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token", nullable = false, unique = true, length = 512)
    private String token;

    @Column(name = "session_id", nullable = false, unique = true)
    private String sessionId;

    @Column(name = "qr_type", nullable = false)
    @Enumerated(EnumType.STRING)
    private QrType qrType;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    private QrStatus status = QrStatus.PENDING;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user; // null khi chưa scan

    @Column(name = "order_id")
    private Integer orderId; // dùng cho QR_ORDER_CONFIRMATION

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "scanned_at")
    private LocalDateTime scannedAt;

    public enum QrType {
        QR_LOGIN,
        QR_ORDER_CONFIRMATION
    }

    public enum QrStatus {
        PENDING,    // Chờ quét
        SCANNED,    // Đã quét, chờ xác nhận
        CONFIRMED,  // Đã xác nhận
        EXPIRED,    // Hết hạn
        CANCELLED   // Đã hủy
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
}