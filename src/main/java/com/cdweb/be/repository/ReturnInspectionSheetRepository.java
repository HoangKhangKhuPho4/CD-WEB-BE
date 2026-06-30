package com.cdweb.be.repository;

import com.cdweb.be.entity.ReturnInspectionSheet;
import com.cdweb.be.entity.ReturnInspectionSheet.SheetStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReturnInspectionSheetRepository extends JpaRepository<ReturnInspectionSheet, Integer> {

  List<ReturnInspectionSheet> findByStatusOrderByCreatedAtDesc(SheetStatus status);

  List<ReturnInspectionSheet> findTop20ByStatusInOrderByProcessedAtDesc(
      List<SheetStatus> statuses);

  Optional<ReturnInspectionSheet> findByProductItemIdAndStatus(
      Integer productItemId, SheetStatus status);

  boolean existsByProductItemIdAndStatus(Integer productItemId, SheetStatus status);

  boolean existsByProductItemIdAndStatusIn(
      Integer productItemId, java.util.Collection<SheetStatus> statuses);

  List<ReturnInspectionSheet> findByStatusOrderByUpdatedAtDesc(SheetStatus status);

  long countByOrderIdAndStatus(Integer orderId, SheetStatus status);

  long countByOrderIdAndStatusIn(Integer orderId, java.util.Collection<SheetStatus> statuses);

  List<ReturnInspectionSheet> findByOrderIdAndStatus(Integer orderId, SheetStatus status);

  long count();
}
