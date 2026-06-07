package com.cdweb.be.repository;

import com.cdweb.be.entity.Coupon;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CouponSpecification {

  private CouponSpecification() {}

  public static Specification<Coupon> adminFilter(
      String keyword,
      Boolean isActive,
      String discountType,
      String lifecycle,
      LocalDateTime now) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (keyword != null && !keyword.isBlank()) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)));
      }

      if (isActive != null) {
        predicates.add(cb.equal(root.get("isActive"), isActive));
      }

      if (discountType != null && !discountType.isBlank()) {
        try {
          predicates.add(
              cb.equal(
                  root.get("discountType"),
                  Coupon.DiscountType.valueOf(discountType.trim().toUpperCase())));
        } catch (IllegalArgumentException ignored) {
          predicates.add(cb.disjunction());
        }
      }

      if (lifecycle != null && !lifecycle.isBlank() && now != null) {
        switch (lifecycle.trim().toUpperCase()) {
          case "EXPIRED" ->
              predicates.add(cb.lessThan(root.get("dateEnd"), now));
          case "UPCOMING" ->
              predicates.add(cb.greaterThan(root.get("dateStart"), now));
          case "EXHAUSTED" ->
              predicates.add(
                  cb.and(
                      cb.isNotNull(root.get("usageLimit")),
                      cb.greaterThanOrEqualTo(root.get("usedCount"), root.get("usageLimit"))));
          case "ACTIVE" ->
              predicates.add(
                  cb.and(
                      cb.isTrue(root.get("isActive")),
                      cb.lessThanOrEqualTo(root.get("dateStart"), now),
                      cb.greaterThanOrEqualTo(root.get("dateEnd"), now),
                      cb.or(
                          cb.isNull(root.get("usageLimit")),
                          cb.lessThan(root.get("usedCount"), root.get("usageLimit")))));
          case "INACTIVE" -> predicates.add(cb.isFalse(root.get("isActive")));
          default -> { /* ALL — no extra filter */ }
        }
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
