package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class ProducerDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {
    @NotBlank(message = "Tên nhà sản xuất không được để trống")
    private String name;

    @NotBlank(message = "Mã nhà sản xuất không được để trống")
    @Size(max = 10, message = "Mã nhà sản xuất tối đa 10 ký tự")
    private String code;

    private String logoUrl;
    private String description;
    private String country;
    private String website;
    private Boolean isActive = true;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class UpdateRequest {
    private String name;

    @Size(max = 10, message = "Mã nhà sản xuất tối đa 10 ký tự")
    private String code;

    private String logoUrl;
    private String description;
    private String country;
    private String website;
    private Boolean isActive;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Integer id;
    private String name;
    private String code;
    private String logoUrl;
    private String description;
    private String country;
    private String website;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private Long productCount;
    private Long activeProductCount;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SlimResponse {
    private Integer id;
    private String name;
    private String code;
    private String logoUrl;
    private Boolean isActive;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class AdminStatsResponse {
    private Long total;
    private Long active;
    private Long inactive;
    private Long withProducts;
    private Long withoutProducts;
    private Long totalLinkedProducts;
  }

  @Data
  public static class BulkStatusRequest {
    @NotEmpty(message = "Danh sách ID không được trống")
    private List<Integer> ids;

    @NotNull(message = "Trạng thái active không được trống")
    private Boolean isActive;
  }

  @Data
  public static class ValidateCodeRequest {
    @NotBlank(message = "Mã không được để trống")
    private String code;

    /** Khi sửa — bỏ qua trùng với chính bản ghi này. */
    private Integer excludeId;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ValidateCodeResponse {
    private Boolean available;
    private String code;
    private String message;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class ProductSummary {
    private Integer id;
    private String name;
    private Boolean isActive;
    private java.math.BigDecimal basePrice;
  }
}
