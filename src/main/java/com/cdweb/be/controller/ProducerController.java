package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ProducerDto;
import com.cdweb.be.service.ProducerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Public API — Danh sách nhà sản xuất cho bộ lọc phía khách hàng. GET /api/producers → trả về tất
 * cả producers đang active.
 */
@RestController
@RequestMapping("/api/producers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProducerController {

  private final ProducerService producerService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ProducerDto.SlimResponse>>> getAllProducers() {
    List<ProducerDto.SlimResponse> producers = producerService.getAllProducersSlim(true);
    return ResponseEntity.ok(ApiResponse.success("Producers retrieved successfully", producers));
  }
}
