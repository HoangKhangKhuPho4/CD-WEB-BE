package com.cdweb.be.service;

import com.cdweb.be.config.GHNConfig;
import com.cdweb.be.dto.GHNDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * GHN Service — Tích hợp API Giao Hàng Nhanh (GHN).
 *
 * <h3>Chức năng:</h3>
 *
 * <ul>
 *   <li>Lấy danh sách Tỉnh/Thành, Quận/Huyện, Phường/Xã từ GHN
 *   <li>Lấy danh sách dịch vụ vận chuyển khả dụng
 *   <li>Tính phí vận chuyển động (real-time từ GHN)
 *   <li>Tính thời gian giao hàng dự kiến
 *   <li>API tổng hợp cho màn hình Checkout (phí + leadtime trong 1 request)
 * </ul>
 *
 * <h3>Fallback Strategy:</h3>
 *
 * <p>Nếu GHN API không khả dụng (timeout, lỗi mạng), service sẽ tự động fallback về phí cứng cấu
 * hình trong {@link GHNConfig#getFallbackShippingFee()}
 */
@Service
public class GhnService {

  private static final Logger log = LoggerFactory.getLogger(GhnService.class);

  private static final String PROVINCE_API = "/shiip/public-api/master-data/province";
  private static final String DISTRICT_API = "/shiip/public-api/master-data/district";
  private static final String WARD_API = "/shiip/public-api/master-data/ward";
  private static final String SERVICES_API =
      "/shiip/public-api/v2/shipping-order/available-services";
  private static final String FEE_API = "/shiip/public-api/v2/shipping-order/fee";
  private static final String LEADTIME_API = "/shiip/public-api/v2/shipping-order/leadtime";

  @Autowired private GHNConfig ghnConfig;

  @Autowired private RestTemplate restTemplate;

  @Autowired private ObjectMapper objectMapper;

  // ═══════════════════════════════════════════════════════════════════════════
  // PUBLIC API — Địa chỉ (Province / District / Ward)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Lấy toàn bộ danh sách Tỉnh/Thành phố từ GHN. Frontend dùng để render dropdown chọn tỉnh/thành.
   */
  public List<GHNDto.ProvinceResponse> getProvinces() {
    String url = ghnConfig.getBaseUrl() + PROVINCE_API;
    System.out.println("=== ĐƯỜNG LINK GHN THỰC TẾ ĐANG GỌI: " + url + " ===");
    try {
      HttpHeaders headers = buildHeaders();
      ResponseEntity<JsonNode> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

      JsonNode root = response.getBody();
      List<GHNDto.ProvinceResponse> provinces = new ArrayList<>();

      if (root != null && root.get("code").asInt() == 200) {
        for (JsonNode item : root.get("data")) {
          GHNDto.ProvinceResponse p = new GHNDto.ProvinceResponse();
          p.setProvinceId(item.get("ProvinceID").asInt());
          p.setProvinceName(item.get("ProvinceName").asText());
          p.setCode(item.has("Code") ? item.get("Code").asText("") : "");
          provinces.add(p);
        }
      }
      log.info("GHN: Fetched {} provinces", provinces.size());
      return provinces;
    } catch (RestClientException e) {
      log.error("GHN getProvinces network error: {}", e.getMessage());
      return new ArrayList<>();
    } catch (Exception e) {
      log.error("GHN getProvinces unexpected error: {}", e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * Lấy danh sách Quận/Huyện theo tỉnh/thành.
   *
   * @param provinceId ID tỉnh/thành từ GHN
   */
  public List<GHNDto.DistrictResponse> getDistricts(int provinceId) {
    String url = ghnConfig.getBaseUrl() + DISTRICT_API + "?province_id=" + provinceId;
    try {
      HttpHeaders headers = buildHeaders();
      ResponseEntity<JsonNode> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

      JsonNode root = response.getBody();
      List<GHNDto.DistrictResponse> districts = new ArrayList<>();

      if (root != null && root.get("code").asInt() == 200) {
        for (JsonNode item : root.get("data")) {
          GHNDto.DistrictResponse d = new GHNDto.DistrictResponse();
          d.setDistrictId(item.get("DistrictID").asInt());
          d.setProvinceId(item.get("ProvinceID").asInt());
          d.setDistrictName(item.get("DistrictName").asText());
          d.setDistrictEncode(item.has("Type") ? item.get("Type").asInt(0) : 0);
          districts.add(d);
        }
      }
      log.info("GHN: Fetched {} districts for province {}", districts.size(), provinceId);
      return districts;
    } catch (RestClientException e) {
      log.error("GHN getDistricts network error for province {}: {}", provinceId, e.getMessage());
      return new ArrayList<>();
    } catch (Exception e) {
      log.error(
          "GHN getDistricts unexpected error for province {}: {}", provinceId, e.getMessage());
      return new ArrayList<>();
    }
  }

  /**
   * Lấy danh sách Phường/Xã theo quận/huyện.
   *
   * @param districtId ID quận/huyện từ GHN
   */
  public List<GHNDto.WardResponse> getWards(int districtId) {
    String url = ghnConfig.getBaseUrl() + WARD_API + "?district_id=" + districtId;
    try {
      HttpHeaders headers = buildHeaders();
      ResponseEntity<JsonNode> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

      JsonNode root = response.getBody();
      List<GHNDto.WardResponse> wards = new ArrayList<>();

      if (root != null && root.get("code").asInt() == 200) {
        for (JsonNode item : root.get("data")) {
          GHNDto.WardResponse w = new GHNDto.WardResponse();
          w.setWardCode(item.get("WardCode").asText());
          w.setDistrictId(item.get("DistrictID").asInt());
          w.setWardName(item.get("WardName").asText());
          wards.add(w);
        }
      }
      log.info("GHN: Fetched {} wards for district {}", wards.size(), districtId);
      return wards;
    } catch (Exception e) {
      log.error("GHN getWards failed for district {}: {}", districtId, e.getMessage());
      return new ArrayList<>();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // SHIPPING FEE — Tính phí vận chuyển
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Tính phí vận chuyển thực tế từ GHN dựa trên địa chỉ nhận hàng.
   *
   * <p>Flow:
   *
   * <ol>
   *   <li>Gọi GHN lấy danh sách dịch vụ khả dụng (available-services)
   *   <li>Chọn dịch vụ đầu tiên (mặc định hoặc rẻ nhất)
   *   <li>Gọi GHN tính phí ship với service đó
   *   <li>Nếu lỗi → fallback về phí cứng trong config
   * </ol>
   *
   * @param request Thông tin địa chỉ nhận hàng
   * @return ShippingFeeResponse với phí vận chuyển và thông tin dịch vụ
   */
  public GHNDto.ShippingFeeResponse calculateShippingFee(GHNDto.ShippingFeeRequest request) {
    GHNDto.ShippingFeeResponse result = new GHNDto.ShippingFeeResponse();

    try {
      // Bước 1: Lấy dịch vụ khả dụng
      Integer serviceId =
          getFirstAvailableService(ghnConfig.getFromDistrictId(), request.getToDistrictId());

      if (serviceId == null) {
        log.warn(
            "GHN: No available service for district {}. Using fallback fee.",
            request.getToDistrictId());
        return buildFallbackFeeResponse();
      }

      // Bước 2: Tính phí vận chuyển
      Map<String, Object> body = new HashMap<>();
      body.put("service_id", serviceId);
      body.put("from_district_id", ghnConfig.getFromDistrictId());
      body.put("from_ward_code", ghnConfig.getFromWardCode());
      body.put("to_district_id", request.getToDistrictId());
      body.put("to_ward_code", request.getToWardCode());
      body.put(
          "weight",
          request.getWeight() != null ? request.getWeight() : ghnConfig.getDefaultWeight());
      body.put("length", ghnConfig.getDefaultLength());
      body.put("width", ghnConfig.getDefaultWidth());
      body.put("height", ghnConfig.getDefaultHeight());
      if (request.getInsuranceValue() != null && request.getInsuranceValue() > 0) {
        body.put("insurance_value", request.getInsuranceValue());
      }

      String feeUrl = ghnConfig.getBaseUrl() + FEE_API;
      HttpHeaders headers = buildShopHeaders();
      ResponseEntity<JsonNode> feeResponse =
          restTemplate.exchange(
              feeUrl, HttpMethod.GET, new HttpEntity<>(body, headers), JsonNode.class);

      JsonNode feeRoot = feeResponse.getBody();
      if (feeRoot == null || feeRoot.get("code").asInt() != 200) {
        log.warn("GHN Fee API error: {}", feeRoot);
        return buildFallbackFeeResponse();
      }

      long fee = feeRoot.get("data").get("total").asLong();
      result.setShippingFee(fee);
      result.setShippingFeeFormatted(formatVND(fee));
      result.setServiceId(serviceId);
      result.setServiceName("GHN Express");
      result.setFreeShipping(false);

      log.info(
          "GHN Fee calculated: {}đ for district {} ward {}",
          fee,
          request.getToDistrictId(),
          request.getToWardCode());
      return result;

    } catch (Exception e) {
      log.error("GHN calculateShippingFee error: {}", e.getMessage());
      return buildFallbackFeeResponse();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // LEAD TIME — Thời gian giao hàng dự kiến
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Tính thời gian giao hàng dự kiến từ GHN.
   *
   * @param request Thông tin địa chỉ nhận hàng
   * @return LeadTimeResponse với ngày giao dự kiến
   */
  public GHNDto.LeadTimeResponse calculateLeadTime(GHNDto.LeadTimeRequest request) {
    GHNDto.LeadTimeResponse result = new GHNDto.LeadTimeResponse();

    try {
      Integer serviceId =
          getFirstAvailableService(ghnConfig.getFromDistrictId(), request.getToDistrictId());
      if (serviceId == null) {
        return buildFallbackLeadTime();
      }

      Map<String, Object> body = new HashMap<>();
      body.put("from_district_id", ghnConfig.getFromDistrictId());
      body.put("from_ward_code", ghnConfig.getFromWardCode());
      body.put("to_district_id", request.getToDistrictId());
      body.put("to_ward_code", request.getToWardCode());
      body.put("service_id", serviceId);

      String leadtimeUrl = ghnConfig.getBaseUrl() + LEADTIME_API;
      HttpHeaders headers = buildShopHeaders();
      ResponseEntity<JsonNode> response =
          restTemplate.exchange(
              leadtimeUrl, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

      JsonNode root = response.getBody();
      if (root == null || root.get("code").asInt() != 200) {
        return buildFallbackLeadTime();
      }

      long leadtimeTimestamp = root.get("data").get("leadtime").asLong();
      result.setLeadtime(leadtimeTimestamp);
      result.setServiceId(serviceId);

      // Format ngày giao hàng đẹp tiếng Việt
      LocalDate deliveryDate =
          Instant.ofEpochSecond(leadtimeTimestamp)
              .atZone(ZoneId.of("Asia/Ho_Chi_Minh"))
              .toLocalDate();

      result.setEstimatedDeliveryDate(
          deliveryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
      result.setEstimatedDeliveryDisplay(formatDeliveryDisplay(deliveryDate));

      log.info(
          "GHN LeadTime: delivery by {} for district {}",
          result.getEstimatedDeliveryDate(),
          request.getToDistrictId());
      return result;

    } catch (Exception e) {
      log.error("GHN calculateLeadTime error: {}", e.getMessage());
      return buildFallbackLeadTime();
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // CHECKOUT SHIPPING INFO — API tổng hợp cho màn hình Checkout
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * API tổng hợp: Tính PHÍ + THỜI GIAN GIAO HÀNG trong 1 request duy nhất.
   *
   * <p>Frontend chỉ cần gọi 1 API này thay vì gọi 2 API riêng lẻ. Backend proxy qua GHN, xử lý lỗi,
   * và trả về đầy đủ thông tin.
   *
   * <p>Logic kiểm tra miễn phí ship:
   *
   * <ul>
   *   <li>Nếu {@code orderSubtotal >= ghnConfig.freeShippingThreshold} → miễn phí
   *   <li>Nếu {@code freeShippingThreshold == 0} → không có miễn phí
   * </ul>
   *
   * @param request Địa chỉ + tổng giá trị đơn hàng
   * @return CheckoutShippingResponse với đầy đủ thông tin phí + thời gian
   */
  public GHNDto.CheckoutShippingResponse getCheckoutShippingInfo(
      GHNDto.CheckoutShippingRequest request) {
    GHNDto.CheckoutShippingResponse result = new GHNDto.CheckoutShippingResponse();

    try {
      // Bước 1: Lấy service ID khả dụng
      Integer serviceId =
          getFirstAvailableService(ghnConfig.getFromDistrictId(), request.getToDistrictId());

      if (serviceId == null) {
        log.warn(
            "GHN Checkout: No service available for district {}. Fallback.",
            request.getToDistrictId());
        return buildFallbackCheckoutResponse();
      }

      // Bước 2: Gọi song song Fee + LeadTime
      long shippingFee = callFeeApi(serviceId, request.getToDistrictId(), request.getToWardCode());
      String leadTimeDisplay =
          callLeadTimeApi(serviceId, request.getToDistrictId(), request.getToWardCode());
      String leadTimeDate =
          getLeadTimeDateString(serviceId, request.getToDistrictId(), request.getToWardCode());

      // Bước 3: Kiểm tra miễn phí ship
      boolean isFreeShipping = false;
      String freeShippingReason = null;
      long threshold = ghnConfig.getFreeShippingThreshold();

      if (threshold > 0
          && request.getOrderSubtotal() != null
          && request.getOrderSubtotal() >= threshold) {
        isFreeShipping = true;
        shippingFee = 0;
        freeShippingReason = "Miễn phí vận chuyển cho đơn hàng từ " + formatVND(threshold);
      }

      // Bước 4: Build response
      result.setShippingFee(shippingFee);
      result.setShippingFeeFormatted(isFreeShipping ? "Miễn phí" : formatVND(shippingFee));
      result.setFreeShipping(isFreeShipping);
      result.setFreeShippingReason(freeShippingReason);
      result.setEstimatedDeliveryDate(leadTimeDate);
      result.setEstimatedDeliveryDisplay(leadTimeDisplay);
      result.setServiceId(serviceId);
      result.setServiceName("GHN Express");
      result.setError(false);

      return result;

    } catch (Exception e) {
      log.error("GHN getCheckoutShippingInfo error: {}", e.getMessage());
      return buildFallbackCheckoutResponse();
    }
  }

  /**
   * Tính phí ship từ BigDecimal subtotal — dùng trong OrderService khi tạo đơn hàng.
   *
   * <p>Đây là method để OrderService gọi khi cần phí ship thực tế, thay thế cho hardcode
   * DEFAULT_SHIPPING_FEE.
   *
   * @param toDistrictId District ID nơi nhận
   * @param toWardCode Ward Code nơi nhận
   * @param orderSubtotal Tổng tiền đơn hàng (để check free shipping)
   * @return Phí vận chuyển (VNĐ), 0 nếu miễn phí
   */
  public BigDecimal calculateShippingFeeForOrder(
      Integer toDistrictId, String toWardCode, BigDecimal orderSubtotal) {
    // Check miễn phí trước
    long threshold = ghnConfig.getFreeShippingThreshold();
    if (threshold > 0
        && orderSubtotal != null
        && orderSubtotal.compareTo(BigDecimal.valueOf(threshold)) >= 0) {
      log.info("Free shipping applied for order >= {}đ", threshold);
      return BigDecimal.ZERO;
    }

    // Nếu không có địa chỉ GHN → fallback
    if (toDistrictId == null || toWardCode == null || toWardCode.isBlank()) {
      log.warn("GHN: Missing district/ward info. Using fallback fee.");
      return BigDecimal.valueOf(ghnConfig.getFallbackShippingFee());
    }

    try {
      Integer serviceId = getFirstAvailableService(ghnConfig.getFromDistrictId(), toDistrictId);
      if (serviceId == null) {
        return BigDecimal.valueOf(ghnConfig.getFallbackShippingFee());
      }

      long fee = callFeeApi(serviceId, toDistrictId, toWardCode);
      return BigDecimal.valueOf(fee);

    } catch (Exception e) {
      log.error("GHN calculateShippingFeeForOrder error: {}", e.getMessage());
      return BigDecimal.valueOf(ghnConfig.getFallbackShippingFee());
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // PRIVATE HELPERS
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Lấy Service ID phù hợp nhất từ GHN cho hàng điện tử.
   *
   * <p>Ưu tiên chọn theo thứ tự:
   *
   * <ol>
   *   <li>service_type_id = 2 → "Hàng nhẹ" (Electro Store chủ yếu bán điện tử)
   *   <li>service_type_id = 5 → "Hàng nặng" (fallback nếu không có Hàng nhẹ)
   *   <li>Phần tử đầu tiên bất kỳ (fallback cuối cùng)
   * </ol>
   *
   * <p>Dựa trên kết quả GHN thực tế:
   *
   * <ul>
   *   <li>service_id=53320 | "Hàng nhẹ" | service_type_id=2 ← ƯU TIÊN
   *   <li>service_id=100039 | "Hàng nặng" | service_type_id=5 ← Fallback
   * </ul>
   */
  private Integer getFirstAvailableService(int fromDistrictId, int toDistrictId) {
    try {
      String url = ghnConfig.getBaseUrl() + SERVICES_API;

      Map<String, Object> body = new HashMap<>();
      body.put("shop_id", ghnConfig.getShopId());
      body.put("from_district", fromDistrictId);
      body.put("to_district", toDistrictId);

      HttpHeaders headers = buildShopHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      log.debug("GHN Available Services request: url={}, body={}", url, body);

      ResponseEntity<JsonNode> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

      JsonNode root = response.getBody();
      log.debug("GHN Available Services response: {}", root);

      if (root == null || root.get("code").asInt() != 200 || root.get("data").size() == 0) {
        log.warn("GHN Available Services trả về rỗng hoặc lỗi: {}", root);
        return null;
      }

      JsonNode services = root.get("data");

      // Ưu tiên 1: Hàng nhẹ (service_type_id = 2) — phù hợp điện tử
      for (JsonNode svc : services) {
        if (svc.get("service_type_id").asInt() == 2) {
          int id = svc.get("service_id").asInt();
          log.info(
              "GHN: Selected service '{}' (id={}, type=Hàng nhẹ)",
              svc.get("short_name").asText(),
              id);
          return id;
        }
      }

      // Ưu tiên 2: Hàng nặng (service_type_id = 5)
      for (JsonNode svc : services) {
        if (svc.get("service_type_id").asInt() == 5) {
          int id = svc.get("service_id").asInt();
          log.info(
              "GHN: Selected service '{}' (id={}, type=Hàng nặng)",
              svc.get("short_name").asText(),
              id);
          return id;
        }
      }

      // Fallback: Lấy phần tử đầu tiên
      int id = services.get(0).get("service_id").asInt();
      log.info("GHN: Selected first available service (id={})", id);
      return id;

    } catch (Exception e) {
      log.error(
          "GHN getFirstAvailableService lỗi: {} | fromDistrict={}, toDistrict={}",
          e.getMessage(),
          fromDistrictId,
          toDistrictId);
      return null; // Trả null → caller sẽ dùng fallback
    }
  }

  /**
   * Gọi GHN Fee API và trả về tổng phí vận chuyển. Dùng {@link UriComponentsBuilder} để build URL
   * an toàn, tránh lỗi encoding.
   */
  private long callFeeApi(int serviceId, int toDistrictId, String toWardCode) {
    try {
      // Dùng UriComponentsBuilder thay vì build query string thủ công
      String url =
          UriComponentsBuilder.fromHttpUrl(ghnConfig.getBaseUrl() + FEE_API)
              .queryParam("service_id", serviceId)
              .queryParam("from_district_id", ghnConfig.getFromDistrictId())
              .queryParam("from_ward_code", ghnConfig.getFromWardCode())
              .queryParam("to_district_id", toDistrictId)
              .queryParam("to_ward_code", toWardCode)
              .queryParam("weight", ghnConfig.getDefaultWeight())
              .queryParam("length", ghnConfig.getDefaultLength())
              .queryParam("width", ghnConfig.getDefaultWidth())
              .queryParam("height", ghnConfig.getDefaultHeight())
              .toUriString();

      HttpHeaders headers = buildShopHeaders();
      log.debug("GHN Fee API request: {}", url);

      ResponseEntity<JsonNode> response =
          restTemplate.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), JsonNode.class);

      JsonNode root = response.getBody();
      log.debug("GHN Fee API response: {}", root);

      if (root != null && root.get("code").asInt() == 200) {
        long fee = root.get("data").get("total").asLong();
        log.info(
            "GHN Fee: {}d (serviceId={}, district={}, ward={})",
            fee,
            serviceId,
            toDistrictId,
            toWardCode);
        return fee;
      }
      log.warn("GHN Fee API trả về lỗi: {}", root);
    } catch (Exception e) {
      log.error(
          "GHN callFeeApi lỗi: {} | serviceId={}, district={}, ward={}",
          e.getMessage(),
          serviceId,
          toDistrictId,
          toWardCode);
    }
    return ghnConfig.getFallbackShippingFee();
  }

  /**
   * Gọi GHN LeadTime API và trả về chuỗi hiển thị ngày giao.
   *
   * <p>Ưu tiên dùng {@code leadtime_order.to_estimate_date} (ISO string rõ ràng hơn). Fallback về
   * {@code leadtime} (Unix timestamp) nếu không có.
   *
   * <p>Ví dụ response GHN thực tế:
   *
   * <pre>
   * {
   *   "leadtime": 1773161999,
   *   "leadtime_order": {
   *     "from_estimate_date": "2026-03-10T16:59:59Z",
   *     "to_estimate_date":   "2026-03-10T16:59:59Z"  ← Dùng cái này
   *   }
   * }
   * </pre>
   */
  private String callLeadTimeApi(int serviceId, int toDistrictId, String toWardCode) {
    try {
      Map<String, Object> body = new HashMap<>();
      body.put("from_district_id", ghnConfig.getFromDistrictId());
      body.put("from_ward_code", ghnConfig.getFromWardCode());
      body.put("to_district_id", toDistrictId);
      body.put("to_ward_code", toWardCode);
      body.put("service_id", serviceId);

      String url = ghnConfig.getBaseUrl() + LEADTIME_API;
      HttpHeaders headers = buildShopHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      ResponseEntity<JsonNode> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

      JsonNode root = response.getBody();
      if (root != null && root.get("code").asInt() == 200) {
        JsonNode data = root.get("data");
        LocalDate deliveryDate;

        // Ưu tiên: dùng to_estimate_date từ leadtime_order (ISO string chính xác hơn)
        if (data.has("leadtime_order")
            && data.get("leadtime_order").has("to_estimate_date")
            && !data.get("leadtime_order").get("to_estimate_date").isNull()) {
          String isoDate = data.get("leadtime_order").get("to_estimate_date").asText();
          deliveryDate = Instant.parse(isoDate).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
          log.info("GHN LeadTime: dùng leadtime_order.to_estimate_date = {}", isoDate);
        } else {
          // Fallback: dùng Unix timestamp
          long timestamp = data.get("leadtime").asLong();
          deliveryDate =
              Instant.ofEpochSecond(timestamp).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
          log.info("GHN LeadTime: dùng leadtime timestamp = {}", timestamp);
        }
        return formatDeliveryDisplay(deliveryDate);
      }
    } catch (Exception e) {
      log.warn("GHN LeadTime call failed: {}", e.getMessage());
    }
    return "Dự kiến giao trong 2-3 ngày";
  }

  /** Gọi GHN LeadTime API và trả về chuỗi ngày dd/MM/yyyy. */
  private String getLeadTimeDateString(int serviceId, int toDistrictId, String toWardCode) {
    try {
      Map<String, Object> body = new HashMap<>();
      body.put("from_district_id", ghnConfig.getFromDistrictId());
      body.put("from_ward_code", ghnConfig.getFromWardCode());
      body.put("to_district_id", toDistrictId);
      body.put("to_ward_code", toWardCode);
      body.put("service_id", serviceId);

      String url = ghnConfig.getBaseUrl() + LEADTIME_API;
      HttpHeaders headers = buildShopHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);

      ResponseEntity<JsonNode> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

      JsonNode root = response.getBody();
      if (root != null && root.get("code").asInt() == 200) {
        JsonNode data = root.get("data");
        LocalDate deliveryDate;

        // Ưu tiên: to_estimate_date từ leadtime_order (đồng bộ với callLeadTimeApi)
        if (data.has("leadtime_order")
            && data.get("leadtime_order").has("to_estimate_date")
            && !data.get("leadtime_order").get("to_estimate_date").isNull()) {
          String isoDate = data.get("leadtime_order").get("to_estimate_date").asText();
          deliveryDate = Instant.parse(isoDate).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        } else {
          // Fallback: Unix timestamp
          long timestamp = data.get("leadtime").asLong();
          deliveryDate =
              Instant.ofEpochSecond(timestamp).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toLocalDate();
        }
        return deliveryDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
      }
    } catch (Exception e) {
      log.warn("GHN getLeadTimeDateString call failed: {}", e.getMessage());
    }
    // Fallback: 3 ngày kể từ hôm nay
    return LocalDate.now().plusDays(3).format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
  }

  /** Build HTTP headers với GHN Token (không kèm ShopId — dành cho master-data APIs) */
  private HttpHeaders buildHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Token", ghnConfig.getToken());
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /** Build HTTP headers với GHN Token + ShopId (dành cho shipping APIs) */
  private HttpHeaders buildShopHeaders() {
    HttpHeaders headers = new HttpHeaders();
    headers.set("Token", ghnConfig.getToken());
    headers.set("ShopId", String.valueOf(ghnConfig.getShopId()));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

  /** Format số tiền VNĐ: 25000 → "25.000 đ" */
  private String formatVND(long amount) {
    NumberFormat nf = NumberFormat.getNumberInstance(new Locale("vi", "VN"));
    return nf.format(amount) + " đ";
  }

  /**
   * Format ngày giao hàng thân thiện tiếng Việt. VD: "Dự kiến giao: Thứ Hai, ngày 10 tháng 3 năm
   * 2026"
   */
  private String formatDeliveryDisplay(LocalDate date) {
    String dayOfWeek = date.getDayOfWeek().getDisplayName(TextStyle.FULL, new Locale("vi"));
    // Capitalize first letter
    dayOfWeek = dayOfWeek.substring(0, 1).toUpperCase() + dayOfWeek.substring(1);

    return String.format(
        "Dự kiến giao: %s, ngày %d tháng %d",
        dayOfWeek, date.getDayOfMonth(), date.getMonthValue());
  }

  /** Build response fallback khi GHN API không khả dụng. */
  private GHNDto.ShippingFeeResponse buildFallbackFeeResponse() {
    GHNDto.ShippingFeeResponse r = new GHNDto.ShippingFeeResponse();
    r.setShippingFee(ghnConfig.getFallbackShippingFee());
    r.setShippingFeeFormatted(formatVND(ghnConfig.getFallbackShippingFee()));
    r.setServiceId(0);
    r.setServiceName("Vận chuyển tiêu chuẩn");
    r.setFreeShipping(false);
    return r;
  }

  private GHNDto.LeadTimeResponse buildFallbackLeadTime() {
    GHNDto.LeadTimeResponse r = new GHNDto.LeadTimeResponse();
    LocalDate estimated = LocalDate.now().plusDays(3);
    r.setEstimatedDeliveryDate(estimated.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    r.setEstimatedDeliveryDisplay(formatDeliveryDisplay(estimated));
    r.setLeadtime(0);
    r.setServiceId(0);
    return r;
  }

  private GHNDto.CheckoutShippingResponse buildFallbackCheckoutResponse() {
    GHNDto.CheckoutShippingResponse r = new GHNDto.CheckoutShippingResponse();
    r.setShippingFee(ghnConfig.getFallbackShippingFee());
    r.setShippingFeeFormatted(formatVND(ghnConfig.getFallbackShippingFee()));
    r.setFreeShipping(false);

    LocalDate estimated = LocalDate.now().plusDays(3);
    r.setEstimatedDeliveryDate(estimated.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    r.setEstimatedDeliveryDisplay(formatDeliveryDisplay(estimated));

    r.setServiceId(0);
    r.setServiceName("Vận chuyển tiêu chuẩn");
    r.setError(false); // không show error cho user, dùng fallback trong suốt
    return r;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TẠO VẬN ĐƠN GHN (Create Shipping Order)
  // ═══════════════════════════════════════════════════════════════════════════

  private static final String CREATE_ORDER_API = "/shiip/public-api/v2/shipping-order/create";
  private static final String CANCEL_ORDER_API = "/shiip/public-api/v2/switch-status/cancel";
  private static final String TRACKING_API = "/shiip/public-api/v2/shipping-order/detail";

  /**
   * Tạo vận đơn thực tế trên hệ thống GHN.
   *
   * @param orderCode Mã đơn hàng nội bộ (ORD-xxx)
   * @param toName Tên người nhận
   * @param toPhone SĐT người nhận
   * @param toAddress Địa chỉ chi tiết người nhận
   * @param toWardCode Ward Code GHN người nhận
   * @param toDistrictId District ID GHN người nhận
   * @param codAmount Số tiền COD thu hộ (0 nếu đã thanh toán online)
   * @param insuranceValue Giá trị bảo hiểm hàng hoá (dùng giá tiền đơn hàng)
   * @param itemName Tên sản phẩm (hiển thị trên vận đơn)
   * @param quantity Tổng số lượng sản phẩm
   * @return CreateOrderResponse chứa mã vận đơn GHN (order_code)
   */
  public GHNDto.CreateOrderResponse createShippingOrder(
      String orderCode,
      String toName,
      String toPhone,
      String toAddress,
      String toWardCode,
      int toDistrictId,
      long codAmount,
      long insuranceValue,
      String itemName,
      int quantity) {

    String url = ghnConfig.getBaseUrl() + CREATE_ORDER_API;

    try {
      // Lấy service ID khả dụng
      Integer serviceId = getFirstAvailableService(ghnConfig.getFromDistrictId(), toDistrictId);
      if (serviceId == null) {
        throw new RuntimeException(
            "Không tìm thấy dịch vụ GHN khả dụng cho quận/huyện: " + toDistrictId);
      }

      // Build request body theo đặc tả GHN API v2
      Map<String, Object> body = new HashMap<>();
      body.put("payment_type_id", 2); // 2 = Người gửi trả phí ship (Shop trả)
      body.put("note", "Đơn hàng " + orderCode);
      body.put("required_note", "CHOTHUHANG"); // Cho thử hàng
      body.put("client_order_code", orderCode); // Mã đơn nội bộ
      body.put("to_name", toName);
      body.put("to_phone", toPhone);
      body.put("to_address", toAddress);
      body.put("to_ward_code", toWardCode);
      body.put("to_district_id", toDistrictId);
      body.put("cod_amount", codAmount);
      body.put("weight", ghnConfig.getDefaultWeight());
      body.put("length", ghnConfig.getDefaultLength());
      body.put("width", ghnConfig.getDefaultWidth());
      body.put("height", ghnConfig.getDefaultHeight());
      body.put("insurance_value", insuranceValue);
      body.put("service_id", serviceId);
      body.put("service_type_id", 2); // Hàng nhẹ

      // Items
      List<Map<String, Object>> items = new ArrayList<>();
      Map<String, Object> item = new HashMap<>();
      item.put("name", itemName);
      item.put("quantity", quantity);
      item.put("weight", ghnConfig.getDefaultWeight());
      items.add(item);
      body.put("items", items);

      HttpHeaders headers = buildShopHeaders();
      log.info("GHN Create Order request for internal order: {}", orderCode);

      ResponseEntity<JsonNode> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

      JsonNode root = response.getBody();
      if (root == null || root.get("code").asInt() != 200) {
        String msg =
            root != null && root.has("message")
                ? root.get("message").asText()
                : "Unknown GHN error";
        log.error("GHN Create Order failed: {}", msg);
        throw new RuntimeException("GHN tạo vận đơn thất bại: " + msg);
      }

      JsonNode data = root.get("data");
      GHNDto.CreateOrderResponse result = new GHNDto.CreateOrderResponse();
      result.setOrderCode(data.get("order_code").asText());
      result.setSortCode(data.has("sort_code") ? data.get("sort_code").asText() : "");
      result.setTransType(data.has("trans_type") ? data.get("trans_type").asText() : "");
      result.setTotalFee(data.has("total_fee") ? data.get("total_fee").asLong() : 0);
      result.setExpectedDeliveryTime(
          data.has("expected_delivery_time") ? data.get("expected_delivery_time").asText() : "");

      log.info(
          "GHN Order created successfully! GHN Code: {}, Internal: {}",
          result.getOrderCode(),
          orderCode);
      return result;

    } catch (RestClientException e) {
      log.error("GHN API error for order {}: {}", orderCode, e.getMessage());
      throw new RuntimeException("Lỗi kết nối GHN: " + e.getMessage(), e);
    } catch (Exception e) {
      log.error(
          "GHN createShippingOrder unexpected error for order {}: {}", orderCode, e.getMessage());
      throw new RuntimeException("Không thể tạo vận đơn GHN: " + e.getMessage(), e);
    }
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // HỦY VẬN ĐƠN GHN (Cancel Shipping Order)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Hủy vận đơn đã tạo trên GHN. Chỉ hủy được khi shipper chưa lấy hàng.
   *
   * @param ghnOrderCodes Danh sách mã vận đơn GHN cần hủy
   * @return CancelOrderResponse
   */
  public GHNDto.CancelOrderResponse cancelShippingOrder(List<String> ghnOrderCodes) {
    String url = ghnConfig.getBaseUrl() + CANCEL_ORDER_API;
    GHNDto.CancelOrderResponse result = new GHNDto.CancelOrderResponse();

    try {
      Map<String, Object> body = new HashMap<>();
      body.put("order_codes", ghnOrderCodes);

      HttpHeaders headers = buildShopHeaders();
      log.info("GHN Cancel Order request for codes: {}", ghnOrderCodes);

      ResponseEntity<JsonNode> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

      JsonNode root = response.getBody();
      if (root != null && root.get("code").asInt() == 200) {
        result.setSuccess(true);
        result.setMessage("Đã hủy vận đơn GHN thành công: " + String.join(", ", ghnOrderCodes));
        log.info("GHN Order(s) cancelled: {}", ghnOrderCodes);
      } else {
        String msg =
            root != null && root.has("message") ? root.get("message").asText() : "Unknown error";
        result.setSuccess(false);
        result.setMessage("GHN hủy vận đơn thất bại: " + msg);
        log.warn("GHN Cancel Order failed: {}", msg);
      }
    } catch (Exception e) {
      log.error("GHN cancelShippingOrder error: {}", e.getMessage());
      result.setSuccess(false);
      result.setMessage("Lỗi khi hủy vận đơn GHN: " + e.getMessage());
    }
    return result;
  }

  // ═══════════════════════════════════════════════════════════════════════════
  // TRA CỨU VẬN ĐƠN (Track & Trace)
  // ═══════════════════════════════════════════════════════════════════════════

  /**
   * Lấy thông tin tracking chi tiết của vận đơn GHN.
   *
   * @param ghnOrderCode Mã vận đơn GHN
   * @return TrackingResponse chứa trạng thái + lịch sử di chuyển
   */
  public GHNDto.TrackingResponse getTrackingInfo(String ghnOrderCode) {
    String url = ghnConfig.getBaseUrl() + TRACKING_API;
    GHNDto.TrackingResponse result = new GHNDto.TrackingResponse();
    result.setOrderCode(ghnOrderCode);

    try {
      Map<String, Object> body = new HashMap<>();
      body.put("order_code", ghnOrderCode);

      HttpHeaders headers = buildShopHeaders();
      ResponseEntity<JsonNode> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

      JsonNode root = response.getBody();
      if (root == null || root.get("code").asInt() != 200) {
        result.setStatus("unknown");
        result.setStatusDisplay("Không thể tra cứu");
        return result;
      }

      JsonNode data = root.get("data");
      String ghnStatus = data.has("status") ? data.get("status").asText() : "unknown";
      result.setStatus(ghnStatus);
      result.setStatusDisplay(translateGHNStatus(ghnStatus));

      if (data.has("leadtime") && !data.get("leadtime").isNull()) {
        result.setExpectedDeliveryTime(data.get("leadtime").asText());
      }

      // Parse log entries
      List<GHNDto.TrackingLog> logs = new ArrayList<>();
      if (data.has("log") && data.get("log").isArray()) {
        for (JsonNode logEntry : data.get("log")) {
          GHNDto.TrackingLog trackingLog = new GHNDto.TrackingLog();
          String logStatus = logEntry.has("status") ? logEntry.get("status").asText() : "";
          trackingLog.setStatus(logStatus);
          trackingLog.setStatusDisplay(translateGHNStatus(logStatus));
          trackingLog.setUpdatedDate(
              logEntry.has("updated_date") ? logEntry.get("updated_date").asText() : "");
          logs.add(trackingLog);
        }
      }
      result.setLogs(logs);

      log.info("GHN Tracking for {}: status={}", ghnOrderCode, ghnStatus);
      return result;

    } catch (Exception e) {
      log.error("GHN getTrackingInfo error for {}: {}", ghnOrderCode, e.getMessage());
      result.setStatus("error");
      result.setStatusDisplay("Lỗi khi tra cứu vận đơn");
      return result;
    }
  }

  private static final String GEN_PRINT_TOKEN_API = "/shiip/public-api/v2/a5/gen-token";
  private static final String PRINT_A5_PATH = "/shiip/public-api/v2/print/A5";

  /**
   * Lấy token in nhãn vận đơn GHN (khổ A5). Mở {@link GHNDto.PrintLabelResponse#printUrl} trên trình
   * duyệt để in.
   */
  public GHNDto.PrintLabelResponse generatePrintLabel(String ghnOrderCode) {
    if (ghnOrderCode == null || ghnOrderCode.isBlank()) {
      throw new RuntimeException("Thiếu mã vận đơn GHN");
    }

    String url = ghnConfig.getBaseUrl() + GEN_PRINT_TOKEN_API;
    GHNDto.PrintLabelResponse result = new GHNDto.PrintLabelResponse();
    result.setGhnOrderCode(ghnOrderCode);

    try {
      Map<String, Object> body = new HashMap<>();
      body.put("order_codes", List.of(ghnOrderCode));

      HttpHeaders headers = buildShopHeaders();
      ResponseEntity<JsonNode> response =
          restTemplate.exchange(
              url, HttpMethod.POST, new HttpEntity<>(body, headers), JsonNode.class);

      JsonNode root = response.getBody();
      if (root == null || root.get("code").asInt() != 200) {
        String msg =
            root != null && root.has("message")
                ? root.get("message").asText()
                : "GHN gen-token thất bại";
        throw new RuntimeException(msg);
      }

      String token = root.get("data").get("token").asText();
      result.setToken(token);
      result.setPrintUrl(ghnConfig.getBaseUrl() + PRINT_A5_PATH + "?token=" + token);
      log.info("GHN print token generated for {}", ghnOrderCode);
      return result;
    } catch (RestClientException e) {
      log.error("GHN generatePrintLabel error: {}", e.getMessage());
      throw new RuntimeException("Lỗi kết nối GHN khi tạo nhãn in: " + e.getMessage(), e);
    }
  }

  /** Dịch mã trạng thái GHN sang tiếng Việt. */
  private String translateGHNStatus(String ghnStatus) {
    if (ghnStatus == null) return "Không xác định";
    return switch (ghnStatus.toLowerCase()) {
      case "ready_to_pick" -> "Chờ lấy hàng";
      case "picking" -> "Đang lấy hàng";
      case "cancel" -> "Đã hủy";
      case "money_collect_picking" -> "Đang thu tiền người gửi";
      case "picked" -> "Đã lấy hàng";
      case "storing" -> "Hàng đang ở kho";
      case "transporting" -> "Đang luân chuyển";
      case "sorting" -> "Đang phân loại";
      case "delivering" -> "Đang giao hàng";
      case "money_collect_delivering" -> "Đang thu tiền người nhận";
      case "delivered" -> "Giao hàng thành công";
      case "delivery_fail" -> "Giao hàng thất bại";
      case "waiting_to_return" -> "Chờ trả hàng";
      case "return" -> "Đang trả hàng";
      case "return_transporting" -> "Đang luân chuyển trả hàng";
      case "return_sorting" -> "Đang phân loại trả hàng";
      case "returning" -> "Đang trả hàng cho shop";
      case "return_fail" -> "Trả hàng thất bại";
      case "returned" -> "Đã trả hàng";
      case "exception" -> "Hàng ngoại lệ";
      case "damage" -> "Hàng hư hỏng";
      case "lost" -> "Hàng thất lạc";
      default -> ghnStatus;
    };
  }
}
