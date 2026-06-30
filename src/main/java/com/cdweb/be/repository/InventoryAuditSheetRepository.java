package com.cdweb.be.repository;

import com.cdweb.be.entity.InventoryAuditSheet;
import com.cdweb.be.entity.InventoryAuditSheet.AuditStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface InventoryAuditSheetRepository extends JpaRepository<InventoryAuditSheet, Integer> {

  List<InventoryAuditSheet> findAllByOrderByCreatedAtDesc();

  List<InventoryAuditSheet> findTop8ByOrderByCreatedAtDesc();

  List<InventoryAuditSheet> findByStatusInOrderByCreatedAtDesc(List<AuditStatus> statuses);

  List<InventoryAuditSheet> findTop10ByStatusInOrderByUpdatedAtDesc(List<AuditStatus> statuses);

  long countByStatus(AuditStatus status);

  long countByStatusIn(List<AuditStatus> statuses);

  boolean existsByProductTypeIdAndRetailLockedTrueAndStatusIn(
      Integer productTypeId, List<AuditStatus> statuses);

  Optional<InventoryAuditSheet> findFirstByProductTypeIdAndStatusInOrderByCreatedAtDesc(
      Integer productTypeId, List<AuditStatus> statuses);
}
