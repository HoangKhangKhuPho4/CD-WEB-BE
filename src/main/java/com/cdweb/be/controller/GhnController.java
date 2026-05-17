package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.GHNDto;
import com.cdweb.be.service.GhnService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ghn") // Khớp với Frontend (api.ts)
@Tag(name = "Giao Hàng Nhanh", description = "Tích hợp API GHN – tính phí, tạo đơn, theo dõi")
public class GhnController {

  @Autowired
  private GhnService ghnService;

  // ═══════════════════════════════════════════════════════════════════════════
  // 1. LẤY ĐỊA CHỈ (Dùng chuẩn DTO của code cũ)
  // ═══════════════════════════════════════════════════════════════════════════

  @GetMapping("/provinces")
  @Operation(summary = "Danh sách tỉnh/thành phố")
  public ResponseEntity<ApiResponse<List<GHNDto.ProvinceResponse>>> getProvinces() {
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách tỉnh thành công", ghnService.getProvinces()));
  }

  @GetMapping("/districts")
  @Operation(summary = "Danh sách quận/huyện theo tỉnh")
  public ResponseEntity<ApiResponse<List<GHNDto.DistrictResponse>>> getDistricts(@RequestParam Integer provinceId) {
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách quận/huyện thành công", ghnService.getDistricts(provinceId)));
  }

  @GetMapping("/wards")
  @Operation(summary = "Danh sách phường/xã theo quận")
  public ResponseEntity<ApiResponse<List<GHNDto.WardResponse>>> getWards(@RequestParam Integer districtId) {
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách phường/xã thành công", ghnService.getWards(districtId)));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 2. TÍNH PHÍ VÀ THỜI GIAN (Khớp với Frontend api.ts)
  // ═══════════════════════════════════════════════════════════════════════════

  @PostMapping("/calculate-fee")
  @Operation(summary = "Tính phí vận chuyển GHN")
  public ResponseEntity<ApiResponse<GHNDto.ShippingFeeResponse>> calculateFee(@RequestBody GHNDto.ShippingFeeRequest request) {
    // Dùng DTO thay vì Map<String, Object> cho an toàn
    GHNDto.ShippingFeeResponse response = ghnService.calculateShippingFee(request);
    return ResponseEntity.ok(ApiResponse.success("Tính phí vận chuyển thành công", response));
  }

  @PostMapping("/checkout")
  @Operation(summary = "Tính phí và thời gian giao hàng tổng hợp cho trang Checkout")
  public ResponseEntity<ApiResponse<GHNDto.CheckoutShippingResponse>> getCheckoutShippingInfo(
          @RequestBody GHNDto.CheckoutShippingRequest request) {
    GHNDto.CheckoutShippingResponse response = ghnService.getCheckoutShippingInfo(request);
    return ResponseEntity.ok(ApiResponse.success("Lấy thông tin vận chuyển thành công", response));
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // 3. QUẢN LÝ ĐƠN HÀNG (Tính năng từ code mới)
  // ═══════════════════════════════════════════════════════════════════════════

  // ═══════════════════════════════════════════════════════════════════════════
  // 3. QUẢN LÝ ĐƠN HÀNG (Đã sửa lại gọi đúng tên hàm của file Service xịn)
  // ═══════════════════════════════════════════════════════════════════════════

  @PostMapping("/create-order")
  @Operation(summary = "Tạo đơn vận chuyển GHN (Test/Admin)")
  public ResponseEntity<ApiResponse<GHNDto.CreateOrderResponse>> createOrder(@RequestBody Map<String, Object> body) {
    // Bóc tách dữ liệu từ Map để truyền vào hàm createShippingOrder xịn
    String orderCode = (String) body.getOrDefault("orderCode", "TEST-01");
    String toName = (String) body.getOrDefault("toName", "Khách hàng");
    String toPhone = (String) body.getOrDefault("toPhone", "0909999999");
    String toAddress = (String) body.getOrDefault("toAddress", "123 Đường Test");
    String toWardCode = (String) body.getOrDefault("toWardCode", "90737");
    int toDistrictId = body.containsKey("toDistrictId") ? (Integer) body.get("toDistrictId") : 3695;
    long codAmount = body.containsKey("codAmount") ? ((Number) body.get("codAmount")).longValue() : 0L;
    long insuranceValue = body.containsKey("insuranceValue") ? ((Number) body.get("insuranceValue")).longValue() : 0L;
    String itemName = (String) body.getOrDefault("itemName", "Sản phẩm test");
    int quantity = body.containsKey("quantity") ? (Integer) body.get("quantity") : 1;

    GHNDto.CreateOrderResponse result = ghnService.createShippingOrder(
            orderCode, toName, toPhone, toAddress, toWardCode, toDistrictId, codAmount, insuranceValue, itemName, quantity
    );
    return ResponseEntity.ok(ApiResponse.success("Tạo đơn vận chuyển thành công", result));
  }

  @GetMapping("/track/{orderCode}")
  @Operation(summary = "Theo dõi đơn hàng GHN")
  public ResponseEntity<ApiResponse<GHNDto.TrackingResponse>> trackOrder(@PathVariable String orderCode) {
    // Gọi đúng tên hàm getTrackingInfo
    GHNDto.TrackingResponse result = ghnService.getTrackingInfo(orderCode);
    return ResponseEntity.ok(ApiResponse.success("Lấy thông tin vận chuyển thành công", result));
  }

  @PostMapping("/webhook")
  @Operation(summary = "GHN Webhook callback", description = "Lắng nghe cập nhật trạng thái từ GHN")
  public ResponseEntity<String> webhook(@RequestBody Map<String, Object> payload) {
    // Sau này bạn sẽ mở comment dòng dưới để cập nhật trạng thái đơn hàng tự động
    // String orderCode = (String) payload.get("order_code");
    // String status = (String) payload.get("status");
    // orderManagementService.updateByGhnCode(orderCode, status);

    return ResponseEntity.ok("OK");
  }
}