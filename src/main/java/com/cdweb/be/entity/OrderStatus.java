package com.cdweb.be.entity;

public enum OrderStatus {
  PENDING("Chờ xác nhận"),
  CONFIRMED("Đã xác nhận"),
  PROCESSING("Đang xử lý"),
  SHIPPING("Đang vận chuyển"),
  DELIVERED("Đã giao hàng"),
  COMPLETED("Hoàn thành"),
  CANCELLED("Đã huỷ"),
  REFUNDED("Đã hoàn tiền");

  private final String displayName;

  OrderStatus(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }
}
