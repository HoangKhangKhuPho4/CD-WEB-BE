package com.cdweb.be.dto;

import java.math.BigDecimal;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class UserInteractionDto {

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Request {
    private Long userId;
    private Integer productId;
    private String actionType; // e.g. "VIEW", "ADD_TO_CART", "PURCHASE", "RATED"
    private BigDecimal rating;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Long id;
    private Long userId;
    private Integer productId;
    private String actionType;
    private BigDecimal rating;
    private BigDecimal interactionScore;
    private String message;
  }
}
