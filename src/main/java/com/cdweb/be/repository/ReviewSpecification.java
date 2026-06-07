package com.cdweb.be.repository;

import com.cdweb.be.entity.Review;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ReviewSpecification {

  private ReviewSpecification() {}

  public static Specification<Review> adminFilter(
      Boolean isApproved,
      Integer rating,
      Integer productId,
      String keyword,
      Boolean verifiedOnly,
      Boolean hasReply,
      LocalDateTime fromDate,
      LocalDateTime toDate) {
    return (root, query, cb) -> {
      if (query != null && Long.class != query.getResultType()) {
        query.distinct(true);
      }

      List<Predicate> predicates = new ArrayList<>();

      if (isApproved != null) {
        predicates.add(cb.equal(root.get("isApproved"), isApproved));
      }
      if (rating != null) {
        predicates.add(cb.equal(root.get("rating"), rating));
      }
      if (productId != null) {
        predicates.add(cb.equal(root.get("product").get("id"), productId));
      }
      if (Boolean.TRUE.equals(verifiedOnly)) {
        predicates.add(cb.isTrue(root.get("isVerifiedPurchase")));
      }
      if (hasReply != null) {
        if (hasReply) {
          predicates.add(cb.isNotNull(root.get("replyContent")));
          predicates.add(cb.notEqual(root.get("replyContent"), ""));
        } else {
          predicates.add(
              cb.or(
                  cb.isNull(root.get("replyContent")),
                  cb.equal(root.get("replyContent"), "")));
        }
      }
      if (fromDate != null) {
        predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), fromDate));
      }
      if (toDate != null) {
        predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), toDate));
      }
      if (keyword != null && !keyword.isBlank()) {
        String pattern = "%" + keyword.trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("product").get("name")), pattern),
                cb.like(cb.lower(root.get("user").get("username")), pattern),
                cb.like(cb.lower(root.get("user").get("fullName")), pattern),
                cb.like(cb.lower(root.get("title")), pattern),
                cb.like(cb.lower(root.get("content")), pattern)));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
