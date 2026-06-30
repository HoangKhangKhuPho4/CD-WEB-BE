package com.cdweb.be.repository;

import com.cdweb.be.entity.PurchaseOrder;
import com.cdweb.be.entity.PurchaseOrder.PurchaseOrderStatus;
import java.util.Collection;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, Integer> {

  @Query(
      "SELECT DISTINCT po FROM PurchaseOrder po "
          + "LEFT JOIN FETCH po.supplier "
          + "LEFT JOIN FETCH po.items "
          + "WHERE po.status IN :statuses "
          + "ORDER BY po.createdAt DESC")
  List<PurchaseOrder> findByStatusInOrderByExpectedDate(
      @Param("statuses") Collection<PurchaseOrderStatus> statuses);

  long countByStatusIn(Collection<PurchaseOrderStatus> statuses);

  @Query(
      "SELECT po FROM PurchaseOrder po WHERE po.status IN :statuses")
  Page<PurchaseOrder> findByStatusIn(
      @Param("statuses") Collection<PurchaseOrderStatus> statuses, Pageable pageable);

  @Query(
      "SELECT DISTINCT po FROM PurchaseOrder po "
          + "LEFT JOIN FETCH po.supplier "
          + "LEFT JOIN FETCH po.items i "
          + "LEFT JOIN FETCH i.variant v "
          + "LEFT JOIN FETCH v.product "
          + "WHERE po.id = :id")
  java.util.Optional<PurchaseOrder> findDetailedById(@Param("id") Integer id);
}
