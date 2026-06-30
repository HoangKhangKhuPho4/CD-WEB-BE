package com.cdweb.be.repository;

import com.cdweb.be.entity.PurchaseOrderReceiveIssue;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderReceiveIssueRepository
    extends JpaRepository<PurchaseOrderReceiveIssue, Integer> {

  List<PurchaseOrderReceiveIssue> findByPurchaseOrder_IdOrderByCreatedAtDesc(Integer purchaseOrderId);
}
