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

  @Query("SELECT COUNT(pi) FROM ProductItem pi WHERE pi.variant.id = :variantId")
  long countByVariantId(@Param("variantId") Integer variantId);

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

  List<ProductItem> findByVariantIdAndStatusOrderByCreatedAtAsc(
      Integer variantId, ProductItem.ProductItemStatus status, Pageable pageable);

  List<ProductItem> findByPurchaseOrder_IdAndVariant_IdOrderByCreatedAtAsc(
      Integer purchaseOrderId, Integer variantId);

  @Query(
      """
      SELECT pi FROM ProductItem pi
      JOIN FETCH pi.variant v
      JOIN FETCH v.product p
      WHERE pi.status = 'AVAILABLE'
        AND p.productType.id = :productTypeId
      ORDER BY v.id, pi.createdAt ASC
      """)
  List<ProductItem> findAvailableByProductTypeId(@Param("productTypeId") Integer productTypeId);

  @Query(
      """
      SELECT pi.variant.id, COUNT(pi) FROM ProductItem pi
      JOIN pi.variant v
      JOIN v.product p
      WHERE pi.status = 'AVAILABLE'
        AND p.productType.id = :productTypeId
      GROUP BY pi.variant.id
      """)
  List<Object[]> countAvailableByVariantForProductType(
      @Param("productTypeId") Integer productTypeId);

  @Query(
      "SELECT pi.variant.id, COUNT(pi) FROM ProductItem pi "
          + "WHERE pi.status = :status GROUP BY pi.variant.id")
  List<Object[]> countByVariantGroupedByStatus(
      @Param("status") ProductItem.ProductItemStatus status);

  @Query(
      value =
          """
          SELECT pi.variant_id, pi.location FROM product_items pi
          INNER JOIN (
            SELECT variant_id, MAX(created_at) AS max_created
            FROM product_items
            WHERE status = 'AVAILABLE' AND location IS NOT NULL AND location <> ''
            GROUP BY variant_id
          ) latest ON pi.variant_id = latest.variant_id AND pi.created_at = latest.max_created
          WHERE pi.status = 'AVAILABLE' AND pi.location IS NOT NULL AND pi.location <> ''
          """,
      nativeQuery = true)
  List<Object[]> findLatestShelfLocationByVariant();

  @Query(
      "SELECT COUNT(DISTINCT pi.batchNumber) FROM ProductItem pi "
          + "WHERE pi.purchaseOrder.id = :poId AND pi.batchNumber IS NOT NULL AND pi.batchNumber <> ''")
  long countDistinctBatchNumbersByPurchaseOrderId(@Param("poId") Integer poId);

  long countByPurchaseOrder_IdAndBatchNumber(Integer purchaseOrderId, String batchNumber);

  @Query(
      """
      SELECT pi.batchNumber, COUNT(pi) FROM ProductItem pi
      WHERE pi.purchaseOrder.id = :poId
        AND pi.batchNumber IS NOT NULL AND pi.batchNumber <> ''
      GROUP BY pi.batchNumber
      ORDER BY MIN(pi.createdAt) ASC
      """)
  List<Object[]> countGroupedByBatchForPurchaseOrder(@Param("poId") Integer poId);
}
