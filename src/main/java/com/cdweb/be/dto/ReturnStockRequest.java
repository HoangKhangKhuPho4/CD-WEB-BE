package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReturnStockRequest {

  @NotBlank(message = "Mã IMEI/Serial không được để trống")
  private String imei;

  private String reason;
  private Boolean isDefective = false;
}
