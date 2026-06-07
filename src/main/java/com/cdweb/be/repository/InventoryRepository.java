package com.cdweb.be.repository;

import com.cdweb.be.entity.Inventory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryRepository
    extends JpaRepository<Inventory, Integer>, JpaSpecificationExecutor<Inventory> {
  List<Inventory> findAllByOrderByCreatedAtDesc();
}
