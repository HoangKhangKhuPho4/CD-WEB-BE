package com.cdweb.be.repository;

import com.cdweb.be.entity.Wishlist;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, Integer> {

  @EntityGraph(attributePaths = {"product", "product.images", "variant"})
  Page<Wishlist> findByUserId(Long userId, Pageable pageable);

  Optional<Wishlist> findByUserIdAndProductIdAndVariantId(
      Long userId, Integer productId, Integer variantId);

  Optional<Wishlist> findByUserIdAndProductIdAndVariantIsNull(Long userId, Integer productId);

  Optional<Wishlist> findByUserIdAndProductId(Long userId, Integer productId);

  boolean existsByUserIdAndProductId(Long userId, Integer productId);

  void deleteByUserId(Long userId);
}
