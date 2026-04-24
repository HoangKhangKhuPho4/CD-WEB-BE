package com.cdweb.be.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Cấu hình cho GHN (Giao Hàng Nhanh) Shipping API. Đọc từ application.properties với prefix "ghn"
 *
 * <p>Đăng ký tài khoản tại: https://sso.ghn.vn/register
 *
 * <p>API Documentation: https://api.ghn.vn/home/docs/detail
 */
@Configuration
@ConfigurationProperties(prefix = "ghn")
@Data
public class GHNConfig {

  /** Token xác thực GHN API Lấy từ: https://khachhang.ghn.vn/ → Cài đặt → Token API */
  private String token = "7e2513c5-ed99-11ee-983e-5a49fc0dd8ec";

  /**
   * Shop ID của cửa hàng trên GHN Lấy từ: https://khachhang.ghn.vn/ → Địa chỉ lấy hàng → Shop ID
   */
  private int shopId = 4982538;

  /**
   * Base URL của GHN API Production: https://online-gateway.ghn.vn Sandbox:
   * https://dev-online-gateway.ghn.vn
   */
  private String baseUrl = "https://online-gateway.ghn.vn";

  // ═══════════════════════════════════════════════════════════════════════════
  // Địa chỉ kho xuất hàng (Shop Warehouse — địa chỉ gửi hàng)
  // ═══════════════════════════════════════════════════════════════════════════

  /** Tỉnh/TP của kho hàng (Province ID theo danh mục GHN) 202 = Hồ Chí Minh */
  private int fromProvinceId = 202;

  /** Quận/Huyện của kho hàng (District ID theo danh mục GHN) 3695 = Quận 1, TP.HCM */
  private int fromDistrictId = 3695;

  /** Phường/Xã của kho hàng (Ward Code theo danh mục GHN) 90737 = Phường Bến Nghé, Q1, HCM */
  private String fromWardCode = "90737";

  // ═══════════════════════════════════════════════════════════════════════════
  // Cấu hình đơn hàng mặc định (Default Package Settings)
  // ═══════════════════════════════════════════════════════════════════════════

  /** Trọng lượng mặc định của gói hàng (gram) 500g = 0.5kg cho sản phẩm điện tử nhỏ */
  private int defaultWeight = 500;

  /** Chiều dài mặc định của gói hàng (cm) */
  private int defaultLength = 20;

  /** Chiều rộng mặc định của gói hàng (cm) */
  private int defaultWidth = 15;

  /** Chiều cao mặc định của gói hàng (cm) */
  private int defaultHeight = 10;

  /**
   * Phí vận chuyển mặc định khi không thể gọi GHN API (VNĐ) Dùng làm fallback khi GHN API timeout
   * hoặc lỗi
   */
  private long fallbackShippingFee = 30000L;

  /** Ngưỡng giá trị đơn hàng để được miễn phí vận chuyển (VNĐ) 0 = không có ngưỡng miễn phí */
  private long freeShippingThreshold = 0L;
}
