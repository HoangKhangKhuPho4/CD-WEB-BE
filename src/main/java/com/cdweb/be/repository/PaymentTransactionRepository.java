package com.cdweb.be.repository;

import com.cdweb.be.entity.PaymentTransaction;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Integer> {

  Optional<PaymentTransaction> findByTransactionRef(String transactionRef);

  List<PaymentTransaction> findByOrderCodeOrderByCreatedAtDesc(String orderCode);

  @Query(
      "SELECT pt FROM PaymentTransaction pt WHERE pt.order.id = :orderId ORDER BY pt.createdAt DESC")
  List<PaymentTransaction> findByOrderId(@Param("orderId") Integer orderId);

  List<PaymentTransaction> findByOrderCodeAndStatus(
      String orderCode, PaymentTransaction.TransactionStatus status);

  Optional<PaymentTransaction> findFirstByOrderCodeAndStatusOrderByCreatedAtDesc(
      String orderCode, PaymentTransaction.TransactionStatus status);

  Page<PaymentTransaction> findAll(Pageable pageable);
}
