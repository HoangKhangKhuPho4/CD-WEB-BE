package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ReviewDto;
import com.cdweb.be.service.ReviewService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

  @Autowired private ReviewService reviewService;

  // ─── GET /api/reviews?product_id=1 — Danh sách đánh giá theo sản phẩm ───
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ReviewDto.Response>>> getProductReviews(
      @RequestParam(name = "product_id") Integer productId,
      @RequestParam(required = false) Integer rating,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sort,
      @RequestParam(defaultValue = "desc") String direction) {

    Sort.Direction dir =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sort));
    Page<ReviewDto.Response> reviews = reviewService.getProductReviews(productId, rating, pageable);
    return ResponseEntity.ok(ApiResponse.success("Reviews retrieved successfully", reviews));
  }

  // ─── GET /api/reviews/summary?product_id=1 — Thống kê đánh giá ──────────
  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<ReviewDto.ReviewSummary>> getReviewSummary(
      @RequestParam(name = "product_id") Integer productId) {

    ReviewDto.ReviewSummary summary = reviewService.getReviewSummary(productId);
    return ResponseEntity.ok(ApiResponse.success("Review summary retrieved successfully", summary));
  }

  // ─── GET /api/reviews/recent — Đánh giá mới (trang chủ) ───────────────────
  @GetMapping("/recent")
  public ResponseEntity<ApiResponse<List<ReviewDto.Response>>> getRecentReviews(
      @RequestParam(defaultValue = "8") int size) {
    List<ReviewDto.Response> reviews = reviewService.getRecentApprovedReviews(size);
    return ResponseEntity.ok(ApiResponse.success("Recent reviews retrieved successfully", reviews));
  }

  // ─── GET /api/reviews/my — Đánh giá của tôi ─────────────────────────────
  @GetMapping("/my")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Page<ReviewDto.Response>>> getMyReviews(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
    Page<ReviewDto.Response> reviews = reviewService.getMyReviews(username, pageable);
    return ResponseEntity.ok(ApiResponse.success("My reviews retrieved successfully", reviews));
  }

  // ─── POST /api/reviews — Tạo đánh giá mới ──────────────────────────────
  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> createReview(
      @Valid @RequestBody ReviewDto.CreateRequest request) {

    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    ReviewDto.Response review = reviewService.createReview(request, username);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Review created successfully", review));
  }

  // ─── PUT /api/reviews/{id} — Cập nhật đánh giá của mình ────────────────
  @PutMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> updateReview(
      @PathVariable Integer id, @Valid @RequestBody ReviewDto.UpdateRequest request) {

    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    ReviewDto.Response review = reviewService.updateReview(id, request, username);
    return ResponseEntity.ok(ApiResponse.success("Review updated successfully", review));
  }

  // ─── DELETE /api/reviews/{id} — Xóa đánh giá của mình ──────────────────
  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Integer id) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    reviewService.deleteReview(id, username);
    return ResponseEntity.ok(ApiResponse.success("Review deleted successfully", null));
  }

  // ─── POST /api/reviews/{id}/helpful — Đánh dấu hữu ích ────────────────
  @PostMapping("/{id}/helpful")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> markHelpful(@PathVariable Integer id) {
    ReviewDto.Response review = reviewService.markHelpful(id);
    return ResponseEntity.ok(ApiResponse.success("Marked as helpful", review));
  }
}
