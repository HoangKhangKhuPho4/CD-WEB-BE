package com.cdweb.be.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiResponse<T> {
  private boolean success;
  private String message;
  private T data;
  private Object error;

  public static <T> ApiResponse<T> success(T data) {
    return new ApiResponse<>(true, "Success", data, null);
  }

  public static <T> ApiResponse<T> success(String message, T data) {
    return new ApiResponse<>(true, message, data, null);
  }

  public static <T> ApiResponse<T> error(String message) {
    return new ApiResponse<>(false, message, null, null);
  }

  public static <T> ApiResponse<T> error(String message, Object error) {
    return new ApiResponse<>(false, message, null, error);
  }
}
