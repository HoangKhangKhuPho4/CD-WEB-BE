package com.cdweb.be.service;

import com.cdweb.be.dto.CouponDto;
import com.cdweb.be.entity.Coupon;
import com.cdweb.be.entity.Order;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponService {

  Page<CouponDto.Response> getAllCoupons(
      String keyword,
      Boolean isActive,
      String discountType,
      String lifecycle,
      Pageable pageable);

  CouponDto.AdminStatsResponse getAdminStats();

  CouponDto.Response getCouponById(Integer id);

  CouponDto.Response getCouponByCode(String code);

  CouponDto.Response createCoupon(CouponDto.CreateRequest request);

  CouponDto.Response updateCoupon(Integer id, CouponDto.UpdateRequest request);

  void deleteCoupon(Integer id);

  void hardDeleteCoupon(Integer id);

  CouponDto.Response toggleCouponStatus(Integer id);

  List<CouponDto.Response> bulkUpdateStatus(CouponDto.BulkStatusRequest request);

  List<CouponDto.PublicResponse> getAvailableCoupons();

  /** Legacy — admin form sản phẩm. */
  List<CouponDto.Response> getActiveCoupons();

  CouponDto.ValidateResponse validateCoupon(CouponDto.ValidateRequest request);

  CouponDto.ValidateResponse validateForCheckout(
      String couponCode,
      Long userId,
      BigDecimal orderTotal,
      List<CouponDto.CheckoutLineItem> lineItems);

  BigDecimal calculateDiscount(Coupon coupon, BigDecimal orderTotal);

  Page<CouponDto.UsageOrderSummary> getCouponUsageOrders(String couponCode, Pageable pageable);

  void incrementUsage(String couponCode);

  void decrementUsage(String couponCode);
}
