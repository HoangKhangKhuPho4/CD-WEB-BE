package com.cdweb.be.repository;

import com.cdweb.be.entity.Wishlist;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

  Page<Wishlist> findByUserId(Integer userId, Pageable pageable);

  Optional<Wishlist> findByUserIdAndProductIdAndVariantId(
      Integer userId, Integer productId, Integer variantId);

  Optional<Wishlist> findByUserIdAndProductIdAndVariantIsNull(Integer userId, Integer productId);

  Optional<Wishlist> findByUserIdAndProductId(Integer userId, Integer productId);

  boolean existsByUserIdAndProductId(Integer userId, Integer productId);

  void deleteByUserId(Integer userId);
}
