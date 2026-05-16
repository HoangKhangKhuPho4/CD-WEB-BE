package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AddressDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {

    @NotBlank(message = "Receiver name is required")
    @Size(max = 150, message = "Receiver name must not exceed 150 characters")
    private String receiverName;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{9,12}$", message = "Phone must be 9-12 digits")
    private String phone;

    @Size(max = 100, message = "Province must not exceed 100 characters")
    private String province;

    @Size(max = 100, message = "District must not exceed 100 characters")
    private String district;

    @Size(max = 100, message = "Ward must not exceed 100 characters")
    private String ward;

    @NotBlank(message = "Address detail is required")
    @Size(max = 500, message = "Address detail must not exceed 500 characters")
    private String addressDetail;

    private Boolean isDefault = false;

    @Size(max = 50, message = "Label must not exceed 50 characters")
    private String label; // e.g. "Nhà", "Công ty", "Khác"
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Long id;
    private String receiverName;
    private String phone;
    private String province;
    private String district;
    private String ward;
    private String addressDetail;
    private Boolean isDefault;
    private String label;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
  }
}
