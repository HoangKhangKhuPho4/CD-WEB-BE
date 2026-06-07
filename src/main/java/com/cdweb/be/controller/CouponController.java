package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.CouponDto;
import com.cdweb.be.service.CouponService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public API — Mã giảm giá cho khách hàng. */
@RestController
@RequestMapping("/api/coupons")
@CrossOrigin(origins = "*")
public class CouponController {

  @Autowired private CouponService couponService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<CouponDto.PublicResponse>>> getAvailableCoupons() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Lấy danh sách mã giảm giá khả dụng thành công",
            couponService.getAvailableCoupons()));
  }

  @GetMapping("/{code}")
  public ResponseEntity<ApiResponse<CouponDto.PublicResponse>> getCouponByCode(
      @PathVariable String code) {
    CouponDto.Response full = couponService.getCouponByCode(code);
    CouponDto.PublicResponse pub = new CouponDto.PublicResponse();
    pub.setCode(full.getCode());
    pub.setName(full.getName());
    pub.setDescription(full.getDescription());
    pub.setDiscountType(full.getDiscountType());
    pub.setDiscountValue(full.getDiscountValue());
    pub.setMinOrderValue(full.getMinOrderValue());
    pub.setMaxDiscountAmount(full.getMaxDiscountAmount());
    pub.setDateEnd(full.getDateEnd());
    return ResponseEntity.ok(ApiResponse.success("Lấy thông tin mã giảm giá thành công", pub));
  }

  @PostMapping("/validate")
  public ResponseEntity<ApiResponse<CouponDto.ValidateResponse>> validateCoupon(
      @Valid @RequestBody CouponDto.ValidateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Kiểm tra mã giảm giá thành công", couponService.validateCoupon(request)));
  }
}
