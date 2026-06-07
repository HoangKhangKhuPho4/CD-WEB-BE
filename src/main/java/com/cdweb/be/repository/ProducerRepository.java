package com.cdweb.be.repository;

import com.cdweb.be.entity.Producer;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface ProducerRepository
    extends JpaRepository<Producer, Integer>, JpaSpecificationExecutor<Producer> {

  Optional<Producer> findByCode(String code);

  Optional<Producer> findByName(String name);

  boolean existsByCode(String code);

  boolean existsByName(String name);

  Page<Producer> findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
      String name, String code, Pageable pageable);

  Page<Producer> findByIsActiveAndNameContainingIgnoreCaseOrIsActiveAndCodeContainingIgnoreCase(
      Boolean isActive1, String name, Boolean isActive2, String code, Pageable pageable);

  List<Producer> findByIsActive(Boolean isActive);

  long countByIsActiveTrue();

  long countByIsActiveFalse();
}
