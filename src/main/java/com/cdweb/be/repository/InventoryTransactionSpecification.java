package com.cdweb.be.repository;

import com.cdweb.be.entity.Inventory;
import com.cdweb.be.entity.Inventory.TransactionType;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class InventoryTransactionSpecification {

  private InventoryTransactionSpecification() {}

  public static Specification<Inventory> adminFilter(
      Integer variantId,
      TransactionType transactionType,
      String referenceType,
      Integer referenceId,
      LocalDate fromDate,
      LocalDate toDate) {
    return (root, query, cb) -> {
      if (query != null && Inventory.class.equals(query.getResultType())) {
        root.fetch("variant", JoinType.LEFT);
        root.fetch("user", JoinType.LEFT);
        query.distinct(true);
      }

      List<Predicate> predicates = new ArrayList<>();

      if (variantId != null) {
        predicates.add(cb.equal(root.join("variant", JoinType.LEFT).get("id"), variantId));
      }

      if (transactionType != null) {
        predicates.add(cb.equal(root.get("transactionType"), transactionType));
      }

      if (referenceType != null && !referenceType.isBlank()) {
        predicates.add(cb.equal(root.get("referenceType"), referenceType.trim()));
      }

      if (referenceId != null) {
        predicates.add(cb.equal(root.get("referenceId"), referenceId));
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
