package com.cdweb.be.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Entity
@Table(name = "warranty_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyTicket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Integer id;

  @Column(name = "ticket_code", nullable = false, unique = true, length = 50)
  private String ticketCode;

  // Liên kết với Máy lỗi
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "product_item_id", nullable = false)
  private ProductItem productItem;

  // Thông tin khách yêu cầu bảo hành (có thể khác khách mua nếu sang tay)
  @Column(name = "customer_name", nullable = false, length = 100)
  private String customerName;

  @Column(name = "customer_phone", nullable = false, length = 20)
  private String customerPhone;

  @Column(name = "issue_description", columnDefinition = "TEXT", nullable = false)
  private String issueDescription;

  @Column(name = "technician_note", columnDefinition = "TEXT")
  private String technicianNote;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false)
  private TicketStatus status = TicketStatus.PENDING;

  // Đánh giá lỗi thuộc về hãng hay người dùng, có tốn phí không
  @Column(name = "repair_cost", precision = 12, scale = 2)
  private BigDecimal repairCost;

  @Column(name = "received_at", nullable = false, updatable = false)
  private LocalDateTime receivedAt;

  @Column(name = "resolved_at")
  private LocalDateTime resolvedAt;

  @Column(name = "returned_at")
  private LocalDateTime returnedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy; // Nhân viên tiếp nhận

  @PrePersist
  protected void onCreate() {
    receivedAt = LocalDateTime.now();
  }

  public enum TicketStatus {
    PENDING, // Chờ kiểm tra
    IN_PROGRESS, // Đang sửa chữa (hoặc đã gửi hãng)
    COMPLETED, // Đã sửa xong, chờ khách lấy
    CANCELLED, // Hủy phiếu
    RETURNED // Đã trả khách
  }
}
