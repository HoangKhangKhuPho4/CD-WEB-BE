package com.cdweb.be.dto;

import lombok.Data;

/**
 * DTO cho GHN (Giao Hàng Nhanh) Shipping API.
 *
 * <p>Bao gồm tất cả các Request/Response cần thiết cho:
 *
 * <ul>
 *   <li>Lấy danh sách Tỉnh/Thành (Province)
 *   <li>Lấy danh sách Quận/Huyện (District)
 *   <li>Lấy danh sách Phường/Xã (Ward)
 *   <li>Lấy dịch vụ vận chuyển khả dụng
 *   <li>Tính phí vận chuyển
 *   <li>Tính thời gian giao hàng dự kiến
 * </ul>
 */
public class GHNDto {

  // ═══════════════════════════════════════════════════════════════════════════
  // PROVINCE — Tỉnh/Thành phố
  // ═══════════════════════════════════════════════════════════════════════════

  /** Response trả về danh sách tỉnh/thành */
  @Data
  public static class ProvinceResponse {
    /** ID tỉnh/thành (GHN internal ID) */
    private int provinceId;

    /** Tên tỉnh/thành */
    private String provinceName;

    /** Mã tỉnh/thành (VD: HCM, HN) */
    private String code;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // DISTRICT — Quận/Huyện
  // ═══════════════════════════════════════════════════════════════════════════

  /** Response trả về danh sách quận/huyện */
  @Data
  public static class DistrictResponse {
    /** ID quận/huyện */
    private int districtId;

    /** ID tỉnh/thành cha */
    private int provinceId;

    /** Tên quận/huyện */
    private String districtName;

    /** Loại quận/huyện (1=Quận, 2=Huyện, 3=Thị xã, 4=Thành phố) */
    private int districtEncode;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // WARD — Phường/Xã
  // ═══════════════════════════════════════════════════════════════════════════

  /** Response trả về danh sách phường/xã */
  @Data
  public static class WardResponse {
    /** Mã phường/xã (là String, không phải int!) */
    private String wardCode;

    /** ID quận/huyện cha */
    private int districtId;

    /** Tên phường/xã */
    private String wardName;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // AVAILABLE SERVICES — Dịch vụ vận chuyển khả dụng
  // ═══════════════════════════════════════════════════════════════════════════

  /** Request để lấy dịch vụ vận chuyển khả dụng */
  @Data
  public static class AvailableServiceRequest {
    /** Shop ID (từ GHNConfig) */
    private int shopId;

    /** District ID của nơi nhận hàng */
    private int toDistrictId;
  }

  /** Response của một dịch vụ vận chuyển */
  @Data
  public static class ServiceResponse {
    /** ID dịch vụ (dùng cho API tính phí và leadtime) */
    private int serviceId;

    /** Tên ngắn của dịch vụ (VD: "Hỏa Tốc", "Nhanh") */
    private String shortName;

    /** Loại dịch vụ */
    private int serviceTypeId;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SHIPPING FEE — Phí vận chuyển
  // ═══════════════════════════════════════════════════════════════════════════

  /** Request tính phí vận chuyển (gửi lên backend, FE không cần gọi GHN trực tiếp) */
  @Data
  public static class ShippingFeeRequest {
    /** District ID của nơi nhận */
    private Integer toDistrictId;

    /** Ward Code của nơi nhận */
    private String toWardCode;

    /** Trọng lượng gói hàng (gram) — optional, dùng default nếu null */
    private Integer weight;

    /** Giá trị đơn hàng để tính phí COD (nếu có) */
    private Long insuranceValue;
  }

  /** Response phí vận chuyển trả về cho Frontend */
  @Data
  public static class ShippingFeeResponse {
    /** Phí vận chuyển (VNĐ) */
    private long shippingFee;

    /** Phí vận chuyển được format (VD: "25.000 ₫") */
    private String shippingFeeFormatted;

    /** Service ID được dùng để tính phí */
    private int serviceId;

    /** Tên dịch vụ */
    private String serviceName;

