package com.cdweb.be.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import lombok.Data;

@Data
public class ImportStockRequest {

  @NotEmpty(message = "Danh sách sản phẩm nhập không được để trống")
  @Valid
  private List<ImportStockItemDto> items;

  private String supplier;
  private String note;
}
