package com.cdweb.be.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VariantAutocompleteDto {
  private Integer id;
  private String skuCode;
  private String variantName;
  private String productName;
}
