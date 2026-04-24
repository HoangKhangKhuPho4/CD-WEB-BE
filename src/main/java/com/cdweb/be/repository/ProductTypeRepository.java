package com.cdweb.be.repository;

import com.cdweb.be.entity.ProductType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductTypeRepository extends JpaRepository<ProductType, Integer> {

  Optional<ProductType> findByCode(String code);

  Optional<ProductType> findByName(String name);

  boolean existsByCode(String code);

  boolean existsByName(String name);

  // Cho Category API
  @Query("SELECT pt FROM ProductType pt WHERE pt.isActive = true")
  Page<ProductType> findAllActive(Pageable pageable);

  @Query("SELECT pt FROM ProductType pt WHERE pt.isActive = true")
  List<ProductType> findAllActive();

  @Query("SELECT pt FROM ProductType pt WHERE pt.isActive = true AND pt.parent IS NULL")
  List<ProductType> findRootCategories();

  @Query("SELECT pt FROM ProductType pt WHERE pt.isActive = true AND pt.parent.id = :parentId")
  List<ProductType> findByParentId(@Param("parentId") Integer parentId);

  @Query(
      "SELECT pt FROM ProductType pt WHERE pt.isActive = true AND "
          + "LOWER(pt.name) LIKE LOWER(CONCAT('%', :keyword, '%'))")
  Page<ProductType> searchByName(@Param("keyword") String keyword, Pageable pageable);

  @Query(
      "SELECT COUNT(p) FROM Product p WHERE p.productType.id = :productTypeId AND p.isActive = true")
  Long countProductsByProductTypeId(@Param("productTypeId") Integer productTypeId);
}
