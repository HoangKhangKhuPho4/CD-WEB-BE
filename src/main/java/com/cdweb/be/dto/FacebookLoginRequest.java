package com.cdweb.be.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacebookLoginRequest {
  @NotBlank(message = "AccessToken is required")
  private String accessToken;
}
