package com.cdweb.be.repository;

import com.cdweb.be.entity.OrderStatusHistory;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Integer> {

    List<OrderStatusHistory> findByOrderIdOrderByCreatedAtAsc(Integer orderId);
}