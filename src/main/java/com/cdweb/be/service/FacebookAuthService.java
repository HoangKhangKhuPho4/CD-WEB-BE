package com.cdweb.be.service;

import com.cdweb.be.dto.FacebookUserResponse;
import com.cdweb.be.exception.BadRequestException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class FacebookAuthService {

  @Autowired private RestTemplate restTemplate;

  public FacebookUserResponse verifyToken(String accessToken) {
    String url = "https://graph.facebook.com/me?fields=id,name,email&access_token=" + accessToken;

    try {
      FacebookUserResponse response = restTemplate.getForObject(url, FacebookUserResponse.class);

      if (response == null || response.getId() == null) {
        throw new BadRequestException("Mã xác thực Facebook không hợp lệ!");
      }

      return response;
    } catch (Exception e) {
      throw new BadRequestException("Không thể xác thực với Facebook: " + e.getMessage());
    }
  }
}
