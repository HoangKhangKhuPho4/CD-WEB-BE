package com.cdweb.be.repository;

import com.cdweb.be.entity.Coupon;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface CouponRepository extends JpaRepository<Coupon, Integer> {

  @Query(
      "SELECT c FROM Coupon c WHERE "
          + "(:keyword IS NULL OR LOWER(c.code) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(c.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) "
          + "AND (:isActive IS NULL OR c.isActive = :isActive)")
  Page<Coupon> searchCoupons(String keyword, Boolean isActive, Pageable pageable);

  Optional<Coupon> findByCode(String code);

  @Query(
      "SELECT c FROM Coupon c WHERE c.dateStart <= :now AND c.dateEnd >= :now AND c.isActive = true")
  List<Coupon> findActiveCoupons(LocalDateTime now);

  @Query(
      "SELECT c FROM Coupon c WHERE c.code = :code AND c.dateStart <= :now AND c.dateEnd >= :now AND c.isActive = true")
  Optional<Coupon> findActiveCouponByCode(String code, LocalDateTime now);

  boolean existsByCode(String code);
}
