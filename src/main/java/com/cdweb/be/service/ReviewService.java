package com.cdweb.be.service;

import com.cdweb.be.dto.ReviewDto;
import com.cdweb.be.entity.*;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ReviewService {

  @Autowired private ReviewRepository reviewRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private UserRepository userRepository;

  @Autowired private ProductVariantRepository productVariantRepository;

  @Autowired private OrderRepository orderRepository;

  // ═══════════════════════════════════════════════════════════════════════════
  // GET REVIEWS — lấy danh sách đánh giá theo product_id
  // ═══════════════════════════════════════════════════════════════════════════
  @Transactional(readOnly = true)
  public Page<ReviewDto.Response> getProductReviews(
      Integer productId, Integer rating, Pageable pageable) {
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Product", "id", productId);
    }

    Page<Review> reviews;
    if (rating != null && rating >= 1 && rating <= 5) {
      reviews = reviewRepository.findByProductIdAndRating(productId, rating, pageable);
    } else {
      reviews = reviewRepository.findByProductIdAndIsApproved(productId, true, pageable);
    }

    return reviews.map(this::mapToResponse);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GET REVIEW SUMMARY — thống kê đánh giá sản phẩm
  // ═══════════════════════════════════════════════════════════════════════════
  @Transactional(readOnly = true)
  public ReviewDto.ReviewSummary getReviewSummary(Integer productId) {
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Product", "id", productId);
    }

    Double avgRating = reviewRepository.findAverageRatingByProductId(productId);
    Integer totalReviews = reviewRepository.countApprovedByProductId(productId);
    List<Object[]> ratingCounts = reviewRepository.countByProductIdGroupByRating(productId);

    // Xây dựng phân bố 1→5 sao
    Map<Integer, Integer> distribution = new LinkedHashMap<>();
    for (int i = 5; i >= 1; i--) {
      distribution.put(i, 0);
    }
    for (Object[] row : ratingCounts) {
      Integer star = (Integer) row[0];
      Long count = (Long) row[1];
      distribution.put(star, count.intValue());
    }

    ReviewDto.ReviewSummary summary = new ReviewDto.ReviewSummary();
    summary.setProductId(productId);
    summary.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
    summary.setTotalReviews(totalReviews != null ? totalReviews : 0);
    summary.setRatingDistribution(distribution);
    return summary;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // GET MY REVIEWS — xem đánh giá của chính mình
  // ═══════════════════════════════════════════════════════════════════════════
  @Transactional(readOnly = true)
  public Page<ReviewDto.Response> getMyReviews(String username, Pageable pageable) {
    User user = findUser(username);
    return reviewRepository.findByUserId(user.getId(), pageable).map(this::mapToResponse);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CREATE REVIEW — tạo đánh giá mới
  // ═══════════════════════════════════════════════════════════════════════════
  public ReviewDto.Response createReview(ReviewDto.CreateRequest request, String username) {
    User user = findUser(username);

    Product product =
        productRepository
            .findById(request.getProductId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Product", "id", request.getProductId()));

    // Kiểm tra đã đánh giá chưa
    if (reviewRepository.existsByProductIdAndUserId(request.getProductId(), user.getId())) {
      throw new BadRequestException("Bạn đã đánh giá sản phẩm này rồi");
    }

    Review review = new Review();
    review.setProduct(product);
    review.setUser(user);
    review.setRating(request.getRating());
    review.setTitle(request.getTitle());
    review.setContent(request.getContent());
    review.setPros(request.getPros());
    review.setCons(request.getCons());
    review.setIsApproved(true); // auto-approve

    // Variant (tùy chọn)
    if (request.getVariantId() != null) {
      ProductVariant variant =
          productVariantRepository.findById(request.getVariantId()).orElse(null);
      review.setVariant(variant);
    }

    // Order (tùy chọn) + kiểm tra verified purchase
    if (request.getOrderId() != null) {
      Order order = orderRepository.findById(request.getOrderId()).orElse(null);
      if (order != null
          && order.getUser().getId().equals(user.getId())
          && (order.getStatus() == Order.OrderStatus.DELIVERED
              || order.getStatus() == Order.OrderStatus.COMPLETED)) {
        review.setOrder(order);
        review.setIsVerifiedPurchase(true);
      }
    } else {
      // Tự động kiểm tra lịch sử mua hàng nếu FE không truyền orderId
      List<Order> orders =
          orderRepository.findUserOrdersByProduct(
              user.getId(),
              product.getId(),
              Arrays.asList(Order.OrderStatus.DELIVERED, Order.OrderStatus.COMPLETED),
              org.springframework.data.domain.PageRequest.of(0, 1));
      if (!orders.isEmpty()) {
        review.setOrder(orders.get(0));
        review.setIsVerifiedPurchase(true);
      }
    }

    // Hình ảnh đính kèm (Bổ sung mới 🆕)
    if (request.getImages() != null && !request.getImages().isEmpty()) {
      List<ReviewImage> reviewImages =
          request.getImages().stream()
              .filter(url -> url != null && !url.trim().isEmpty())
              .map(
                  url -> {
                    ReviewImage img = new ReviewImage();
                    img.setImageUrl(url);
                    img.setReview(review);
                    return img;
                  })
              .collect(Collectors.toList());
      review.setReviewImages(reviewImages);
    }

    Review saved = reviewRepository.save(review);
    return mapToResponse(saved);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // UPDATE REVIEW — cập nhật đánh giá của mình
  // ═══════════════════════════════════════════════════════════════════════════
  public ReviewDto.Response updateReview(
      Integer reviewId, ReviewDto.UpdateRequest request, String username) {
    User user = findUser(username);

    Review review =
        reviewRepository
            .findByIdAndUserId(reviewId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

    review.setRating(request.getRating());
    review.setTitle(request.getTitle());
    review.setContent(request.getContent());
    review.setPros(request.getPros());
    review.setCons(request.getCons());

    // Cập nhật hình ảnh (Bổ sung mới 🆕)
    if (request.getImages() != null) {
      if (review.getReviewImages() == null) {
        review.setReviewImages(new ArrayList<>());
      } else {
        review.getReviewImages().clear();
      }
      List<ReviewImage> newImages =
          request.getImages().stream()
              .filter(url -> url != null && !url.trim().isEmpty())
              .map(
                  url -> {
                    ReviewImage img = new ReviewImage();
                    img.setImageUrl(url);
                    img.setReview(review);
                    return img;
                  })
              .collect(Collectors.toList());
      review.getReviewImages().addAll(newImages);
    }

    Review saved = reviewRepository.save(review);
    return mapToResponse(saved);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DELETE REVIEW — xóa đánh giá của mình
  // ═══════════════════════════════════════════════════════════════════════════
  public void deleteReview(Integer reviewId, String username) {
    User user = findUser(username);

    Review review =
        reviewRepository
            .findByIdAndUserId(reviewId, user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

    reviewRepository.delete(review);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPFUL — tăng lượt hữu ích
  // ═══════════════════════════════════════════════════════════════════════════
  public ReviewDto.Response markHelpful(Integer reviewId) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

    review.setHelpfulCount(review.getHelpfulCount() + 1);
    Review saved = reviewRepository.save(review);
    return mapToResponse(saved);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // ADMIN — Quản lý đánh giá
  // ═══════════════════════════════════════════════════════════════════════════

  /** Admin lấy toàn bộ đánh giá (phân trang) */
  @Transactional(readOnly = true)
  public Page<ReviewDto.Response> adminGetAllReviews(Pageable pageable) {
    return reviewRepository.findAll(pageable).map(this::mapToResponse);
  }

  /** Admin duyệt hoặc ẩn đánh giá */
  public ReviewDto.Response adminUpdateStatus(
      Integer reviewId, ReviewDto.AdminUpdateStatusRequest request) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

    review.setIsApproved(request.getIsApproved());
    Review saved = reviewRepository.save(review);
    return mapToResponse(saved);
  }

  /** Admin/Shop phản hồi đánh giá */
  public ReviewDto.Response adminReply(Integer reviewId, ReviewDto.AdminReplyRequest request) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

    review.setReplyContent(request.getReplyContent());
    review.setRepliedAt(LocalDateTime.now());
    Review saved = reviewRepository.save(review);
    return mapToResponse(saved);
  }

  /** Admin xóa vĩnh viễn đánh giá */
  public void adminDeleteReview(Integer reviewId) {
    if (!reviewRepository.existsById(reviewId)) {
      throw new ResourceNotFoundException("Review", "id", reviewId);
    }
    reviewRepository.deleteById(reviewId);
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  private User findUser(String username) {
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
  }

  private ReviewDto.Response mapToResponse(Review review) {
    ReviewDto.Response response = new ReviewDto.Response();
    response.setId(review.getId());
    response.setRating(review.getRating());
    response.setTitle(review.getTitle());
    response.setContent(review.getContent());
    response.setPros(review.getPros());
    response.setCons(review.getCons());
    response.setIsVerifiedPurchase(review.getIsVerifiedPurchase());
    response.setIsApproved(review.getIsApproved());
    response.setHelpfulCount(review.getHelpfulCount());
    response.setCreatedAt(review.getCreatedAt());
    response.setUpdatedAt(review.getUpdatedAt());
    response.setReplyContent(review.getReplyContent());
    response.setRepliedAt(review.getRepliedAt());

    // Product info
    if (review.getProduct() != null) {
      response.setProductId(review.getProduct().getId());
      response.setProductName(review.getProduct().getName());
    }

    // Variant info
    if (review.getVariant() != null) {
      response.setVariantId(review.getVariant().getId());
      response.setVariantName(review.getVariant().getVariantName());
    }

    // User info
    if (review.getUser() != null) {
      ReviewDto.ReviewUserDto userDto = new ReviewDto.ReviewUserDto();
      userDto.setId(review.getUser().getId());
      userDto.setUsername(review.getUser().getUsername());
      userDto.setName(review.getUser().getName());
      response.setUser(userDto);
    }

    // Images
    List<ReviewImage> imgs = review.getReviewImages();
    if (imgs != null && !imgs.isEmpty()) {
      response.setImages(imgs.stream().map(ReviewImage::getImageUrl).collect(Collectors.toList()));
    } else {
      response.setImages(Collections.emptyList());
    }

    return response;
  }
}
