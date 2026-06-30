package com.cdweb.be.repository;

import com.cdweb.be.entity.OrderItem;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {

  List<OrderItem> findByOrderDetailId(Integer orderDetailId);

  @Query("SELECT oi FROM OrderItem oi WHERE oi.orderDetail.order.id = :orderId")
  List<OrderItem> findByOrderId(@Param("orderId") Integer orderId);

  @Query("SELECT COUNT(oi) FROM OrderItem oi WHERE oi.orderDetail.id = :orderDetailId")
  long countByOrderDetailId(@Param("orderDetailId") Integer orderDetailId);

  @Query(
      """
      SELECT oi FROM OrderItem oi
      JOIN FETCH oi.orderDetail od
      JOIN FETCH od.order o
      WHERE oi.productItem.id = :productItemId
      ORDER BY o.orderDate DESC
      """)
  List<OrderItem> findByProductItemIdWithOrder(@Param("productItemId") Integer productItemId);

  @Query(
      """
      SELECT oi FROM OrderItem oi
      JOIN FETCH oi.productItem pi
      JOIN FETCH pi.variant v
      LEFT JOIN FETCH v.product
      JOIN FETCH oi.orderDetail od
      JOIN FETCH od.order o
      WHERE pi.status = 'RETURNED'
      ORDER BY pi.updatedAt DESC
      """)
  Page<OrderItem> findReturnedItemsPendingInspection(Pageable pageable);
}
