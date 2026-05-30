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

/** Admin API - Quản lý mã giảm giá (Vouchers/Coupons) */
@RestController
@RequestMapping("/api/admin/coupons")
@PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'ROLE_ADMIN')")
@CrossOrigin(origins = "*")
public class AdminCouponController {

  @Autowired private CouponService couponService;

  // ─── GET /api/admin/coupons — Danh sách coupon (có phân trang & tìm kiếm) ───
  @GetMapping
  public ResponseEntity<ApiResponse<Page<CouponDto.Response>>> getAllCoupons(
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "isActive", required = false) Boolean isActive,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
      @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {

    Sort sort =
        sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<CouponDto.Response> coupons = couponService.getAllCoupons(keyword, isActive, pageable);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách coupon thành công", coupons));
  }

  // ─── GET /api/admin/coupons/{id} — Chi tiết coupon ─────────────────────
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CouponDto.Response>> getCouponById(@PathVariable Integer id) {
    CouponDto.Response coupon = couponService.getCouponById(id);
    return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết coupon thành công", coupon));
  }

  // ─── POST /api/admin/coupons — Thêm mới coupon ──────────────────────────
  @PostMapping
  public ResponseEntity<ApiResponse<CouponDto.Response>> createCoupon(
      @Valid @RequestBody CouponDto.CreateRequest request) {
    CouponDto.Response coupon = couponService.createCoupon(request);
    return ResponseEntity.ok(ApiResponse.success("Thêm mới coupon thành công", coupon));
  }

  // ─── PUT /api/admin/coupons/{id} — Cập nhật coupon ──────────────────────
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<CouponDto.Response>> updateCoupon(
      @PathVariable Integer id, @Valid @RequestBody CouponDto.UpdateRequest request) {
    CouponDto.Response coupon = couponService.updateCoupon(id, request);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật coupon thành công", coupon));
  }

  // ─── patch /api/admin/coupons/{id}/toggle — Ẩn/Hiện coupon (Toggle Active) ─
  @PatchMapping("/{id}/toggle")
  public ResponseEntity<ApiResponse<CouponDto.Response>> toggleCoupon(@PathVariable Integer id) {
    CouponDto.Response coupon = couponService.toggleActive(id);
    String status = coupon.getIsActive() ? "kích hoạt" : "vô hiệu hóa";
    return ResponseEntity.ok(ApiResponse.success("Mã coupon đã được " + status, coupon));
  }

  // ─── DELETE /api/admin/coupons/{id} — Xóa coupon (Vô hiệu hóa) ─────────────
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteCoupon(@PathVariable Integer id) {
    couponService.deleteCoupon(id);
    return ResponseEntity.ok(
        ApiResponse.success("Xóa coupon thành công (Đã chuyển về trạng thái Inactive)", null));
  }

  /** Legacy API: Trả về danh sách coupon đang active cho form sản phẩm. */
  @GetMapping("/active")
  public ResponseEntity<ApiResponse<List<CouponDto.Response>>> getActiveCoupons() {
    List<CouponDto.Response> coupons = couponService.getActiveCoupons();
    return ResponseEntity.ok(
        ApiResponse.success("Lấy danh sách coupon active thành công", coupons));
  }
}
