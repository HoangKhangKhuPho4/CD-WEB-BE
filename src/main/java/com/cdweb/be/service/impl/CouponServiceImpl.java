package com.cdweb.be.service.impl;

import com.cdweb.be.dto.CouponDto;
import com.cdweb.be.entity.Coupon;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.CouponRepository;
import com.cdweb.be.service.CouponService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CouponServiceImpl implements CouponService {

  @Autowired private CouponRepository couponRepository;

  @Autowired private ModelMapper modelMapper;

  @Override
  @Transactional(readOnly = true)
  public Page<CouponDto.Response> getAllCoupons(
      String keyword, Boolean isActive, Pageable pageable) {
    return couponRepository.searchCoupons(keyword, isActive, pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public CouponDto.Response getCouponById(Integer id) {
    Coupon coupon =
        couponRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    return mapToResponse(coupon);
  }

  @Override
  public CouponDto.Response createCoupon(CouponDto.CreateRequest request) {
    if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
      throw new BadRequestException("Coupon code already exists");
    }

    Coupon coupon = new Coupon();
    mapToEntity(request, coupon);
    coupon.setUsedCount(0);

    Coupon savedCoupon = couponRepository.save(coupon);
    return mapToResponse(savedCoupon);
  }

  @Override
  public CouponDto.Response updateCoupon(Integer id, CouponDto.UpdateRequest request) {
    Coupon coupon =
        couponRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));

    if (request.getCode() != null && !request.getCode().equalsIgnoreCase(coupon.getCode())) {
      if (couponRepository.existsByCode(request.getCode().toUpperCase())) {
        throw new BadRequestException("Coupon code already exists");
      }
      coupon.setCode(request.getCode().toUpperCase());
    }

    if (request.getName() != null) coupon.setName(request.getName());
    if (request.getDescription() != null) coupon.setDescription(request.getDescription());
    if (request.getDiscountType() != null) {
      try {
        coupon.setDiscountType(
            Coupon.DiscountType.valueOf(request.getDiscountType().toUpperCase()));
      } catch (IllegalArgumentException e) {
        throw new BadRequestException("Invalid discount type. Use PERCENT or FIXED");
      }
    }
    if (request.getDiscountValue() != null) coupon.setDiscountValue(request.getDiscountValue());
    if (request.getMinOrderValue() != null) coupon.setMinOrderValue(request.getMinOrderValue());
    if (request.getMaxDiscountAmount() != null)
      coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
    if (request.getUsageLimit() != null) coupon.setUsageLimit(request.getUsageLimit());
    if (request.getDateStart() != null) coupon.setDateStart(request.getDateStart());
    if (request.getDateEnd() != null) coupon.setDateEnd(request.getDateEnd());
    if (request.getIsActive() != null) coupon.setIsActive(request.getIsActive());

    Coupon updatedCoupon = couponRepository.save(coupon);
    return mapToResponse(updatedCoupon);
  }

  @Override
  public void deleteCoupon(Integer id) {
    Coupon coupon =
        couponRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    // Soft delete bằng cách vô hiệu hóa thay vì xóa cứng để giữ lịch sử đơn hàng
    coupon.setIsActive(false);
    couponRepository.save(coupon);
  }

  @Override
  public CouponDto.Response toggleActive(Integer id) {
    Coupon coupon =
        couponRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Coupon", "id", id));
    coupon.setIsActive(coupon.getIsActive() == null || !coupon.getIsActive());
    Coupon savedCoupon = couponRepository.save(coupon);
    return mapToResponse(savedCoupon);
  }

  @Override
  @Transactional(readOnly = true)
  public List<CouponDto.Response> getActiveCoupons() {
    return couponRepository.findActiveCoupons(LocalDateTime.now()).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  private void mapToEntity(CouponDto.CreateRequest request, Coupon coupon) {
    coupon.setCode(request.getCode().toUpperCase());
    coupon.setName(request.getName());
    coupon.setDescription(request.getDescription());
    try {
      coupon.setDiscountType(Coupon.DiscountType.valueOf(request.getDiscountType().toUpperCase()));
    } catch (IllegalArgumentException e) {
      throw new BadRequestException("Invalid discount type. Use PERCENT or FIXED");
    }
    coupon.setDiscountValue(request.getDiscountValue());
    coupon.setMinOrderValue(request.getMinOrderValue());
    coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
    coupon.setUsageLimit(request.getUsageLimit());
    coupon.setDateStart(request.getDateStart());
    coupon.setDateEnd(request.getDateEnd());
    coupon.setIsActive(Boolean.TRUE.equals(request.getIsActive()));
  }

  private CouponDto.Response mapToResponse(Coupon coupon) {
    CouponDto.Response response = modelMapper.map(coupon, CouponDto.Response.class);
    response.setDiscountType(coupon.getDiscountType().name());
    return response;
  }
}
