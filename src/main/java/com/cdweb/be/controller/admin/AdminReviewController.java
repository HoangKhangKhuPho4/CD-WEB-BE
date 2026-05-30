package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ReviewDto;
import com.cdweb.be.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Admin Review Controller — Quản lý đánh giá/bình luận sản phẩm.
 *
 * <h3>Quyền hạn:</h3>
 *
 * <ul>
 *   <li>REVIEW_MANAGE: Xem tất cả, duyệt, ẩn, xóa đánh giá
 *   <li>REVIEW_REPLY: Trả lời phản hồi khách hàng
 * </ul>
 */
@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
public class AdminReviewController {

  @Autowired private ReviewService reviewService;

  // ─── GET /api/admin/reviews — Danh sách toàn bộ đánh giá ────────────────
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ReviewDto.Response>>> getAllReviews(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String direction) {

    Sort.Direction dir =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

    Page<ReviewDto.Response> reviews = reviewService.adminGetAllReviews(pageable);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", reviews));
  }

  // ─── PUT /api/admin/reviews/{id}/status — Duyệt hoặc Ẩn đánh giá ─────────
  @PutMapping("/{id}/status")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> updateStatus(
      @PathVariable Integer id, @Valid @RequestBody ReviewDto.AdminUpdateStatusRequest request) {

    ReviewDto.Response response = reviewService.adminUpdateStatus(id, request);
    String msg = request.getIsApproved() ? "Đã duyệt đánh giá" : "Đã ẩn đánh giá";
    return ResponseEntity.ok(ApiResponse.success(msg, response));
  }

  // ─── POST /api/admin/reviews/{id}/reply — Phản hồi khách hàng ───────────
  @PostMapping("/{id}/reply")
  @PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> replyReview(
      @PathVariable Integer id, @Valid @RequestBody ReviewDto.AdminReplyRequest request) {

    ReviewDto.Response response = reviewService.adminReply(id, request);
    return ResponseEntity.ok(ApiResponse.success("Đã gửi phản hồi thành công", response));
  }

  // ─── DELETE /api/admin/reviews/{id} — Xóa vĩnh viễn đánh giá ────────────
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Integer id) {
    reviewService.adminDeleteReview(id);
    return ResponseEntity.ok(ApiResponse.success("Đã xóa đánh giá vĩnh viễn", null));
  }
}
