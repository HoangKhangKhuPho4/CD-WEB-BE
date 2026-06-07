package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ProductDto;
import com.cdweb.be.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Admin API — Quản lý Sản phẩm (đủ scenario kiểm thử). */
@RestController
@RequestMapping("/api/admin/products")
@PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'ROLE_ADMIN')")
@Tag(name = "Quản trị Sản phẩm (Admin Product)", description = "API quản lý sản phẩm, variant, hình ảnh")
@CrossOrigin(origins = "*")
public class AdminProductController {

  @Autowired private ProductService productService;

  @GetMapping
  @Operation(summary = "Danh sách sản phẩm (Admin)")
  public ResponseEntity<ApiResponse<Page<ProductDto.AdminProductListResponse>>> getAllProducts(
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "10") int size,
      @RequestParam(value = "keyword", required = false) String keyword,
      @RequestParam(value = "isActive", required = false) Boolean isActive,
      @RequestParam(value = "isFeatured", required = false) Boolean isFeatured,
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
        productService.adminGetAllProducts(
            keyword, isActive, productTypeId, producerId, isFeatured, pageable);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách sản phẩm thành công", products));
  }

  @GetMapping("/stats")
  public ResponseEntity<ApiResponse<ProductDto.AdminStatsResponse>> getProductStats() {
    return ResponseEntity.ok(
        ApiResponse.success("Thống kê sản phẩm", productService.adminGetProductStats()));
  }

  @PostMapping("/validate-sku")
  public ResponseEntity<ApiResponse<ProductDto.ValidateSkuResponse>> validateSku(
      @Valid @RequestBody ProductDto.ValidateSkuRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Kiểm tra SKU", productService.validateSku(request)));
  }

  @PatchMapping("/bulk-status")
  public ResponseEntity<ApiResponse<ProductDto.BulkStatusResult>> bulkStatus(
      @Valid @RequestBody ProductDto.BulkStatusRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật hàng loạt", productService.adminBulkStatus(request)));
  }

  @GetMapping("/slug/{slug}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> getBySlug(
      @PathVariable String slug) {
    return ResponseEntity.ok(
        ApiResponse.success("Chi tiết sản phẩm theo slug", productService.adminGetProductBySlug(slug)));
  }

  @GetMapping("/{id:\\d+}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> getProductById(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Chi tiết sản phẩm", productService.adminGetProductById(id)));
  }

  @PostMapping
  @Operation(summary = "Tạo mới sản phẩm")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> createProduct(
      @Valid @RequestBody ProductDto.AdminCreateRequest request) {
    ProductDto.AdminProductResponse product = productService.adminCreateProduct(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo sản phẩm thành công", product));
  }

  @PutMapping("/{id:\\d+}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> updateProduct(
      @PathVariable Integer id, @Valid @RequestBody ProductDto.AdminUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật sản phẩm thành công", productService.adminUpdateProduct(id, request)));
  }

  @DeleteMapping("/{id:\\d+}")
  public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Integer id) {
    productService.adminDeleteProduct(id);
    return ResponseEntity.ok(ApiResponse.success("Xoá sản phẩm thành công", null));
  }

  @PutMapping("/{id:\\d+}/toggle-status")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> toggleStatus(
      @PathVariable Integer id) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật trạng thái thành công", productService.adminToggleStatus(id)));
  }

  @PatchMapping("/{id:\\d+}/featured")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> setFeatured(
      @PathVariable Integer id, @Valid @RequestBody ProductDto.FeaturedRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật nổi bật", productService.adminSetFeatured(id, request)));
  }

  @PostMapping("/{id:\\d+}/variants")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> addVariant(
      @PathVariable Integer id, @Valid @RequestBody ProductDto.AdminVariantRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Thêm variant thành công", productService.adminAddVariant(id, request)));
  }

  @PutMapping("/{productId:\\d+}/variants/{variantId:\\d+}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> updateVariant(
      @PathVariable Integer productId,
      @PathVariable Integer variantId,
      @Valid @RequestBody ProductDto.AdminVariantRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Cập nhật variant thành công",
            productService.adminUpdateVariant(productId, variantId, request)));
  }

  @PutMapping("/{productId:\\d+}/variants/{variantId:\\d+}/stock")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> setVariantStock(
      @PathVariable Integer productId,
      @PathVariable Integer variantId,
      @Valid @RequestBody ProductDto.VariantStockRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Cập nhật tồn kho variant",
            productService.adminSetVariantStock(productId, variantId, request)));
  }

  @DeleteMapping("/{productId:\\d+}/variants/{variantId:\\d+}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> deleteVariant(
      @PathVariable Integer productId, @PathVariable Integer variantId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Vô hiệu hóa variant thành công",
            productService.adminDeleteVariant(productId, variantId)));
  }

  @PostMapping("/{id:\\d+}/images")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> addImage(
      @PathVariable Integer id, @Valid @RequestBody ProductDto.AdminImageRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Thêm hình ảnh thành công", productService.adminAddImage(id, request)));
  }

  @DeleteMapping("/{productId:\\d+}/images/{imageId:\\d+}")
  public ResponseEntity<ApiResponse<ProductDto.AdminProductResponse>> deleteImage(
      @PathVariable Integer productId, @PathVariable Integer imageId) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Xoá hình ảnh thành công", productService.adminDeleteImage(productId, imageId)));
  }
}
