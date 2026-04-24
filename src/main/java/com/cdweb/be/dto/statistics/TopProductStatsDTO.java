package com.cdweb.be.dto.statistics;

import java.math.BigDecimal;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/** DTO sản phẩm bán chạy + sản phẩm tồn kho thấp */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductStatsDTO {

  private String type; // "best-selling" | "low-stock"
  private List<ProductStat> products;

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductStat {
    private Integer rank; // Hạng (chỉ dùng cho best-selling)
    private Integer productId;
    private String productName;
    private String variantName;
    private String categoryName; // Tên danh mục (mới bổ sung)
    private String imageUrl;

    // ── Dùng cho best-selling ────────────────────────────────────────────
    private Long quantitySold; // Số lượng đã bán
    private BigDecimal revenue; // Doanh thu

    // ── Dùng cho low-stock ───────────────────────────────────────────────
    private Integer currentStock; // Tồn kho hiện tại
    private Integer lowStockThreshold; // Ngưỡng cảnh báo
    private String status; // "OUT_OF_STOCK", "CRITICAL", "WARNING"
  }
}
