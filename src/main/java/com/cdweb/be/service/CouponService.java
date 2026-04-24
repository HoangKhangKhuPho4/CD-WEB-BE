package com.cdweb.be.service;

import com.cdweb.be.dto.CouponDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CouponService {
  Page<CouponDto.Response> getAllCoupons(String keyword, Boolean isActive, Pageable pageable);

  CouponDto.Response getCouponById(Integer id);

  CouponDto.Response createCoupon(CouponDto.CreateRequest request);

  CouponDto.Response updateCoupon(Integer id, CouponDto.UpdateRequest request);

  void deleteCoupon(Integer id);

  CouponDto.Response toggleActive(Integer id);

  List<CouponDto.Response> getActiveCoupons();
}
