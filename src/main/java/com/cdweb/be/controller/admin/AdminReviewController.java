package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ReviewDto;
import com.cdweb.be.service.ReviewService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/reviews")
@PreAuthorize(
    "hasAnyAuthority('REVIEW_MANAGE', 'REVIEW_REPLY', 'PRODUCT_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
public class AdminReviewController {

  @Autowired private ReviewService reviewService;

  @GetMapping
  public ResponseEntity<ApiResponse<Page<ReviewDto.Response>>> searchReviews(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String direction,
      @RequestParam(required = false) Boolean isApproved,
      @RequestParam(required = false) Integer rating,
      @RequestParam(required = false) Integer productId,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean verifiedOnly,
      @RequestParam(required = false) Boolean hasReply,
      @RequestParam(required = false) LocalDate fromDate,
      @RequestParam(required = false) LocalDate toDate) {

    Sort.Direction dir =
        "asc".equalsIgnoreCase(direction) ? Sort.Direction.ASC : Sort.Direction.DESC;
    Pageable pageable = PageRequest.of(page, size, Sort.by(dir, sortBy));

    Page<ReviewDto.Response> reviews =
        reviewService.adminSearchReviews(
            isApproved,
            rating,
            productId,
            keyword,
            verifiedOnly,
            hasReply,
            fromDate,
            toDate,
            pageable);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách đánh giá thành công", reviews));
  }

  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<ReviewDto.AdminStatsResponse>> getStats() {
    return ResponseEntity.ok(
        ApiResponse.success("Lấy thống kê đánh giá thành công", reviewService.adminGetStats()));
  }

  @PatchMapping("/bulk-status")
  @PreAuthorize(
      "hasAnyAuthority('REVIEW_MANAGE', 'PRODUCT_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<ReviewDto.Response>>> bulkUpdateStatus(
      @Valid @RequestBody ReviewDto.BulkStatusRequest request) {
    List<ReviewDto.Response> updated = reviewService.adminBulkUpdateStatus(request);
    return ResponseEntity.ok(
        ApiResponse.success("Đã cập nhật trạng thái " + updated.size() + " đánh giá", updated));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> getReviewById(@PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Lấy chi tiết đánh giá thành công", reviewService.adminGetReviewById(id)));
  }

  @PutMapping("/{id}/status")
  @PreAuthorize(
      "hasAnyAuthority('REVIEW_MANAGE', 'PRODUCT_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> updateStatus(
      @PathVariable Integer id, @Valid @RequestBody ReviewDto.AdminUpdateStatusRequest request) {

    ReviewDto.Response response = reviewService.adminUpdateStatus(id, request);
    String msg = request.getIsApproved() ? "Đã duyệt đánh giá" : "Đã ẩn đánh giá";
    return ResponseEntity.ok(ApiResponse.success(msg, response));
  }

  @PostMapping("/{id}/reply")
  @PreAuthorize(
      "hasAnyAuthority('REVIEW_REPLY', 'REVIEW_MANAGE', 'PRODUCT_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> replyReview(
      @PathVariable Integer id, @Valid @RequestBody ReviewDto.AdminReplyRequest request) {

    ReviewDto.Response response = reviewService.adminReply(id, request);
    return ResponseEntity.ok(ApiResponse.success("Đã gửi phản hồi thành công", response));
  }

  @PutMapping("/{id}/reply")
  @PreAuthorize(
      "hasAnyAuthority('REVIEW_REPLY', 'REVIEW_MANAGE', 'PRODUCT_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> updateReply(
      @PathVariable Integer id, @Valid @RequestBody ReviewDto.AdminReplyRequest request) {

    ReviewDto.Response response = reviewService.adminUpdateReply(id, request);
    return ResponseEntity.ok(ApiResponse.success("Đã cập nhật phản hồi thành công", response));
  }

  @DeleteMapping("/{id}/reply")
  @PreAuthorize(
      "hasAnyAuthority('REVIEW_REPLY', 'REVIEW_MANAGE', 'PRODUCT_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<ReviewDto.Response>> deleteReply(@PathVariable Integer id) {
    ReviewDto.Response response = reviewService.adminDeleteReply(id);
    return ResponseEntity.ok(ApiResponse.success("Đã xóa phản hồi thành công", response));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize(
      "hasAnyAuthority('REVIEW_MANAGE', 'PRODUCT_MANAGE', 'WARRANTY_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Void>> deleteReview(@PathVariable Integer id) {
    reviewService.adminDeleteReview(id);
    return ResponseEntity.ok(ApiResponse.success("Đã xóa đánh giá vĩnh viễn", null));
  }
}
