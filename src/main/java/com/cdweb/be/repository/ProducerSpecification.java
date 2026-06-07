package com.cdweb.be.repository;

import com.cdweb.be.entity.Producer;
import com.cdweb.be.entity.Product;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProducerSpecification {

  private ProducerSpecification() {}

  public static Specification<Producer> adminFilter(
      String keyword, Boolean isActive, String country, Boolean hasProducts) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      if (keyword != null && !keyword.isBlank()) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("code")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)));
      }

      if (isActive != null) {
        predicates.add(cb.equal(root.get("isActive"), isActive));
      }

      if (country != null && !country.isBlank()) {
        predicates.add(
            cb.like(cb.lower(root.get("country")), "%" + country.trim().toLowerCase() + "%"));
      }

      if (hasProducts != null) {
        Subquery<Integer> sq = query.subquery(Integer.class);
        var productRoot = sq.from(Product.class);
        sq.select(productRoot.get("producer").get("id"))
            .where(cb.equal(productRoot.get("producer"), root));
        predicates.add(hasProducts ? cb.exists(sq) : cb.not(cb.exists(sq)));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
