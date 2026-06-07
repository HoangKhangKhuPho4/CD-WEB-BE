package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ProducerDto;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.service.ProducerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Public API — Thương hiệu cho shop / bộ lọc khách hàng. */
@RestController
@RequestMapping("/api/producers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ProducerController {

  private final ProducerService producerService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<ProducerDto.SlimResponse>>> getAllProducers() {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Lấy danh sách thương hiệu thành công", producerService.getAllProducersSlim(true)));
  }

  @GetMapping("/{code}")
  public ResponseEntity<ApiResponse<ProducerDto.SlimResponse>> getProducerByCode(
      @PathVariable String code) {
    ProducerDto.Response full = producerService.getProducerByCode(code);
    if (!Boolean.TRUE.equals(full.getIsActive())) {
      throw new BadRequestException("Thương hiệu không còn hoạt động");
    }
    ProducerDto.SlimResponse slim =
        new ProducerDto.SlimResponse(
            full.getId(), full.getName(), full.getCode(), full.getLogoUrl(), true);
    return ResponseEntity.ok(ApiResponse.success("Lấy thông tin thương hiệu thành công", slim));
  }
}
