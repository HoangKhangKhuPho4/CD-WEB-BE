package com.cdweb.be.repository;

import com.cdweb.be.entity.ProductSpecification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductSpecificationRepository
    extends JpaRepository<ProductSpecification, Integer> {

  List<ProductSpecification> findByProductId(Integer productId);

  void deleteByProductId(Integer productId);
}
