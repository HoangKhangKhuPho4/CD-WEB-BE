package com.cdweb.be.repository;

import com.cdweb.be.entity.CartItem;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Integer> {

  @Query("SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId")
  List<CartItem> findByUserId(@Param("userId") Integer userId);

  @Query(
      "SELECT ci FROM CartItem ci WHERE ci.cart.user.id = :userId AND ci.variant.id = :variantId")
  Optional<CartItem> findByUserIdAndVariantId(
      @Param("userId") Integer userId, @Param("variantId") Integer variantId);

  @Modifying
  @Query("DELETE FROM CartItem ci WHERE ci.cart.user.id = :userId")
  void deleteAllByUserId(@Param("userId") Integer userId);

  @Query("SELECT COUNT(ci) FROM CartItem ci WHERE ci.cart.user.id = :userId")
  Long countByUserId(@Param("userId") Integer userId);
}
