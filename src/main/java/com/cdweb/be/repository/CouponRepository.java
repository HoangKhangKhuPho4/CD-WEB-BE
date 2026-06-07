package com.cdweb.be.repository;

import com.cdweb.be.entity.Coupon;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository
    extends JpaRepository<Coupon, Integer>, JpaSpecificationExecutor<Coupon> {

  Optional<Coupon> findByCode(String code);

  boolean existsByCode(String code);

  @Query(
      "SELECT c FROM Coupon c WHERE c.isActive = true AND c.dateStart <= :now AND c.dateEnd >= :now "
          + "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
  List<Coupon> findAvailableCoupons(LocalDateTime now);

  @Query(
      "SELECT c FROM Coupon c WHERE c.code = :code AND c.isActive = true "
          + "AND c.dateStart <= :now AND c.dateEnd >= :now "
          + "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
  Optional<Coupon> findAvailableCouponByCode(String code, LocalDateTime now);

  long countByIsActiveTrue();

  long countByIsActiveFalse();

  @Query("SELECT COUNT(c) FROM Coupon c WHERE c.dateEnd < :now")
  long countExpired(LocalDateTime now);

  @Query("SELECT COUNT(c) FROM Coupon c WHERE c.dateStart > :now")
  long countUpcoming(LocalDateTime now);

  @Query(
      "SELECT COUNT(c) FROM Coupon c WHERE c.usageLimit IS NOT NULL AND c.usedCount >= c.usageLimit")
  long countExhausted();

  @Query("SELECT COALESCE(SUM(c.usedCount), 0L) FROM Coupon c")
  Long sumUsedCount();

  long countByFirstOrderOnlyTrue();

  /** Legacy — giữ tương thích. */
  @Query(
      "SELECT c FROM Coupon c WHERE "
          + "(:keyword IS NULL OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) "
          + "OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
          + "AND (:isActive IS NULL OR c.isActive = :isActive)")
  Page<Coupon> searchCoupons(String keyword, Boolean isActive, Pageable pageable);

  @Query(
      "SELECT c FROM Coupon c WHERE c.dateStart <= :now AND c.dateEnd >= :now AND c.isActive = true "
          + "AND (c.usageLimit IS NULL OR c.usedCount < c.usageLimit)")
  List<Coupon> findActiveCoupons(LocalDateTime now);

  /** Alias tương thích OrderService cũ. */
  default Optional<Coupon> findActiveCouponByCode(String code, LocalDateTime now) {
    return findAvailableCouponByCode(code, now);
  }
}
