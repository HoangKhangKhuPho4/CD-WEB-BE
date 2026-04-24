package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.ProductDto;
import com.cdweb.be.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@CrossOrigin(origins = "*")
@Tag(name = "Sản phẩm (Product)", description = "Các API dành cho việc tìm kiếm và xem thông tin sản phẩm")
public class ProductController {

  @Autowired private ProductService productService;

  @Autowired private com.cdweb.be.service.AIRecommendationService aiRecommendationService;

  @GetMapping
  @Operation(summary = "Lấy danh sách tất cả sản phẩm", description = "Hỗ trợ phân trang mặc định 10 sản phẩm/trang")
  public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getAllProducts(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<ProductDto.Response> products = productService.getAllProducts(pageable);
    return ResponseEntity.ok(ApiResponse.success("Products retrieved successfully", products));
  }

  @GetMapping("/{id}")
  @Operation(summary = "Xem chi tiết một sản phẩm", description = "Lấy thông tin đầy đủ của sản phẩm theo ID")
  public ResponseEntity<ApiResponse<ProductDto.Response>> getProductById(
      @PathVariable("id") Integer id) {
    ProductDto.Response product = productService.getProductById(id.longValue());
    return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
  }

  @GetMapping("/{id}/variants")
  public ResponseEntity<ApiResponse<List<ProductDto.VariantDto>>> getVariantsByProductId(
      @PathVariable("id") Integer id) {
    List<ProductDto.VariantDto> variants = productService.getVariantsByProductId(id.longValue());
    return ResponseEntity.ok(
        ApiResponse.success("Product variants retrieved successfully", variants));
  }

  @GetMapping("/sku/{sku}")
  public ResponseEntity<ApiResponse<ProductDto.Response>> getProductBySku(
      @PathVariable("sku") String sku) {
    ProductDto.Response product = productService.getProductBySku(sku);
    return ResponseEntity.ok(ApiResponse.success("Product retrieved successfully", product));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<ProductDto.Response>> createProduct(
      @Valid @RequestBody ProductDto.CreateRequest createRequest) {
    ProductDto.Response product = productService.createProduct(createRequest);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Product created successfully", product));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<ProductDto.Response>> updateProduct(
      @PathVariable("id") Integer id, @Valid @RequestBody ProductDto.UpdateRequest updateRequest) {
    ProductDto.Response product = productService.updateProduct(id.longValue(), updateRequest);
    return ResponseEntity.ok(ApiResponse.success("Product updated successfully", product));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable("id") Integer id) {
    productService.deleteProduct(id.longValue());
    return ResponseEntity.ok(ApiResponse.success("Product deleted successfully", null));
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> searchProducts(
      @RequestParam(name = "keyword", required = false) String keyword,
      @RequestParam(value = "product_type_id", required = false) Integer productTypeId,
      @RequestParam(value = "min_price", required = false) String minPrice,
      @RequestParam(value = "max_price", required = false) String maxPrice,
      @RequestParam(value = "producer_id", required = false) Integer producerId,
      @RequestParam(value = "min_rating", required = false) Double minRating,
      @RequestParam(value = "color", required = false) String color,
      @RequestParam(value = "sort_by", defaultValue = "name") String sortBy,
      @RequestParam(value = "sort_dir", defaultValue = "asc") String sortDir,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size) {

    ProductDto.SearchRequest searchRequest = new ProductDto.SearchRequest();
    searchRequest.setKeyword(keyword);
    searchRequest.setProductTypeId(productTypeId);
    if (minPrice != null && !minPrice.trim().isEmpty()) {
      searchRequest.setMinPrice(Double.parseDouble(minPrice));
    }
    if (maxPrice != null && !maxPrice.trim().isEmpty()) {
      searchRequest.setMaxPrice(Double.parseDouble(maxPrice));
    }
    searchRequest.setProducerId(producerId);
    searchRequest.setMinRating(minRating);
    searchRequest.setColor(color);
    searchRequest.setSortBy(sortBy);
    searchRequest.setSortDir(sortDir);
    searchRequest.setPage(page);
    searchRequest.setSize(size);

    Page<ProductDto.Response> products = productService.searchProducts(searchRequest);
    return ResponseEntity.ok(ApiResponse.success("Products search completed", products));
  }

  @GetMapping("/product-type/{productTypeId}")
  public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getProductsByProductType(
      @PathVariable("productTypeId") Integer productTypeId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<ProductDto.Response> products =
        productService.getProductsByCategory(productTypeId.longValue(), pageable);
    return ResponseEntity.ok(
        ApiResponse.success("Products by product type retrieved successfully", products));
  }

  @GetMapping("/producer/{producerId}")
  public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getProductsByProducer(
      @PathVariable("producerId") Integer producerId,
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<ProductDto.Response> products =
        productService.getProductsByBrand(producerId.toString(), pageable);
    return ResponseEntity.ok(
        ApiResponse.success("Products by producer retrieved successfully", products));
  }

  @GetMapping("/best-sellers")
  public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getBestSellingProducts(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "8") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<ProductDto.Response> products = productService.getBestSellingProducts(pageable);
    return ResponseEntity.ok(
        ApiResponse.success("Best selling products retrieved successfully", products));
  }

  @GetMapping("/brands")
  public ResponseEntity<ApiResponse<List<String>>> getAllBrands() {
    List<String> brands = productService.getAllBrands();
    return ResponseEntity.ok(ApiResponse.success("Brands retrieved successfully", brands));
  }

  @PutMapping("/{id}/stock")
  @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<Void>> updateStock(
      @PathVariable("id") Integer id, @RequestParam("quantity") Integer quantity) {
    productService.updateStock(id.longValue(), quantity);
    return ResponseEntity.ok(ApiResponse.success("Stock updated successfully", null));
  }

  @GetMapping("/featured")
  public ResponseEntity<ApiResponse<Page<ProductDto.Response>>> getFeaturedProducts(
      @RequestParam(name = "page", defaultValue = "0") int page,
      @RequestParam(name = "size", defaultValue = "8") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<ProductDto.Response> products = productService.getFeaturedProducts(pageable);
    return ResponseEntity.ok(
        ApiResponse.success("Featured products retrieved successfully", products));
  }

  @GetMapping("/recommendations")
  public ResponseEntity<ApiResponse<List<ProductDto.Response>>> getRecommendations(
      @RequestParam("userId") Integer userId) {
    List<ProductDto.Response> products = aiRecommendationService.getRecommendationsForUser(userId);
    return ResponseEntity.ok(
        ApiResponse.success("AI Recommendations retrieved successfully", products));
  }
}
