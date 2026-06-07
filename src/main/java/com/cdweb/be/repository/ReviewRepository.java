package com.cdweb.be.repository;

import com.cdweb.be.entity.Review;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReviewRepository
    extends JpaRepository<Review, Integer>, JpaSpecificationExecutor<Review> {

  Page<Review> findByProductIdAndIsApproved(
      Integer productId, Boolean isApproved, Pageable pageable);

  @Query(
      "SELECT r FROM Review r WHERE r.product.id = :productId AND r.isApproved = true AND r.rating = :rating")
  Page<Review> findByProductIdAndRating(
      @Param("productId") Integer productId, @Param("rating") Integer rating, Pageable pageable);

  boolean existsByProductIdAndUserId(Integer productId, Long userId);

  Optional<Review> findByProductIdAndUserId(Integer productId, Long userId);

  Optional<Review> findByIdAndUserId(Integer id, Long userId);

  @Query("SELECT r FROM Review r WHERE r.user.id = :userId ORDER BY r.createdAt DESC")
  Page<Review> findByUserId(@Param("userId") Long userId, Pageable pageable);

  @Query(
      "SELECT AVG(r.rating) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
  Double findAverageRatingByProductId(@Param("productId") Integer productId);

  @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true")
  Integer countApprovedByProductId(@Param("productId") Integer productId);

  @Query(
      "SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id = :productId AND r.isApproved = true GROUP BY r.rating")
  List<Object[]> countByProductIdGroupByRating(@Param("productId") Integer productId);

  Page<Review> findByIsApprovedTrueOrderByCreatedAtDesc(Pageable pageable);

  @Query(
      "SELECT r FROM Review r "
          + "LEFT JOIN FETCH r.product "
          + "LEFT JOIN FETCH r.user "
          + "LEFT JOIN FETCH r.variant "
          + "LEFT JOIN FETCH r.reviewImages "
          + "WHERE r.id = :id")
  Optional<Review> findByIdWithDetails(@Param("id") Integer id);

  long countByIsApprovedTrue();

  long countByIsApprovedFalse();

  long countByIsVerifiedPurchaseTrue();

  @Query(
      "SELECT COUNT(r) FROM Review r WHERE r.replyContent IS NULL OR TRIM(r.replyContent) = ''")
  long countWithoutReply();

  @Query("SELECT AVG(r.rating) FROM Review r WHERE r.isApproved = true")
  Double findGlobalAverageRating();

  @Query(
      "SELECT r.rating, COUNT(r) FROM Review r WHERE r.isApproved = true GROUP BY r.rating ORDER BY r.rating DESC")
  List<Object[]> countGlobalGroupByRating();
}
