package com.cdweb.be.dto.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** KPI tổng quan cho nhân viên kho / sale (không cần REPORT_REVENUE). */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StaffOverviewStatisticsDTO {
  private long pendingOrders;
  private long confirmedOrders;
  private long processingOrders;
  private long shippingOrders;
  private long ordersToday;
  private long lowStockVariants;
  private long customerAccounts;
  /** PO chờ nhập (PENDING + APPROVED + RECEIVING). */
  private long pendingPurchaseOrders;
  /** Đơn đã hoàn tiền — chờ kho ghi nhận hàng hoàn. */
  private long pendingReturnOrders;
}
