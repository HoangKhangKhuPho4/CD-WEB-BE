package com.cdweb.be.config;

import com.cdweb.be.entity.Coupon;
import com.cdweb.be.repository.CouponRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataSeeder {

  @Bean
  CommandLineRunner seedCoupons(CouponRepository couponRepository) {
    return args -> {
      if (couponRepository.count() == 0) {
        Coupon coupon1 = new Coupon();
        coupon1.setCode("HELLO2024");
        coupon1.setName("Chào mừng 2024");
        coupon1.setDescription("Giảm giá 10% cho đơn hàng đầu tiên");
        coupon1.setDiscountType(Coupon.DiscountType.PERCENT);
        coupon1.setDiscountValue(new BigDecimal("10.00"));
        coupon1.setMinOrderValue(new BigDecimal("500000.00"));
        coupon1.setMaxDiscountAmount(new BigDecimal("100000.00"));
        coupon1.setUsageLimit(100);
        coupon1.setUsedCount(0);
        coupon1.setDateStart(LocalDateTime.now().minusDays(1));
        coupon1.setDateEnd(LocalDateTime.now().plusMonths(1));
        coupon1.setIsActive(true);

        Coupon coupon2 = new Coupon();
        coupon2.setCode("FIXED50K");
        coupon2.setName("Giảm 50K");
        coupon2.setDescription("Giảm trực tiếp 50.000 VNĐ cho đơn hàng từ 1 triệu");
        coupon2.setDiscountType(Coupon.DiscountType.FIXED);
        coupon2.setDiscountValue(new BigDecimal("50000.00"));
        coupon2.setMinOrderValue(new BigDecimal("1000000.00"));
        coupon2.setMaxDiscountAmount(new BigDecimal("50000.00"));
        coupon2.setUsageLimit(50);
        coupon2.setUsedCount(0);
        coupon2.setDateStart(LocalDateTime.now().minusDays(1));
        coupon2.setDateEnd(LocalDateTime.now().plusMonths(2));
        coupon2.setIsActive(true);

        couponRepository.save(coupon1);
        couponRepository.save(coupon2);

        System.out.println("Seeded 2 sample coupons.");
      }
    };
  }
}
