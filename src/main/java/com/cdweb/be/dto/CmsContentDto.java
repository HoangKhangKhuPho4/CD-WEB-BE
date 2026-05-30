package com.cdweb.be.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public class CmsContentDto {

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class SaveRequest {
    @NotBlank(message = "Title is required")
    private String title;

    private String subtitle;
    private String linkUrl;
    private String imageUrl;
    private String body;
    private String author;
    private Boolean active;
    private Integer sortOrder;
  }

  @Data
  @NoArgsConstructor
  @AllArgsConstructor
  public static class Response {
    private Long id;
    private String title;
    private String subtitle;
    private String linkUrl;
    private String imageUrl;
    private String body;
    private String author;
    private Boolean active;
    private Integer sortOrder;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
  }
}
