package com.cdweb.be.repository;

import com.cdweb.be.entity.UserInteraction;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInteractionRepository extends JpaRepository<UserInteraction, Long> {
  Optional<UserInteraction> findByUserIdAndProductIdAndActionType(
      Integer userId, Integer productId, String actionType);

  // 📊 1. Thống kê Views và Purchases theo từng Sản phẩm
  @org.springframework.data.jpa.repository.Query(
      "SELECT u.productId, p.name, "
          + "SUM(CASE WHEN u.actionType = 'VIEW' THEN 1 ELSE 0 END) as views, "
          + "SUM(CASE WHEN u.actionType = 'PURCHASE' THEN 1 ELSE 0 END) as purchases "
          + "FROM UserInteraction u JOIN Product p ON u.productId = p.id "
          + "GROUP BY u.productId, p.name")
  java.util.List<Object[]> countViewsAndPurchasesByProduct();

  // 📊 2. Phân vùng Sở thích: Đếm lượng action theo Danh mục (ProductType)
  @org.springframework.data.jpa.repository.Query(
      "SELECT p.productType.id, p.productType.name, "
          + "COUNT(u) as interactions "
          + "FROM UserInteraction u JOIN Product p ON u.productId = p.id "
          + "GROUP BY p.productType.id, p.productType.name")
  java.util.List<Object[]> countInteractionsByProductType();
}
