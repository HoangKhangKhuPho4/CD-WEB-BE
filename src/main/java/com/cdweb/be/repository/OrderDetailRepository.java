package com.cdweb.be.repository;

import com.cdweb.be.entity.OrderDetail;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderDetailRepository extends JpaRepository<OrderDetail, Integer> {

  List<OrderDetail> findByOrderId(Integer orderId);

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  PHASE 6: Top Selling Products Query                                 ██
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Top sản phẩm bán chạy nhất — JOIN order_details + orders (DELIVERED/COMPLETED) Trả về:
   * [productId, productName, variantName, SUM(quantity), SUM(totalPrice), stockQuantity,
   * categoryName]
   */
  @Query(
      "SELECT od.variant.product.id, od.productName, od.variantName, "
          + "SUM(od.quantity), SUM(od.totalPrice), od.variant.stockQuantity, od.variant.product.productType.name, "
          + "(SELECT MAX(i.imageUrl) FROM Image i WHERE i.product.id = od.variant.product.id) "
          + "FROM OrderDetail od JOIN od.order o "
          + "WHERE o.status IN :statuses "
          + "GROUP BY od.variant.product.id, od.productName, od.variantName, od.variant.stockQuantity, od.variant.product.productType.name "
          + "ORDER BY SUM(od.quantity) DESC")
  List<Object[]> findTopSellingProducts(
      @org.springframework.data.repository.query.Param("statuses")
          List<com.cdweb.be.entity.Order.OrderStatus> statuses,
      Pageable pageable);

  @Query(
      "SELECT od.variant.product.id, od.productName, od.variantName, "
          + "SUM(od.quantity), SUM(od.totalPrice), od.variant.stockQuantity, od.variant.product.productType.name, "
          + "(SELECT MAX(i.imageUrl) FROM Image i WHERE i.product.id = od.variant.product.id) "
          + "FROM OrderDetail od JOIN od.order o "
          + "WHERE o.status IN :statuses AND o.orderDate BETWEEN :start AND :end "
          + "GROUP BY od.variant.product.id, od.productName, od.variantName, od.variant.stockQuantity, od.variant.product.productType.name "
          + "ORDER BY SUM(od.quantity) DESC")
  List<Object[]> findTopSellingProductsByDateRange(
      @org.springframework.data.repository.query.Param("statuses")
          List<com.cdweb.be.entity.Order.OrderStatus> statuses,
      @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
      @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end,
      Pageable pageable);

  /** Tổng số sản phẩm đã bán (quantity) — cho Overview KPI */
  @Query(
      "SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od JOIN od.order o "
          + "WHERE o.status IN :statuses")
  Long sumTotalProductsSold(
      @org.springframework.data.repository.query.Param("statuses")
          List<com.cdweb.be.entity.Order.OrderStatus> statuses);

  @Query(
      "SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od JOIN od.order o "
          + "WHERE o.status IN :statuses AND o.orderDate BETWEEN :start AND :end")
  Long sumTotalProductsSoldByDateRange(
      @org.springframework.data.repository.query.Param("statuses")
          List<com.cdweb.be.entity.Order.OrderStatus> statuses,
      @org.springframework.data.repository.query.Param("start") java.time.LocalDateTime start,
      @org.springframework.data.repository.query.Param("end") java.time.LocalDateTime end);

  /** Đếm tổng số lượng đã bán của 1 sản phẩm cụ thể (cho User Card) */
  @Query(
      "SELECT COALESCE(SUM(od.quantity), 0) FROM OrderDetail od "
          + "WHERE od.variant.product.id = :productId")
  Long sumQuantityByProductId(
      @org.springframework.data.repository.query.Param("productId") Integer productId);
}
