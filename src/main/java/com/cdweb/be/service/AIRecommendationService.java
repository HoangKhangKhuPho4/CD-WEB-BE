package com.cdweb.be.service;

import com.cdweb.be.dto.ProductDto;
import com.cdweb.be.repository.ProductRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@Service
public class AIRecommendationService {

  @Autowired private RestTemplate restTemplate;

  @Autowired private ProductRepository productRepository;

  @Autowired private ModelMapper modelMapper;

  @Value("${app.server.url:http://localhost:8080}")
  private String serverUrl;

  public List<ProductDto.Response> getRecommendationsForUser(Integer userId) {
    // Gọi sang AI Microservice Flask vừa dựng ở Port 5000
    String url = "http://localhost:5000/recommend/" + userId + "?top_k=10";
    List<ProductDto.Response> result = new ArrayList<>();

    try {
      ResponseEntity<Map<String, Object>> response =
          restTemplate.exchange(
              url,
              org.springframework.http.HttpMethod.GET,
              null,
              new org.springframework.core.ParameterizedTypeReference<Map<String, Object>>() {});
      Map<String, Object> body = response.getBody();

      if (body != null && body.containsKey("recommendations")) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> recommendedList =
            (List<Map<String, Object>>) body.get("recommendations");

        for (Map<String, Object> rec : recommendedList) {
          Integer pId = ((Number) rec.get("product_id")).intValue();
          productRepository
              .findById(pId)
              .ifPresent(
                  product -> {
                    ProductDto.Response dto = modelMapper.map(product, ProductDto.Response.class);
                    if (product.getBasePrice() != null) {
                      dto.setPrice(product.getBasePrice().doubleValue());
                    }
                    dto.setActive(Boolean.TRUE.equals(product.getIsActive()) ? 1 : 0);
                    if (product.getImages() != null && !product.getImages().isEmpty()) {
                      List<ProductDto.ImageDto> imgs = new ArrayList<>();
                      for (var img : product.getImages()) {
                        ProductDto.ImageDto imgDto = new ProductDto.ImageDto();
                        imgDto.setId(img.getId());
                        imgDto.setLinkImage(serverUrl + "/img/" + img.getId());
                        imgDto.setVariantId(
                            img.getVariant() != null ? img.getVariant().getId() : null);
                        imgs.add(imgDto);
                      }
                      dto.setImages(imgs);
                    }
                    result.add(dto);
                  });
        }
      }
    } catch (RestClientException e) {
      System.err.println("AI Service Error (Network/HTTP): " + e.getMessage());
    } catch (Exception e) {
      System.err.println("AI Service Error (Unexpected): " + e.getMessage());
    }
    return result;
  }
}