    /** Có được miễn phí ship không */
    private boolean freeShipping;

    /** Lý do miễn phí (nếu có) */
    private String freeShippingReason;

    /** Tỉnh đến (tên) */
    private String toProvinceName;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // LEAD TIME — Thời gian giao hàng dự kiến
  // ═══════════════════════════════════════════════════════════════════════════

  /** Request tính thời gian giao hàng dự kiến */
  @Data
  public static class LeadTimeRequest {
    /** District ID nơi nhận */
    private Integer toDistrictId;

    /** Ward Code nơi nhận */
    private String toWardCode;
  }

  /** Response thời gian giao hàng dự kiến */
  @Data
  public static class LeadTimeResponse {
    /** Unix timestamp dự kiến giao hàng */
    private long leadtime;

    /** Ngày giao hàng dự kiến (format: dd/MM/yyyy) */
    private String estimatedDeliveryDate;

    /** Hiển thị thân thiện: "Dự kiến giao: Thứ Hai, 10 tháng 3" */
    private String estimatedDeliveryDisplay;

    /** Service ID được dùng */
    private int serviceId;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CHECKOUT SHIPPING INFO — Thông tin vận chuyển tổng hợp cho Checkout
  // ═══════════════════════════════════════════════════════════════════════════

  /** Request tổng hợp — FE gọi 1 API để lấy cả phí và thời gian giao */
  @Data
  public static class CheckoutShippingRequest {
    /** District ID nơi nhận */
    private Integer toDistrictId;

    /** Ward Code nơi nhận */
    private String toWardCode;

    /** Province ID nơi nhận (để hiển thị tên tỉnh) */
    private Integer toProvinceId;

    /** Tổng giá trị đơn hàng (để kiểm tra miễn phí ship) */
    private Long orderSubtotal;
  }

  /** Response tổng hợp — phí + thời gian giao hàng */
  @Data
  public static class CheckoutShippingResponse {
    /** Phí vận chuyển (VNĐ) */
    private long shippingFee;

    /** Phí vận chuyển format */
    private String shippingFeeFormatted;

    /** Có được miễn phí ship không */
    private boolean freeShipping;

    /** Lý do miễn phí (nếu có) */
    private String freeShippingReason;

    /** Ngày giao hàng dự kiến (format dd/MM/yyyy) */
    private String estimatedDeliveryDate;

    /** Hiển thị đẹp: "Dự kiến giao: Thứ Hai, 10/03/2026" */
    private String estimatedDeliveryDisplay;

    /** Service ID dùng để ship */
    private int serviceId;

    /** Tên dịch vụ vận chuyển */
    private String serviceName;

    /** Có lỗi khi gọi GHN không */
    private boolean error;

    /** Thông báo lỗi (nếu error = true) */
    private String errorMessage;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CREATE SHIPPING ORDER — Tạo vận đơn GHN
  // ═══════════════════════════════════════════════════════════════════════════

  @Data
  public static class CreateOrderResponse {
    private String orderCode; // Mã vận đơn GHN (tracking code)
    private String sortCode;
    private String transType;
    private String wardEncode;
    private String districtEncode;
    private long totalFee;
    private String expectedDeliveryTime;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CANCEL SHIPPING ORDER — Hủy vận đơn GHN
  // ═══════════════════════════════════════════════════════════════════════════

  @Data
  public static class CancelOrderResponse {
    private boolean success;
    private String message;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TRACKING — Tra cứu trạng thái vận đơn GHN
  // ═══════════════════════════════════════════════════════════════════════════

  @Data
  public static class TrackingResponse {
    private String orderCode; // Mã vận đơn GHN
    private String status; // ready_to_pick, picking, delivering, delivered, cancel...
    private String statusDisplay; // Vietsub
    private String currentLocation;
    private String expectedDeliveryTime;
    private java.util.List<TrackingLog> logs;
  }

  @Data
  public static class TrackingLog {
    private String status;
    private String statusDisplay;
    private String updatedDate; // ISO date
    private String location;
  }
}
