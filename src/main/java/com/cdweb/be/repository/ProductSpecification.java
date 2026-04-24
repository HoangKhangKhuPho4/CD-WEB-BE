package com.cdweb.be.repository;

import com.cdweb.be.dto.ProductDto;
import com.cdweb.be.entity.Attribute;
import com.cdweb.be.entity.AttributeValue;
import com.cdweb.be.entity.Product;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.entity.Review;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Subquery;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class ProductSpecification {

  public static Specification<Product> buildSearchSpec(ProductDto.SearchRequest req) {
    return (root, query, cb) -> {
      List<Predicate> predicates = new ArrayList<>();

      // Always distinct to prevent duplicates from joins
      if (query != null) {
        query.distinct(true);
      }

      // Always filter active products only
      predicates.add(cb.equal(root.get("isActive"), true));

      // Keyword: match name OR description (case-insensitive)
      if (req.getKeyword() != null && !req.getKeyword().trim().isEmpty()) {
        String pattern = "%" + req.getKeyword().trim().toLowerCase() + "%";
        predicates.add(
            cb.or(
                cb.like(cb.lower(root.get("name")), pattern),
                cb.like(cb.lower(root.get("description")), pattern)));
      }

      // Category filter (productType id)
      if (req.getProductTypeId() != null) {
        predicates.add(cb.equal(root.get("productType").get("id"), req.getProductTypeId()));
      }

      // Brand / producer filter
      if (req.getProducerId() != null) {
        predicates.add(cb.equal(root.get("producer").get("id"), req.getProducerId()));
      }

      // Min price filter
      if (req.getMinPrice() != null) {
        predicates.add(
            cb.greaterThanOrEqualTo(root.get("basePrice"), BigDecimal.valueOf(req.getMinPrice())));
      }

      // Max price filter
      if (req.getMaxPrice() != null) {
        predicates.add(
            cb.lessThanOrEqualTo(root.get("basePrice"), BigDecimal.valueOf(req.getMaxPrice())));
      }

      // Rating filter (min rating)
      if (req.getMinRating() != null) {
        Subquery<Double> avgSub = query.subquery(Double.class);
        var reviewRoot = avgSub.from(Review.class);
        avgSub.select(cb.avg(reviewRoot.get("rating")));
        avgSub.where(cb.equal(reviewRoot.get("product").get("id"), root.get("id")));

        predicates.add(cb.greaterThanOrEqualTo(avgSub, req.getMinRating()));
      }

      // Color filter
      if (req.getColor() != null && !req.getColor().trim().isEmpty()) {
        Join<Product, ProductVariant> variantJoin = root.join("variants");
        Join<ProductVariant, AttributeValue> attrValJoin = variantJoin.join("attributeValues");
        Join<AttributeValue, Attribute> attrJoin = attrValJoin.join("attribute");

        String colorPattern = "%" + req.getColor().trim().toLowerCase() + "%";
        predicates.add(
            cb.and(
                cb.or(
                    cb.like(cb.lower(attrJoin.get("name")), "%màu%"),
                    cb.like(cb.lower(attrJoin.get("name")), "%color%")),
                cb.like(cb.lower(attrValJoin.get("value")), colorPattern)));
      }

      return cb.and(predicates.toArray(new Predicate[0]));
    };
  }
}
