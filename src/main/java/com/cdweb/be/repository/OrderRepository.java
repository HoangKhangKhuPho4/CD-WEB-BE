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
          "SELECT o FROM Order o LEFT JOIN o.user u WHERE o.isHidden = false AND ("
                  + "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(o.shippingName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(o.shippingPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  Page<Order> searchOrders(@Param("keyword") String keyword, Pageable pageable);

  @Query(
          "SELECT o FROM Order o LEFT JOIN o.user u WHERE o.isHidden = false AND o.status = :status AND ("
                  + "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(o.shippingName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(o.shippingPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')))")
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
                  + "AND o.orderDate BETWEEN :start AND :end "
                  + "GROUP BY YEAR(o.orderDate) ORDER BY YEAR(o.orderDate)")
  List<Object[]> getRevenueByYearInRange(
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end,
          @Param("statuses") List<Order.OrderStatus> statuses);

  @Query(
          "SELECT o.status, COUNT(o) FROM Order o "
                  + "WHERE o.orderDate BETWEEN :start AND :end "
                  + "GROUP BY o.status")
  List<Object[]> countOrdersByEachStatusInRange(
          @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

  @Query(
          "SELECT o.paymentMethod, COUNT(o), COALESCE(SUM(o.totalAmount), 0) "
                  + "FROM Order o "
                  + "WHERE o.status <> :excludedStatus "
                  + "AND o.orderDate BETWEEN :start AND :end "
                  + "GROUP BY o.paymentMethod ORDER BY COUNT(o) DESC")
  List<Object[]> getPaymentMethodStatsByDateRange(
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end,
          @Param("excludedStatus") Order.OrderStatus excludedStatus);

  @Query(
          "SELECT COUNT(o) FROM Order o "
                  + "WHERE o.status = :status AND o.orderDate BETWEEN :start AND :end")
  Long countByStatusAndDateRange(
          @Param("status") Order.OrderStatus status,
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end);

  @Query("SELECT o.status, COUNT(o) FROM Order o GROUP BY o.status")
  List<Object[]> countOrdersByEachStatus();

  @Query("SELECT o FROM Order o JOIN FETCH o.user ORDER BY o.orderDate DESC")
  List<Order> findRecentOrders(Pageable pageable);

  @Query(
          "SELECT o FROM Order o JOIN FETCH o.user "
                  + "WHERE o.orderDate BETWEEN :start AND :end "
                  + "ORDER BY o.orderDate DESC")
  List<Order> findRecentOrdersByDateRange(
          @Param("start") LocalDateTime start,
          @Param("end") LocalDateTime end,
          Pageable pageable);

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

  @Query(
          "SELECT DISTINCT o FROM Order o "
                  + "JOIN FETCH o.orderDetails od "
                  + "JOIN FETCH od.variant v "
                  + "JOIN FETCH v.product p "
                  + "WHERE o.user.id = :userId "
                  + "AND o.status IN :statuses "
                  + "AND o.isHidden = false "
                  + "ORDER BY o.orderDate DESC")
  List<Order> findDeliveredOrdersWithDetailsForUser(
          @Param("userId") Long userId, @Param("statuses") List<Order.OrderStatus> statuses);

  // 1. Tìm kiếm theo keyword và status (admin quản lý đơn hàng)
  @Query(
          "SELECT DISTINCT o FROM Order o LEFT JOIN o.user u WHERE o.isHidden = false "
                  + "AND (:status IS NULL OR o.status = :status) "
                  + "AND ("
                  + "LOWER(o.orderCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(o.shippingName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(o.shippingPhone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.phone) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
                  + "(:phoneDigits IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(o.shippingPhone, ' ', ''), '-', ''), '.', ''), '+', '') "
                  + "LIKE CONCAT('%', :phoneDigits, '%')) OR "
                  + "(:phoneDigits IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(u.phone, ' ', ''), '-', ''), '.', ''), '+', '') "
                  + "LIKE CONCAT('%', :phoneDigits, '%')) OR "
                  + "(:phoneSuffix IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(o.shippingPhone, ' ', ''), '-', ''), '.', ''), '+', '') "
                  + "LIKE CONCAT('%', :phoneSuffix)) OR "
                  + "(:phoneSuffix IS NOT NULL AND REPLACE(REPLACE(REPLACE(REPLACE(u.phone, ' ', ''), '-', ''), '.', ''), '+', '') "
                  + "LIKE CONCAT('%', :phoneSuffix))"
                  + ")")
  Page<Order> searchOrders(
          @Param("keyword") String keyword,
          @Param("phoneDigits") String phoneDigits,
          @Param("phoneSuffix") String phoneSuffix,
          @Param("status") Order.OrderStatus status,
          Pageable pageable);

  // 2. Lọc theo status
  @Query(
          "SELECT o FROM Order o WHERE o.isHidden = false "
                  + "AND (:status IS NULL OR o.status = :status)")
  Page<Order> findByStatusFilter(@Param("status") Order.OrderStatus status, Pageable pageable);

  // 3. Khách hàng lấy danh sách đơn của họ (có lọc status)
  // Lưu ý: Đổi Long userId thành kiểu dữ liệu ID tương ứng của User trong project của bạn (Long hoặc Integer)
  @Query("SELECT o FROM Order o WHERE o.user.id = :userId AND (:status IS NULL OR o.status = :status)")
  Page<Order> findByUserIdAndStatusFilter(@Param("userId") Long userId, @Param("status") Order.OrderStatus status, Pageable pageable);

  // 4. Khách hàng xem chi tiết đơn của họ
  Optional<Order> findByIdAndUserId(Integer id, Long userId);

  @Query(
      "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId "
          + "AND o.status NOT IN :excludedStatuses")
  long countActiveOrdersByUserId(
      @Param("userId") Long userId, @Param("excludedStatuses") List<Order.OrderStatus> excludedStatuses);

  @Query(
      "SELECT COUNT(o) FROM Order o WHERE o.user.id = :userId AND o.couponCode = :couponCode "
          + "AND o.status NOT IN :excludedStatuses")
  long countCouponUsageByUser(
      @Param("userId") Long userId,
      @Param("couponCode") String couponCode,
      @Param("excludedStatuses") List<Order.OrderStatus> excludedStatuses);

  @Query(
      "SELECT o FROM Order o WHERE o.couponCode = :couponCode "
          + "AND o.status NOT IN :excludedStatuses ORDER BY o.orderDate DESC")
  Page<Order> findByCouponCode(
      @Param("couponCode") String couponCode,
      @Param("excludedStatuses") List<Order.OrderStatus> excludedStatuses,
      Pageable pageable);
}