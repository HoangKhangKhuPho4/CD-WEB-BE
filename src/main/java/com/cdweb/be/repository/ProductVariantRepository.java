package com.cdweb.be.repository;

import com.cdweb.be.entity.ProductVariant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {

  @Query(
      "SELECT v FROM ProductVariant v "
          + "LEFT JOIN FETCH v.attributeValues av "
          + "LEFT JOIN FETCH av.attribute "
          + "WHERE v.product.productType.id = :categoryId AND v.isActive = true")
  List<ProductVariant> findByCategoryId(@Param("categoryId") Integer categoryId);

  @Query(
      "SELECT v FROM ProductVariant v "
          + "LEFT JOIN FETCH v.attributeValues av "
          + "LEFT JOIN FETCH av.attribute "
          + "WHERE v.product.id = :productId")
  List<ProductVariant> findByProductId(Integer productId);

  @Query(
      "SELECT DISTINCT v FROM ProductVariant v "
          + "LEFT JOIN FETCH v.attributeValues av "
          + "LEFT JOIN FETCH av.attribute "
          + "WHERE v.product.id = :productId")
  List<ProductVariant> findByProductIdWithAttributes(@Param("productId") Integer productId);

  java.util.Optional<ProductVariant> findBySkuCode(String skuCode);

  boolean existsBySkuCodeAndIdNot(String skuCode, Integer id);

  long countByIsActiveTrue();

  void deleteByProductId(Integer productId);

  // ═══════════════════════════════════════════════════════════════════════════
  // ██  PHASE 6: Low Stock Query                                            ██
  // ═══════════════════════════════════════════════════════════════════════════

  /** Sản phẩm tồn kho thấp — stock <= ngưỡng cảnh báo lowStockThreshold */
  @Query(
      "SELECT v FROM ProductVariant v "
          + "JOIN FETCH v.product p "
          + "WHERE v.isActive = true AND v.stockQuantity <= v.lowStockThreshold "
          + "ORDER BY v.stockQuantity ASC")
  List<ProductVariant> findLowStockVariants();

  @Query(
      "SELECT v FROM ProductVariant v "
          + "JOIN FETCH v.product p "
          + "WHERE v.isActive = true "
          + "ORDER BY v.stockQuantity ASC, p.name ASC")
  List<ProductVariant> findAllActiveWithProduct();

  @Query(
      "SELECT COUNT(v) FROM ProductVariant v "
          + "WHERE v.isActive = true AND v.stockQuantity <= v.lowStockThreshold")
  long countLowStockVariants();

  @Query(
      "SELECT v FROM ProductVariant v "
          + "JOIN FETCH v.product p "
          + "WHERE v.isActive = true AND (LOWER(v.skuCode) LIKE LOWER(CONCAT('%', :keyword, '%')) "
          + "OR LOWER(v.variantName) LIKE LOWER(CONCAT('%', :keyword, '%')) "
          + "OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))")
  List<ProductVariant> searchVariants(@Param("keyword") String keyword);
}
