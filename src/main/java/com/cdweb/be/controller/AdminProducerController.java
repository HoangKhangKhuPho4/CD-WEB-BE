package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ProducerDto;
import com.cdweb.be.service.ProducerService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin API — Quản lý Thương hiệu / Nhà sản xuất (đủ scenario kiểm thử). */
@RestController
@RequestMapping("/api/admin/producers")
@PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'ROLE_ADMIN')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminProducerController {

  private final ProducerService producerService;

  @GetMapping
  public ResponseEntity<ApiResponse<Page<ProducerDto.Response>>> getAllProducers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean isActive,
      @RequestParam(required = false) String country,
      @RequestParam(required = false) Boolean hasProducts,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    Sort sort =
        sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<ProducerDto.Response> result =
        producerService.getAllProducers(pageable, keyword, isActive, country, hasProducts);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhà sản xuất thành công", result));
  }

  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<ProducerDto.AdminStatsResponse>> getStats() {
    return ResponseEntity.ok(
        ApiResponse.success("Lấy thống kê thương hiệu thành công", producerService.getAdminStats()));
  }

  @GetMapping("/all")
  public ResponseEntity<ApiResponse<List<ProducerDto.SlimResponse>>> getAllProducersForDropdown(
      @RequestParam(required = false) Boolean isActive) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Lấy danh sách nhà sản xuất thành công", producerService.getAllProducersSlim(isActive)));
  }

  @GetMapping("/code/{code}")
  public ResponseEntity<ApiResponse<ProducerDto.Response>> getProducerByCode(
      @PathVariable String code) {
    return ResponseEntity.ok(
        ApiResponse.success("Chi tiết nhà sản xuất", producerService.getProducerByCode(code)));
  }

  @PostMapping("/validate-code")
  public ResponseEntity<ApiResponse<ProducerDto.ValidateCodeResponse>> validateCode(
      @Valid @RequestBody ProducerDto.ValidateCodeRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Kiểm tra mã thành công", producerService.validateCode(request)));
  }

  @GetMapping("/{id:\\d+}")
  public ResponseEntity<ApiResponse<ProducerDto.Response>> getProducerById(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Chi tiết nhà sản xuất", producerService.getProducerById(id)));
  }

  @GetMapping("/{id:\\d+}/products")
  public ResponseEntity<ApiResponse<Page<ProducerDto.ProductSummary>>> getProducerProducts(
      @PathVariable Integer id,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {
    Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
    return ResponseEntity.ok(
        ApiResponse.success(
            "Lấy sản phẩm theo thương hiệu thành công",
            producerService.getProducerProducts(id, pageable)));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<ProducerDto.Response>> createProducer(
      @Valid @RequestBody ProducerDto.Request request) {
    ProducerDto.Response created = producerService.createProducer(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo nhà sản xuất thành công", created));
  }

  @PutMapping("/{id:\\d+}")
  public ResponseEntity<ApiResponse<ProducerDto.Response>> updateProducer(
      @PathVariable Integer id, @Valid @RequestBody ProducerDto.UpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật nhà sản xuất thành công", producerService.updateProducer(id, request)));
  }

  @PatchMapping("/{id:\\d+}/toggle-status")
  public ResponseEntity<ApiResponse<ProducerDto.Response>> toggleStatus(@PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật trạng thái thành công", producerService.toggleStatus(id)));
  }

  @PatchMapping("/bulk-status")
  public ResponseEntity<ApiResponse<List<ProducerDto.Response>>> bulkUpdateStatus(
      @Valid @RequestBody ProducerDto.BulkStatusRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Cập nhật trạng thái hàng loạt thành công",
            producerService.bulkUpdateStatus(request)));
  }

  @DeleteMapping("/{id:\\d+}")
  public ResponseEntity<ApiResponse<Void>> deleteProducer(@PathVariable Integer id) {
    producerService.deleteProducer(id);
    return ResponseEntity.ok(ApiResponse.success("Xóa nhà sản xuất thành công", null));
  }

  @DeleteMapping("/{id:\\d+}/hard")
  public ResponseEntity<ApiResponse<Void>> hardDeleteProducer(@PathVariable Integer id) {
    producerService.hardDeleteProducer(id);
    return ResponseEntity.ok(ApiResponse.success("Xóa cứng nhà sản xuất thành công", null));
  }
}
