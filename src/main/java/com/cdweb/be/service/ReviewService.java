package com.cdweb.be.service;

import com.cdweb.be.dto.ReviewDto;
import com.cdweb.be.entity.*;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class ReviewService {

  private static final List<Order.OrderStatus> DELIVERED_STATUSES =
      Arrays.asList(Order.OrderStatus.DELIVERED, Order.OrderStatus.COMPLETED);

  private final ReviewRepository reviewRepository;
  private final ProductRepository productRepository;
  private final UserRepository userRepository;
  private final ProductVariantRepository productVariantRepository;
  private final OrderRepository orderRepository;

  @Value("${app.server.url:http://localhost:8080}")
  private String serverUrl;

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

    Map<Integer, Integer> distribution = buildRatingDistribution(ratingCounts);

    ReviewDto.ReviewSummary summary = new ReviewDto.ReviewSummary();
    summary.setProductId(productId);
    summary.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
    summary.setTotalReviews(totalReviews != null ? totalReviews : 0);
    summary.setRatingDistribution(distribution);
    return summary;
  }

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
    return reviewRepository.findByUserId(user.getId(), pageable).map(this::mapToResponse);
  }

  @Transactional(readOnly = true)
  public ReviewDto.Response getReviewById(Integer id, String usernameOrNull) {
    Review review =
        reviewRepository
            .findByIdWithDetails(id)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));

    if (Boolean.TRUE.equals(review.getIsApproved())) {
      return mapToResponse(review);
    }

    if (usernameOrNull != null
        && review.getUser() != null
        && usernameOrNull.equals(review.getUser().getUsername())) {
      return mapToResponse(review);
    }

    throw new ResourceNotFoundException("Review", "id", id);
  }

  @Transactional(readOnly = true)
  public ReviewDto.EligibilityResponse getReviewEligibility(Integer productId, String username) {
    if (!productRepository.existsById(productId)) {
      throw new ResourceNotFoundException("Product", "id", productId);
    }

    User user = findUser(username);
    Optional<Review> existing = reviewRepository.findByProductIdAndUserId(productId, user.getId());

    ReviewDto.EligibilityResponse response = new ReviewDto.EligibilityResponse();
    response.setProductId(productId);

    if (existing.isPresent()) {
      response.setCanReview(false);
      response.setAlreadyReviewed(true);
      response.setExistingReviewId(existing.get().getId());
      response.setIsVerifiedEligible(existing.get().getIsVerifiedPurchase());
      response.setReason("ALREADY_REVIEWED");
      return response;
    }

    List<Order> orders =
        orderRepository.findUserOrdersByProduct(
            user.getId(),
            productId,
            DELIVERED_STATUSES,
            org.springframework.data.domain.PageRequest.of(0, 1));

    response.setCanReview(true);
    response.setAlreadyReviewed(false);
    response.setExistingReviewId(null);
    response.setReason(null);

    if (!orders.isEmpty()) {
      Order order = orders.get(0);
      response.setIsVerifiedEligible(true);
      response.setEligibleOrderId(order.getId());
      if (!order.getOrderDetails().isEmpty()) {
        ProductVariant variant = order.getOrderDetails().get(0).getVariant();
        if (variant != null) {
          response.setEligibleVariantId(variant.getId());
        }
      }
    } else {
      response.setIsVerifiedEligible(false);
      response.setEligibleOrderId(null);
      response.setEligibleVariantId(null);
    }

    return response;
  }

  @Transactional(readOnly = true)
  public Page<ReviewDto.ReviewableItem> getReviewableProducts(String username, Pageable pageable) {
    User user = findUser(username);
    Set<Integer> reviewedProductIds =
        reviewRepository.findByUserId(user.getId(), Pageable.unpaged()).getContent().stream()
            .map(r -> r.getProduct().getId())
            .collect(Collectors.toSet());

    List<Order> orders =
        orderRepository.findDeliveredOrdersWithDetailsForUser(user.getId(), DELIVERED_STATUSES);

    Map<Integer, ReviewDto.ReviewableItem> byProduct = new LinkedHashMap<>();
    for (Order order : orders) {
      for (OrderDetail detail : order.getOrderDetails()) {
        ProductVariant variant = detail.getVariant();
        if (variant == null || variant.getProduct() == null) {
          continue;
        }
        Integer productId = variant.getProduct().getId();
        if (reviewedProductIds.contains(productId) || byProduct.containsKey(productId)) {
          continue;
        }
        ReviewDto.ReviewableItem item = new ReviewDto.ReviewableItem();
        item.setOrderId(order.getId());
        item.setProductId(productId);
        item.setProductName(variant.getProduct().getName());
        item.setVariantId(variant.getId());
        item.setVariantName(variant.getVariantName());
        item.setImageUrl(resolveProductImageUrl(variant.getProduct()));
        item.setDeliveredAt(order.getOrderDate());
        byProduct.put(productId, item);
      }
    }

    List<ReviewDto.ReviewableItem> all = new ArrayList<>(byProduct.values());
    int start = (int) pageable.getOffset();
    int end = Math.min(start + pageable.getPageSize(), all.size());
    List<ReviewDto.ReviewableItem> pageContent =
        start >= all.size() ? List.of() : all.subList(start, end);
    return new PageImpl<>(pageContent, pageable, all.size());
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
    review.setIsApproved(false);

    if (request.getVariantId() != null) {
      ProductVariant variant =
          productVariantRepository.findById(request.getVariantId()).orElse(null);
      review.setVariant(variant);
    }

    applyVerifiedPurchase(review, user, product, request.getOrderId());

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
    review.setIsApproved(false);

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

    if (!Boolean.TRUE.equals(review.getIsApproved())) {
      throw new BadRequestException("Chỉ có thể đánh dấu hữu ích với đánh giá đã duyệt");
    }

    review.setHelpfulCount(review.getHelpfulCount() + 1);
    Review saved = reviewRepository.save(review);
    return mapToResponse(saved);
  }

  @Transactional(readOnly = true)
  public Page<ReviewDto.Response> adminSearchReviews(
      Boolean isApproved,
      Integer rating,
      Integer productId,
      String keyword,
      Boolean verifiedOnly,
      Boolean hasReply,
      LocalDate fromDate,
      LocalDate toDate,
      Pageable pageable) {

    LocalDateTime from = fromDate != null ? fromDate.atStartOfDay() : null;
    LocalDateTime to = toDate != null ? toDate.atTime(LocalTime.MAX) : null;

    Specification<Review> spec =
        ReviewSpecification.adminFilter(
            isApproved, rating, productId, keyword, verifiedOnly, hasReply, from, to);

    return reviewRepository.findAll(spec, pageable).map(this::mapToResponse);
  }

  @Transactional(readOnly = true)
  public ReviewDto.Response adminGetReviewById(Integer reviewId) {
    Review review =
        reviewRepository
            .findByIdWithDetails(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));
    return mapToResponse(review);
  }

  @Transactional(readOnly = true)
  public ReviewDto.AdminStatsResponse adminGetStats() {
    ReviewDto.AdminStatsResponse stats = new ReviewDto.AdminStatsResponse();
    stats.setTotal(reviewRepository.count());
    stats.setApproved(reviewRepository.countByIsApprovedTrue());
    stats.setPending(reviewRepository.countByIsApprovedFalse());
    stats.setHidden(0L);
    stats.setVerifiedCount(reviewRepository.countByIsVerifiedPurchaseTrue());
    stats.setUnrepliedCount(reviewRepository.countWithoutReply());

    Double avg = reviewRepository.findGlobalAverageRating();
    stats.setAverageRating(avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0);
    stats.setRatingDistribution(buildRatingDistribution(reviewRepository.countGlobalGroupByRating()));
    return stats;
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

  public List<ReviewDto.Response> adminBulkUpdateStatus(ReviewDto.BulkStatusRequest request) {
    if (request.getIds() == null || request.getIds().isEmpty()) {
      throw new BadRequestException("Danh sách ID đánh giá không được trống");
    }

    List<ReviewDto.Response> results = new ArrayList<>();
    ReviewDto.AdminUpdateStatusRequest statusRequest = new ReviewDto.AdminUpdateStatusRequest();
    statusRequest.setIsApproved(request.getIsApproved());
    for (Integer id : request.getIds()) {
      results.add(adminUpdateStatus(id, statusRequest));
    }
    return results;
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

  public ReviewDto.Response adminUpdateReply(
      Integer reviewId, ReviewDto.AdminReplyRequest request) {
    return adminReply(reviewId, request);
  }

  public ReviewDto.Response adminDeleteReply(Integer reviewId) {
    Review review =
        reviewRepository
            .findById(reviewId)
            .orElseThrow(() -> new ResourceNotFoundException("Review", "id", reviewId));

    review.setReplyContent(null);
    review.setRepliedAt(null);
    Review saved = reviewRepository.save(review);
    return mapToResponse(saved);
  }

  public void adminDeleteReview(Integer reviewId) {
    if (!reviewRepository.existsById(reviewId)) {
      throw new ResourceNotFoundException("Review", "id", reviewId);
    }
    reviewRepository.deleteById(reviewId);
  }

  @Transactional(readOnly = true)
  public Double getAverageRatingForProduct(Integer productId) {
    Double avg = reviewRepository.findAverageRatingByProductId(productId);
    return avg != null ? Math.round(avg * 10.0) / 10.0 : 0.0;
  }

  @Transactional(readOnly = true)
  public Integer getReviewCountForProduct(Integer productId) {
    Integer count = reviewRepository.countApprovedByProductId(productId);
    return count != null ? count : 0;
  }

  private void applyVerifiedPurchase(
      Review review, User user, Product product, Integer orderId) {
    if (orderId != null) {
      Order order = orderRepository.findById(orderId).orElse(null);
      if (order != null
          && order.getUser().getId().equals(user.getId())
          && DELIVERED_STATUSES.contains(order.getStatus())) {
        review.setOrder(order);
        review.setIsVerifiedPurchase(true);
      }
      return;
    }

    List<Order> orders =
        orderRepository.findUserOrdersByProduct(
            user.getId(),
            product.getId(),
            DELIVERED_STATUSES,
            org.springframework.data.domain.PageRequest.of(0, 1));
    if (!orders.isEmpty()) {
      review.setOrder(orders.get(0));
      review.setIsVerifiedPurchase(true);
    }
  }

  private Map<Integer, Integer> buildRatingDistribution(List<Object[]> ratingCounts) {
    Map<Integer, Integer> distribution = new LinkedHashMap<>();
    for (int i = 5; i >= 1; i--) {
      distribution.put(i, 0);
    }
    for (Object[] row : ratingCounts) {
      Integer star = (Integer) row[0];
      Long count = (Long) row[1];
      distribution.put(star, count.intValue());
    }
    return distribution;
  }

  private String resolveProductImageUrl(Product product) {
    if (product.getImages() == null || product.getImages().isEmpty()) {
      return null;
    }
    return serverUrl + "/img/" + product.getImages().get(0).getId();
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

    if (review.getOrder() != null) {
      response.setOrderId(review.getOrder().getId());
    }

    if (review.getUser() != null) {
      ReviewDto.ReviewUserDto userDto = new ReviewDto.ReviewUserDto();
      userDto.setId(review.getUser().getId());
      userDto.setUsername(review.getUser().getUsername());
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
