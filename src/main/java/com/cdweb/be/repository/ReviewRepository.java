package com.cdweb.be.repository;

import com.cdweb.be.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

  Page<Review> findByProductIdAndIsApproved(
      Integer productId, Boolean isApproved, Pageable pageable);

  @Query(
      "SELECT r FROM Review r WHERE r.product.id = :productId AND r.isApproved = true AND r.rating = :rating")
  Page<Review> findByProductIdAndRating(
      @Param("productId") Integer productId, @Param("rating") Integer rating, Pageable pageable);

  boolean existsByProductIdAndUserId(Integer productId, Integer userId);

  Optional<Review> findByIdAndUserId(Integer id, Integer userId);

  @Query("SELECT r FROM Review r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
  Page<Review> findByUserId(@Param("userId") Integer userId, Pageable pageable);

  @Query(
      "SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
  Double findAverageRatingByProductId(@Param("productId") Integer productId);

  @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
  Integer countApprovedByProductId(@Param("productId") Integer productId);

  @Query(
      "SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true GROUP BY r.rating")
  List<Object[]> countByProductIdGroupByRating(@Param("productId") Integer productId);
}
