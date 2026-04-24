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

/** Admin API — Quản lý Nhà sản xuất / Thương hiệu. */
@RestController
@RequestMapping("/api/admin/producers")
@PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminProducerController {

  private final ProducerService producerService;

  /** GET /api/admin/producers Danh sách nhà sản xuất có phân trang và tìm kiếm. */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<ProducerDto.Response>>> getAllProducers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(required = false) Boolean isActive,
      @RequestParam(defaultValue = "id") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {
    Sort sort =
        sortDir.equalsIgnoreCase("desc")
            ? Sort.by(sortBy).descending()
            : Sort.by(sortBy).ascending();
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<ProducerDto.Response> result =
        producerService.getAllProducers(pageable, keyword, isActive);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách nhà sản xuất thành công", result));
  }

  /** GET /api/admin/producers/all Trả về danh sách rút gọn (Slim) để populate dropdown. */
  @GetMapping("/all")
  public ResponseEntity<ApiResponse<List<ProducerDto.SlimResponse>>> getAllProducersForDropdown() {
    List<ProducerDto.SlimResponse> producers = producerService.getAllProducersSlim(null);
    return ResponseEntity.ok(
        ApiResponse.success("Lấy danh sách nhà sản xuất thành công", producers));
  }

  /** GET /api/admin/producers/{id} Xem chi tiết một nhà sản xuất. */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ProducerDto.Response>> getProducerById(
      @PathVariable Integer id) {
    ProducerDto.Response producer = producerService.getProducerById(id);
    return ResponseEntity.ok(ApiResponse.success("Chi tiết nhà sản xuất", producer));
  }

  /** POST /api/admin/producers Tạo mới nhà sản xuất. */
  @PostMapping
  public ResponseEntity<ApiResponse<ProducerDto.Response>> createProducer(
      @Valid @RequestBody ProducerDto.Request request) {
    ProducerDto.Response created = producerService.createProducer(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo nhà sản xuất thành công", created));
  }

  /** PUT /api/admin/producers/{id} Cập nhật thông tin nhà sản xuất. */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ProducerDto.Response>> updateProducer(
      @PathVariable Integer id, @Valid @RequestBody ProducerDto.Request request) {
    ProducerDto.Response updated = producerService.updateProducer(id, request);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật nhà sản xuất thành công", updated));
  }

  /**
   * DELETE /api/admin/producers/{id} Xóa nhà sản xuất (Xóa vĩnh viễn hoặc vô hiệu hóa tùy theo ràng
   * buộc dữ liệu).
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteProducer(@PathVariable Integer id) {
    producerService.deleteProducer(id);
    return ResponseEntity.ok(ApiResponse.success("Xóa nhà sản xuất thành công", null));
  }

  /** PATCH /api/admin/producers/{id}/toggle-status Bật/Tắt trạng thái hoạt động. */
  @PatchMapping("/{id}/toggle-status")
  public ResponseEntity<ApiResponse<ProducerDto.Response>> toggleStatus(@PathVariable Integer id) {
    ProducerDto.Response updated = producerService.toggleStatus(id);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", updated));
  }
}
