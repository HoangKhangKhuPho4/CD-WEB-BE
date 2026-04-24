package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.GHNDto;
import com.cdweb.be.service.GHNService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GHN Controller — Proxy API Giao Hàng Nhanh.
 *
 * <p>Tất cả endpoint đều PUBLIC — Frontend không cần token JWT để gọi. Backend đóng vai trò proxy,
 * ẩn GHN Token khỏi client.
 *
 * <h3>Endpoints:</h3>
 *
 * <ul>
 *   <li>GET /api/shipping/provinces — Danh sách tỉnh/thành
 *   <li>GET /api/shipping/districts?provinceId=X — Danh sách quận/huyện
 *   <li>GET /api/shipping/wards?districtId=X — Danh sách phường/xã
 *   <li>POST /api/shipping/fee — Tính phí vận chuyển
 *   <li>POST /api/shipping/leadtime — Thời gian giao dự kiến
 *   <li>POST /api/shipping/checkout — Phí + Leadtime tổng hợp (dùng cho Checkout)
 * </ul>
 *
 * <h3>Tại sao dùng Backend Proxy thay vì gọi GHN trực tiếp từ Frontend?</h3>
 *
 * <ul>
 *   <li>Bảo mật: Ẩn GHN Token khỏi client (không bị lộ trong Browser DevTools)
 *   <li>CORS: Tránh lỗi CORS khi gọi GHN từ Browser
 *   <li>Fallback: Backend xử lý fallback thông minh khi GHN lỗi
 *   <li>Cache: Có thể thêm cache sau này mà không cần sửa Frontend
 * </ul>
 */
@RestController
@RequestMapping("/api/shipping")
public class GHNController {

  @Autowired private GHNService ghnService;

  // ═══════════════════════════════════════════════════════════════════════════
  // ĐỊA CHỈ — Province / District / Ward
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * GET /api/shipping/provinces Trả về toàn bộ danh sách tỉnh/thành phố Việt Nam từ GHN. Frontend
   * dùng để render dropdown "Tỉnh/Thành" trong form địa chỉ.
   */
  @GetMapping("/provinces")
  public ResponseEntity<ApiResponse<List<GHNDto.ProvinceResponse>>> getProvinces() {
    List<GHNDto.ProvinceResponse> provinces = ghnService.getProvinces();
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tỉnh/thành thành công", provinces));
  }

  /**
   * GET /api/shipping/districts?provinceId=202 Trả về danh sách quận/huyện theo tỉnh/thành được
   * chọn.
   *
   * @param provinceId ID tỉnh/thành từ GHN (VD: 202 = TP.HCM, 269 = Hà Nội)
   */
  @GetMapping("/districts")
  public ResponseEntity<ApiResponse<List<GHNDto.DistrictResponse>>> getDistricts(
      @RequestParam int provinceId) {
    List<GHNDto.DistrictResponse> districts = ghnService.getDistricts(provinceId);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quận/huyện thành công", districts));
  }

  /**
   * GET /api/shipping/wards?districtId=3695 Trả về danh sách phường/xã theo quận/huyện được chọn.
   *
   * @param districtId ID quận/huyện từ GHN (VD: 3695 = Quận 1, HCM)
   */
  @GetMapping("/wards")
  public ResponseEntity<ApiResponse<List<GHNDto.WardResponse>>> getWards(
      @RequestParam int districtId) {
    List<GHNDto.WardResponse> wards = ghnService.getWards(districtId);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phường/xã thành công", wards));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PHÍ VẬN CHUYỂN & THỜI GIAN GIAO
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * POST /api/shipping/fee Tính phí vận chuyển thực tế từ GHN.
   *
   * <p>Request body:
   *
   * <pre>
   * {
   *   "toDistrictId": 3695,
   *   "toWardCode": "90737",
   *   "weight": 500,         // optional, gram
   *   "insuranceValue": 0    // optional
   * }
   * </pre>
   */
  @PostMapping("/fee")
  public ResponseEntity<ApiResponse<GHNDto.ShippingFeeResponse>> calculateShippingFee(
      @RequestBody GHNDto.ShippingFeeRequest request) {
    GHNDto.ShippingFeeResponse response = ghnService.calculateShippingFee(request);
    return ResponseEntity.ok(ApiResponse.success("Tính phí vận chuyển thành công", response));
  }

  /**
   * POST /api/shipping/leadtime Tính thời gian giao hàng dự kiến từ GHN.
   *
   * <p>Request body:
   *
   * <pre>
   * {
   *   "toDistrictId": 3695,
   *   "toWardCode": "90737"
   * }
   * </pre>
   */
  @PostMapping("/leadtime")
  public ResponseEntity<ApiResponse<GHNDto.LeadTimeResponse>> calculateLeadTime(
      @RequestBody GHNDto.LeadTimeRequest request) {
    GHNDto.LeadTimeResponse response = ghnService.calculateLeadTime(request);
    return ResponseEntity.ok(ApiResponse.success("Tính thời gian giao hàng thành công", response));
  }

  /**
   * POST /api/shipping/checkout <b>API CHÍNH CHO CHECKOUT</b> — Tính phí ship + thời gian giao cùng
   * lúc.
   *
   * <p>Frontend chỉ cần gọi 1 API này thay vì 2 API riêng lẻ. Gọi mỗi khi user thay đổi địa chỉ
   * nhận hàng.
   *
   * <p>Request body:
   *
   * <pre>
   * {
   *   "toDistrictId": 3695,
   *   "toWardCode": "90737",
   *   "toProvinceId": 202,
   *   "orderSubtotal": 500000   // để kiểm tra miễn phí ship
   * }
   * </pre>
   *
   * <p>Response:
   *
   * <pre>
   * {
   *   "status": "success",
   *   "data": {
   *     "shippingFee": 25000,
   *     "shippingFeeFormatted": "25.000 đ",
   *     "freeShipping": false,
   *     "freeShippingReason": null,
   *     "estimatedDeliveryDate": "12/03/2026",
   *     "estimatedDeliveryDisplay": "Dự kiến giao: Thứ Năm, ngày 12 tháng 3",
   *     "serviceId": 53321,
   *     "serviceName": "GHN Express",
   *     "error": false
   *   }
   * }
   * </pre>
   */
  @PostMapping("/checkout")
  public ResponseEntity<ApiResponse<GHNDto.CheckoutShippingResponse>> getCheckoutShippingInfo(
      @RequestBody GHNDto.CheckoutShippingRequest request) {
    GHNDto.CheckoutShippingResponse response = ghnService.getCheckoutShippingInfo(request);
    return ResponseEntity.ok(ApiResponse.success("Lấy thông tin vận chuyển thành công", response));
  }
}
