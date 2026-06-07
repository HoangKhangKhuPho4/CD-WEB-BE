package com.cdweb.be.repository;

import com.cdweb.be.entity.OrderItem;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProductItemSpecification {

  private ProductItemSpecification() {}

  public static Specification<ProductItem> adminFilter(
      String keyword,
      ProductItemStatus status,
      Integer variantId,
      String orderCode,
      LocalDate fromDate,
      LocalDate toDate) {
    return (root, query, cb) -> {
      if (query != null && ProductItem.class.equals(query.getResultType())) {
        root.fetch("variant", JoinType.LEFT).fetch("product", JoinType.LEFT);
        query.distinct(true);
      }

      List<Predicate> predicates = new ArrayList<>();

      if (keyword != null && !keyword.isBlank()) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        Join<?, ?> variant = root.join("variant", JoinType.LEFT);
        Join<?, ?> product = variant.join("product", JoinType.LEFT);
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("imei")), pattern),
                cb.like(cb.lower(root.get("serialNumber")), pattern),
                cb.like(cb.lower(product.get("name")), pattern),
                cb.like(cb.lower(variant.get("skuCode")), pattern),
                cb.like(cb.lower(variant.get("variantName")), pattern)));
      }

      if (status != null) {
        predicates.add(cb.equal(root.get("status"), status));
      }

      if (variantId != null) {
        predicates.add(cb.equal(root.join("variant").get("id"), variantId));
      }

      if (orderCode != null && !orderCode.isBlank()) {
        Subquery<Integer> sq = query.subquery(Integer.class);
        var oiRoot = sq.from(OrderItem.class);
        sq.select(oiRoot.get("productItem").get("id"))
            .where(
                cb.equal(oiRoot.get("productItem"), root),
                cb.like(
                    cb.lower(oiRoot.get("orderDetail").get("order").get("orderCode")),
                    "%" + orderCode.trim().toLowerCase() + "%"));
        predicates.add(cb.exists(sq));
      }

      if (fromDate != null) {
        LocalDateTime start = fromDate.atStartOfDay();
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), start));
      }

      if (toDate != null) {
        LocalDateTime end = toDate.plusDays(1).atStartOfDay();
        predicates.add(cb.lessThan(root.get("createdAt"), end));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
