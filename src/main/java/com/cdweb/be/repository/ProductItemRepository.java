package com.cdweb.be.repository;

import com.cdweb.be.entity.ProductItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductItemRepository
    extends JpaRepository<ProductItem, Integer>, JpaSpecificationExecutor<ProductItem> {
  Optional<ProductItem> findByImeiOrSerialNumber(String imei, String serialNumber);

  List<ProductItem> findByVariantIdAndStatus(
      Integer variantId, ProductItem.ProductItemStatus status);

  long countByVariantIdAndStatus(Integer variantId, ProductItem.ProductItemStatus status);

  long countByStatus(ProductItem.ProductItemStatus status);

  @Query("SELECT COUNT(DISTINCT oi.productItem.id) FROM OrderItem oi")
  long countDistinctLinkedToOrders();

  @Query(
      """
      SELECT pi FROM ProductItem pi
      JOIN FETCH pi.variant v
      JOIN FETCH v.product p
      WHERE (:keyword IS NULL OR :keyword = '' OR
        LOWER(pi.imei) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(pi.serialNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(v.skuCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
      ORDER BY pi.createdAt DESC
      """)
  List<ProductItem> searchItems(@Param("keyword") String keyword);

  @Query(
      value =
          """
      SELECT pi FROM ProductItem pi
      JOIN FETCH pi.variant v
      JOIN FETCH v.product p
      WHERE (:keyword IS NULL OR :keyword = '' OR
        LOWER(pi.imei) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(pi.serialNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(v.skuCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
      ORDER BY pi.createdAt DESC
      """,
      countQuery =
          """
      SELECT COUNT(pi) FROM ProductItem pi
      JOIN pi.variant v
      JOIN v.product p
      WHERE (:keyword IS NULL OR :keyword = '' OR
        LOWER(pi.imei) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(pi.serialNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR
        LOWER(v.skuCode) LIKE LOWER(CONCAT('%', :keyword, '%')))
      """)
  Page<ProductItem> searchItems(@Param("keyword") String keyword, Pageable pageable);
}
