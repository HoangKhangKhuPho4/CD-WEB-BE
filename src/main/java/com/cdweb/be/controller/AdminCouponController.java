package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.CouponDto;
import com.cdweb.be.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin API — Quản lý mã giảm giá (đủ scenario kiểm thử). */
@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'ROLE_ADMIN')")
@CrossOrigin(origins = "*")
public class AdminCouponController {

  @Autowired private CouponService couponService;

  @GetMapping
  public ResponseEntity<ApiResponse<Page<CouponDto.Response>>> getAllCoupons(
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "isActive", required = false) Boolean isActive,
      @RequestParam(value = "discountType", required = false) String discountType,
      @RequestParam(value = "lifecycle", required = false) String lifecycle,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
      @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {

    Sort sort =
        sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<CouponDto.Response> coupons =
        couponService.getAllCoupons(keyword, isActive, discountType, lifecycle, pageable);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách coupon thành công", coupons));
  }

  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<CouponDto.AdminStatsResponse>> getStats() {
    return ResponseEntity.ok(
        ApiResponse.success("Lấy thống kê coupon thành công", couponService.getAdminStats()));
  }

  @GetMapping("/{id:\\d+}")
  public ResponseEntity<ApiResponse<CouponDto.Response>> getCouponById(@PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Lấy chi tiết coupon thành công", couponService.getCouponById(id)));
  }

  @GetMapping("/code/{code}")
  public ResponseEntity<ApiResponse<CouponDto.Response>> getCouponByCode(
      @PathVariable String code) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Lấy coupon theo mã thành công", couponService.getCouponByCode(code)));
  }

  @GetMapping("/{id:\\d+}/orders")
  public ResponseEntity<ApiResponse<Page<CouponDto.UsageOrderSummary>>> getCouponOrders(
      @PathVariable Integer id,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size) {
    CouponDto.Response coupon = couponService.getCouponById(id);
    Pageable pageable = PageRequest.of(page, size, Sort.by("orderDate").descending());
    Page<CouponDto.UsageOrderSummary> orders =
        couponService.getCouponUsageOrders(coupon.getCode(), pageable);
    return ResponseEntity.ok(
        ApiResponse.success("Lấy lịch sử sử dụng coupon thành công", orders));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CouponDto.Response>> createCoupon(
      @Valid @RequestBody CouponDto.CreateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Thêm mới coupon thành công", couponService.createCoupon(request)));
  }

  @PutMapping("/{id:\\d+}")
  public ResponseEntity<ApiResponse<CouponDto.Response>> updateCoupon(
      @PathVariable Integer id, @Valid @RequestBody CouponDto.UpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật coupon thành công", couponService.updateCoupon(id, request)));
  }

  @PatchMapping("/{id:\\d+}/toggle")
  public ResponseEntity<ApiResponse<CouponDto.Response>> toggleCoupon(@PathVariable Integer id) {
    CouponDto.Response coupon = couponService.toggleCouponStatus(id);
    String status = Boolean.TRUE.equals(coupon.getIsActive()) ? "kích hoạt" : "vô hiệu hóa";
    return ResponseEntity.ok(ApiResponse.success("Mã coupon đã được " + status, coupon));
  }

  @PatchMapping("/bulk-status")
  public ResponseEntity<ApiResponse<List<CouponDto.Response>>> bulkUpdateStatus(
      @Valid @RequestBody CouponDto.BulkStatusRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Cập nhật trạng thái hàng loạt thành công",
            couponService.bulkUpdateStatus(request)));
  }

  @PostMapping("/validate")
  public ResponseEntity<ApiResponse<CouponDto.ValidateResponse>> validateCoupon(
      @Valid @RequestBody CouponDto.ValidateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Kiểm tra mã coupon thành công", couponService.validateCoupon(request)));
  }

  @DeleteMapping("/{id:\\d+}")
  public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Integer id) {
    couponService.deleteCoupon(id);
    return ResponseEntity.ok(
        ApiResponse.success("Xóa coupon thành công (đã chuyển về Inactive)", null));
  }

  @DeleteMapping("/{id:\\d+}/hard")
  public ResponseEntity<ApiResponse<Void>> hardDeleteCoupon(@PathVariable Integer id) {
    couponService.hardDeleteCoupon(id);
    return ResponseEntity.ok(ApiResponse.success("Xóa cứng coupon thành công", null));
  }

  /** Legacy — danh sách coupon đang khả dụng cho form sản phẩm. */
  @GetMapping("/active")
  public ResponseEntity<ApiResponse<List<CouponDto.Response>>> getActiveCoupons() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Lấy danh sách coupon active thành công", couponService.getActiveCoupons()));
  }
}
