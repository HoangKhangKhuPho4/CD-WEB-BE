package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.CouponDto;
import com.cdweb.be.service.CouponService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public API - Cho người dùng xem danh sách mã giảm giá */
@RestController
@RequestMapping("/api/coupons")
@CrossOrigin(origins = "*")
public class CouponController {

  @Autowired private CouponService couponService;

  // ─── GET /api/coupons — Danh sách mã giảm giá đang hoạt động ────────────────
  @GetMapping
  public ResponseEntity<ApiResponse<List<CouponDto.Response>>> getAvailableCoupons() {
    List<CouponDto.Response> coupons = couponService.getActiveCoupons();
    return ResponseEntity.ok(
        ApiResponse.success("Lấy danh sách mã giảm giá khả dụng thành công", coupons));
  }
}
