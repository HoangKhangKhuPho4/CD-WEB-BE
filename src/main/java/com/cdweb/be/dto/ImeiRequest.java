package com.cdweb.be.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import lombok.Data;

@Data
public class ImeiRequest {

  @NotNull(message = "Variant ID không được để trống")
  private Integer variantId;

  @NotEmpty(message = "Danh sách IMEI không được để trống")
  private List<String> imeis;

  private String batchNumber;
  private String note;
}
