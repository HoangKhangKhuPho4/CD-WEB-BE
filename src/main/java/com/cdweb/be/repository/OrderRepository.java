package com.cdweb.be.repository;

import com.cdweb.be.entity.Order;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderRepository extends JpaRepository<Order, Integer> {

  Optional<Order> findByOrderCode(String orderCode);

  @Query("SELECT o FROM Order o WHERE o.isHidden = false")
  Page<Order> findAllActive(Pageable pageable);

  @Query("SELECT o FROM Order o WHERE o.isHidden = true")
  Page<Order> findHiddenOrders(Pageable pageable);

  @Query("SELECT COUNT(o) FROM Order o WHERE o.isHidden = true")
  Long countHiddenOrders();

  // Đã sửa Integer -> Long userId
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId")
  Page<Order> findByUserId(@Param("userId") Long userId, Pageable pageable);

  @Query("SELECT o FROM Order o WHERE o.status = :status AND o.isHidden = false")
  Page<Order> findByStatus(@Param("status") Order.OrderStatus status, Pageable pageable);

  @Query("SELECT o FROM Order o WHERE " + "o.orderDate BETWEEN :startDate AND :endDate")
  Page<Order> findByOrderDateBetween(
          @Param("startDate") LocalDateTime startDate,
          @Param("endDate") LocalDateTime endDate,
          Pageable pageable);

  // Đã sửa Integer -> Long userId
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND " + "o.status = :status")
  Page<Order> findByUserIdAndStatus(
          @Param("userId") Long userId,
          @Param("status") Order.OrderStatus status,
          Pageable pageable);

  @Query("SELECT COUNT(o) FROM Order o WHERE o.status = :status")
  Long countByStatus(@Param("status") Order.OrderStatus status);

  @Query(
          "SELECT SUM(o.totalAmount) FROM Order o WHERE "
                  + "o.status = 'DELIVERED' AND o.orderDate BETWEEN :startDate AND :endDate")
  Double getTotalRevenueByDateRange(
          @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

  @Query(
          "SELECT o FROM Order o WHERE "
                  + "(o.user.username LIKE %:keyword% OR "
                  + " o.user.email LIKE %:keyword% OR "
                  + " o.orderCode LIKE %:keyword%) "
                  + "AND o.isHidden = false")
  Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);

  @Query(
          "SELECT o FROM Order o WHERE "
                  + "(o.user.username LIKE %:keyword% OR o.user.email LIKE %:keyword% OR o.orderCode LIKE %:keyword%) "
                  + "AND o.status = :status AND o.isHidden = false")
  Page<Order> searchOrdersByKeywordAndStatus(
          @Param("keyword") String keyword,
          @Param("status") Order.OrderStatus status,
          Pageable pageable);

  // ── STATS QUERIES ──
  @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.status IN :statuses")
  java.math.BigDecimal sumRevenueCompleted(@Param("statuses") List<Order.OrderStatus> statuses);

  @Query("SELECT COUNT(o) FROM Order o WHERE o.status <> :excludedStatus")
  Long countActiveOrders(@Param("excludedStatus") Order.OrderStatus excludedStatus);

  @Query("SELECT COUNT(DISTINCT o.user.id) FROM Order o WHERE o.status <> :excludedStatus")
  Long countDistinctCustomers(@Param("excludedStatus") Order.OrderStatus excludedStatus);

  @Query(
          "SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o "
                  + "WHERE o.status IN :statuses "
                  + "AND o.orderDate BETWEEN :start AND :end")
  java.math.BigDecimal sumRevenueByDateRange(
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end,
          @Param("statuses") List<Order.OrderStatus> statuses);

  @Query(
          "SELECT COUNT(o) FROM Order o "
                  + "WHERE o.status <> :excludedStatus "
                  + "AND o.orderDate BETWEEN :start AND :end")
  Long countOrdersByDateRange(
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end,
          @Param("excludedStatus") Order.OrderStatus excludedStatus);

  @Query(
          "SELECT COUNT(DISTINCT o.user.id) FROM Order o "
                  + "WHERE o.status <> :excludedStatus "
                  + "AND o.orderDate BETWEEN :start AND :end")
  Long countDistinctCustomersByDateRange(
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end,
          @Param("excludedStatus") Order.OrderStatus excludedStatus);

  @Query(
          "SELECT CAST(o.orderDate AS date), SUM(o.totalAmount), COUNT(o) "
                  + "FROM Order o "
                  + "WHERE o.status IN :statuses "
                  + "AND o.orderDate BETWEEN :start AND :end "
                  + "GROUP BY CAST(o.orderDate AS date) "
                  + "ORDER BY CAST(o.orderDate AS date)")
  List<Object[]> getRevenueByDay(
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end,
          @Param("statuses") List<Order.OrderStatus> statuses);

  @Query(
          "SELECT YEAR(o.orderDate), MONTH(o.orderDate), SUM(o.totalAmount), COUNT(o) "
                  + "FROM Order o "
                  + "WHERE o.status IN :statuses "
                  + "AND o.orderDate BETWEEN :start AND :end "
                  + "GROUP BY YEAR(o.orderDate), MONTH(o.orderDate) "
                  + "ORDER BY YEAR(o.orderDate), MONTH(o.orderDate)")
  List<Object[]> getRevenueByMonth(
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end,
          @Param("statuses") List<Order.OrderStatus> statuses);

  @Query(
          "SELECT YEAR(o.orderDate), SUM(o.totalAmount), COUNT(o) "
                  + "FROM Order o "
                  + "WHERE o.status IN :statuses "
                  + "GROUP BY YEAR(o.orderDate) ORDER BY YEAR(o.orderDate)")
  List<Object[]> getRevenueByYear(@Param("statuses") List<Order.OrderStatus> statuses);

  @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
  List<Object[]> countOrdersByEachStatus();

  @Query("SELECT o FROM Order o JOIN FETCH o.user ORDER BY o.orderDate DESC")
  List<Order> findRecentOrders(Pageable pageable);

  @Query(
          "SELECT o.paymentMethod, COUNT(o), COALESCE(SUM(o.totalAmount), 0) "
                  + "FROM Order o "
                  + "WHERE o.status <> :excludedStatus "
                  + "GROUP BY o.paymentMethod ORDER BY COUNT(o) DESC")
  List<Object[]> getPaymentMethodStats(@Param("excludedStatus") Order.OrderStatus excludedStatus);

  // Đã sửa Integer -> Long userId
  @Query(
          "SELECT o FROM Order o "
                  + "JOIN o.orderDetails od "
                  + "JOIN od.variant v "
                  + "WHERE o.user.id = :userId AND v.product.id = :productId "
                  + "AND o.status IN :statuses AND o.isHidden = false "
                  + "ORDER BY o.orderDate DESC")
  List<Order> findUserOrdersByProduct(
          @Param("userId") Long userId,
          @Param("productId") Integer productId,
          @Param("statuses") List<Order.OrderStatus> statuses,
          Pageable pageable);
}