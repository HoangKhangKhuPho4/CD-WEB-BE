package com.cdweb.be.service;

import com.cdweb.be.dto.ReviewDto;
import com.cdweb.be.entity.*;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor; // Thêm để dùng Constructor Injection
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor // Xóa các @Autowired và dùng cái này để hết báo vàng
public class ReviewService {

  private final ReviewRepository reviewRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final ProductVariantRepository productVariantRepository;
  private final OrderRepository orderRepository;

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

  @Transactional(readOnly = true)
  public ReviewDto.ReviewSummary getReviewSummary(Integer productId) {
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Product", "id", productId);
    }

    Double avgRating = reviewRepository.findAverageRatingByProductId(productId);
    Integer totalReviews = reviewRepository.countApprovedByProductId(productId);
    List<Object[]> ratingCounts = reviewRepository.countByProductIdGroupByRating(productId);

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

  /** Đánh giá đã duyệt mới nhất — dùng cho trang chủ. */
  @Transactional(readOnly = true)
  public List<ReviewDto.Response> getRecentApprovedReviews(int size) {
    int limit = Math.min(Math.max(size, 1), 20);
    return reviewRepository
        .findByIsApprovedTrueOrderByCreatedAtDesc(
            org.springframework.data.domain.PageRequest.of(0, limit))
        .map(this::mapToResponse)
        .getContent();
  }

  @Transactional(readOnly = true)
  public Page<ReviewDto.Response> getMyReviews(String username, Pageable pageable) {
    User user = findUser(username);
    // Repository giờ đã nhận Long userId
    return reviewRepository.findByUserId(user.getId(), pageable).map(this::mapToResponse);
  }

  public ReviewDto.Response createReview(ReviewDto.CreateRequest request, String username) {
    User user = findUser(username);

    Product product =
            productRepository
                    .findById(request.getProductId())
                    .orElseThrow(
                            () -> new ResourceNotFoundException("Product", "id", request.getProductId()));

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
    review.setIsApproved(true);

    if (request.getVariantId() != null) {
      ProductVariant variant =
              productVariantRepository.findById(request.getVariantId()).orElse(null);
      review.setVariant(variant);
    }

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

  public void deleteReview(Integer reviewId, String username) {
    User user = findUser(username);
    Review review =
            reviewRepository
                    .findByIdAndUserId(reviewId, user.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    reviewRepository.delete(review);
  }

  public ReviewDto.Response markHelpful(Integer reviewId) {
    Review review =
            reviewRepository
                    .findById(reviewId)
                    .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

    review.setHelpfulCount(review.getHelpfulCount() + 1);
    Review saved = reviewRepository.save(review);
    return mapToResponse(saved);
  }

  @Transactional(readOnly = true)
  public Page<ReviewDto.Response> adminGetAllReviews(Pageable pageable) {
    return reviewRepository.findAll(pageable).map(this::mapToResponse);
  }

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

  public void adminDeleteReview(Integer reviewId) {
    if (!reviewRepository.existsById(reviewId)) {
      throw new ResourceNotFoundException("Review", "id", reviewId);
    }
    reviewRepository.deleteById(reviewId);
  }

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

    if (review.getProduct() != null) {
      response.setProductId(review.getProduct().getId());
      response.setProductName(review.getProduct().getName());
    }

    if (review.getVariant() != null) {
      response.setVariantId(review.getVariant().getId());
      response.setVariantName(review.getVariant().getVariantName());
    }

    if (review.getUser() != null) {
      ReviewDto.ReviewUserDto userDto = new ReviewDto.ReviewUserDto();
      userDto.setId(review.getUser().getId());
      userDto.setUsername(review.getUser().getUsername());
      // Đã sửa: getName() -> getFullName()
      userDto.setName(review.getUser().getFullName());
      response.setUser(userDto);
    }

    List<ReviewImage> imgs = review.getReviewImages();
    if (imgs != null && !imgs.isEmpty()) {
      response.setImages(imgs.stream().map(ReviewImage::getImageUrl).collect(Collectors.toList()));
    } else {
      response.setImages(Collections.emptyList());
    }

    return response;
  }
}