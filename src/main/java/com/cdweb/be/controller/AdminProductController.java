package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ProductDto;
import com.cdweb.be.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
@Tag(name = "Quản trị Sản phẩm (Admin Product)", description = "Các API dành cho Admin quản lý kho hàng, variant và hình ảnh")
public class AdminProductController {

  @Autowired private ProductService productService;

  // ═══════════════════════════════════════════════════════════════════
  // ██  CRUD Sản phẩm
  // ═══════════════════════════════════════════════════════════════════

  /**
   * GET /api/admin/products Lấy danh sách sản phẩm (bao gồm cả inactive) — hỗ trợ tìm kiếm, lọc,
   * phân trang, sắp xếp.
   */
  @GetMapping
  @Operation(summary = "Danh sách sản phẩm (Admin)", description = "Lấy tất cả sản phẩm, hỗ trợ tìm kiếm, lọc và phân trang nâng cao")
  public ResponseEntity<ApiResponse<Page<ProductDto.AdminProductListResponse>>> getAllProducts(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "isActive", required = false) Boolean isActive,
      @RequestParam(value = "productTypeId", required = false) Integer productTypeId,
      @RequestParam(value = "producerId", required = false) Integer producerId,
      @RequestParam(value = "sortBy", defaultValue = "createdAt") String sortBy,
      @RequestParam(value = "sortDir", defaultValue = "desc") String sortDir) {

    Sort sort =
        sortDir.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<ProductDto.AdminProductListResponse> products =
        productService.adminGetAllProducts(keyword, isActive, productTypeId, producerId, pageable);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", products));
  }

  /**
   * GET /api/admin/products/{id} Lấy chi tiết sản phẩm (bao gồm variants, images, specifications).
   */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> getProductById(
      @PathVariable("id") Integer id) {
    ProductDto.AdminProductResponse product = productService.adminGetProductById(id);
    return ResponseEntity.ok(ApiResponse.success("Chi tiết sản phẩm", product));
  }

  /**
   * POST /api/admin/products Tạo sản phẩm mới — hỗ trợ tạo kèm variants, images, specifications
   * trong 1 request.
   */
  @PostMapping
  @Operation(summary = "Tạo mới sản phẩm", description = "Tạo sản phẩm kèm theo các biến thể và hình ảnh trong một lần gọi")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> createProduct(
      @Valid @RequestBody ProductDto.AdminCreateRequest request) {
    ProductDto.AdminProductResponse product = productService.adminCreateProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo sản phẩm thành công", product));
  }

  /**
   * PUT /api/admin/products/{id} Cập nhật thông tin sản phẩm (partial update — chỉ cập nhật các
   * trường được gửi lên).
   */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> updateProduct(
      @PathVariable("id") Integer id, @Valid @RequestBody ProductDto.AdminUpdateRequest request) {
    ProductDto.AdminProductResponse product = productService.adminUpdateProduct(id, request);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật sản phẩm thành công", product));
  }

  /** DELETE /api/admin/products/{id} Xoá sản phẩm (soft delete — chuyển trạng thái inactive). */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("id") Integer id) {
    productService.adminDeleteProduct(id);
    return ResponseEntity.ok(ApiResponse.success("Xoá sản phẩm thành công", null));
  }

  /** PUT /api/admin/products/{id}/toggle-status Bật/tắt trạng thái hoạt động của sản phẩm. */
  @PutMapping("/{id}/toggle-status")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> toggleStatus(
      @PathVariable("id") Integer id) {
    ProductDto.AdminProductResponse product = productService.adminToggleStatus(id);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật trạng thái thành công", product));
  }

  // ═══════════════════════════════════════════════════════════════════
  // ██  Quản lý Variant
  // ═══════════════════════════════════════════════════════════════════

  /** POST /api/admin/products/{id}/variants Thêm variant mới cho sản phẩm. */
  @PostMapping("/{id}/variants")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> addVariant(
      @PathVariable("id") Integer id, @Valid @RequestBody ProductDto.AdminVariantRequest request) {
    ProductDto.AdminProductResponse product = productService.adminAddVariant(id, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Thêm variant thành công", product));
  }

  /** PUT /api/admin/products/{productId}/variants/{variantId} Cập nhật variant của sản phẩm. */
  @PutMapping("/{productId}/variants/{variantId}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> updateVariant(
      @PathVariable("productId") Integer productId,
      @PathVariable("variantId") Integer variantId,
      @Valid @RequestBody ProductDto.AdminVariantRequest request) {
    ProductDto.AdminProductResponse product =
        productService.adminUpdateVariant(productId, variantId, request);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật variant thành công", product));
  }

  /**
   * DELETE /api/admin/products/{productId}/variants/{variantId} Vô hiệu hóa variant của sản phẩm
   * (Soft Delete — bảo toàn lịch sử đơn hàng).
   */
  @DeleteMapping("/{productId}/variants/{variantId}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> deleteVariant(
      @PathVariable("productId") Integer productId, @PathVariable("variantId") Integer variantId) {
    ProductDto.AdminProductResponse product =
        productService.adminDeleteVariant(productId, variantId);
    return ResponseEntity.ok(ApiResponse.success("Vô hiệu hóa variant thành công", product));
  }

  // ═══════════════════════════════════════════════════════════════════
  // ██  Quản lý Hình ảnh
  // ═══════════════════════════════════════════════════════════════════

  /** POST /api/admin/products/{id}/images Thêm hình ảnh cho sản phẩm. */
  @PostMapping("/{id}/images")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> addImage(
      @PathVariable("id") Integer id, @Valid @RequestBody ProductDto.AdminImageRequest request) {
    ProductDto.AdminProductResponse product = productService.adminAddImage(id, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Thêm hình ảnh thành công", product));
  }

  /** DELETE /api/admin/products/{productId}/images/{imageId} Xoá hình ảnh của sản phẩm. */
  @DeleteMapping("/{productId}/images/{imageId}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> deleteImage(
      @PathVariable("productId") Integer productId, @PathVariable("imageId") Integer imageId) {
    ProductDto.AdminProductResponse product = productService.adminDeleteImage(productId, imageId);
    return ResponseEntity.ok(ApiResponse.success("Xoá hình ảnh thành công", product));
  }

  // ═══════════════════════════════════════════════════════════════════
  // ██  Thống kê
  // ═══════════════════════════════════════════════════════════════════

  /** GET /api/admin/products/stats Lấy thống kê sản phẩm (tổng, active, inactive). */
  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<Map<String, Object>>> getProductStats() {
    Map<String, Object> stats = productService.adminGetProductStats();
    return ResponseEntity.ok(ApiResponse.success("Thống kê sản phẩm", stats));
  }
}
