package com.cdweb.be.repository;

import com.cdweb.be.entity.WarrantyTicket;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface WarrantyTicketRepository extends JpaRepository<WarrantyTicket, Integer> {

  @Query(
      "SELECT t FROM WarrantyTicket t "
          + "JOIN FETCH t.productItem p "
          + "LEFT JOIN FETCH p.variant v "
          + "LEFT JOIN FETCH v.product prod "
          + "WHERE (:keyword IS NULL OR "
          + "       LOWER(t.ticketCode) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "       LOWER(p.imei) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "       LOWER(p.serialNumber) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "       LOWER(t.customerName) LIKE LOWER(CONCAT('%', :keyword, '%')) OR "
          + "       LOWER(t.customerPhone) LIKE LOWER(CONCAT('%', :keyword, '%')))"
          + "AND (:status IS NULL OR t.status = :status)")
  Page<WarrantyTicket> searchTickets(
      @Param("keyword") String keyword,
      @Param("status") WarrantyTicket.TicketStatus status,
      Pageable pageable);

  @Query("SELECT MAX(t.ticketCode) FROM WarrantyTicket t WHERE t.ticketCode LIKE :prefix%")
  String findMaxTicketCodeWithPrefix(@Param("prefix") String prefix);

  List<WarrantyTicket> findByProductItemIdOrderByReceivedAtDesc(Integer productItemId);
}
