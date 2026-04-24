package com.cdweb.be.repository;

import com.cdweb.be.entity.ProductItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductItemRepository extends JpaRepository<ProductItem, Integer> {
  Optional<ProductItem> findByImeiOrSerialNumber(String imei, String serialNumber);

  List<ProductItem> findByVariantIdAndStatus(
      Integer variantId, ProductItem.ProductItemStatus status);

  long countByVariantIdAndStatus(Integer variantId, ProductItem.ProductItemStatus status);
}
