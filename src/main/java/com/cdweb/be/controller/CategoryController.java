package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.CategoryDto;
import com.cdweb.be.dto.ProductDto;
import com.cdweb.be.service.CategoryService;
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
@RequestMapping("/api/categories")
@CrossOrigin(origins = "*")
public class CategoryController {

  @Autowired private CategoryService categoryService;

  @GetMapping
  public ResponseEntity<ApiResponse<Page<CategoryDto.Response>>> getAllCategories(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<CategoryDto.Response> categories = categoryService.getAllCategories(pageable);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", categories));
  }

  @GetMapping("/list")
  public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getAllCategoriesAsList() {
    List<CategoryDto.Response> categories = categoryService.getAllCategoriesAsList();
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", categories));
  }

  @GetMapping("/root")
  public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getRootCategories() {
    List<CategoryDto.Response> categories = categoryService.getRootCategories();
    return ResponseEntity.ok(ApiResponse.success("Lấy danh mục gốc thành công", categories));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CategoryDto.Response>> getCategoryById(@PathVariable Long id) {
    CategoryDto.Response category = categoryService.getCategoryById(id);
    return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết danh mục thành công", category));
  }

  @GetMapping("/slug/{code}")
  public ResponseEntity<ApiResponse<CategoryDto.Response>> getCategoryByCode(
      @PathVariable String code) {
    CategoryDto.Response category = categoryService.getCategoryBySlug(code);
    return ResponseEntity.ok(ApiResponse.success("Lấy chi tiết danh mục thành công", category));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<CategoryDto.Response>> createCategory(
      @Valid @RequestBody CategoryDto.CreateRequest createRequest) {
    CategoryDto.Response category = categoryService.createCategory(createRequest);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo danh mục thành công", category));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<CategoryDto.Response>> updateCategory(
      @PathVariable Long id, @Valid @RequestBody CategoryDto.UpdateRequest updateRequest) {
    CategoryDto.Response category = categoryService.updateCategory(id, updateRequest);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", category));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("hasAuthority('PRODUCT_MANAGE')")
  public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.ok(ApiResponse.success("Xoá danh mục thành công", null));
  }

  @GetMapping("/{parentId}/subcategories")
  public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getSubCategories(
      @PathVariable Long parentId) {
    List<CategoryDto.Response> subCategories = categoryService.getSubCategories(parentId);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh mục con thành công", subCategories));
  }

  @GetMapping("/search")
  public ResponseEntity<ApiResponse<Page<CategoryDto.Response>>> searchCategories(
      @RequestParam String keyword,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size) {

    Pageable pageable = PageRequest.of(page, size);
    Page<CategoryDto.Response> categories = categoryService.searchCategories(keyword, pageable);
    return ResponseEntity.ok(ApiResponse.success("Tìm kiếm danh mục hoàn tất", categories));
  }

  @GetMapping("/{id}/variants")
  public ResponseEntity<ApiResponse<List<ProductDto.VariantDto>>> getVariantsByCategory(
      @PathVariable Long id) {
    List<ProductDto.VariantDto> variants = categoryService.getVariantsByCategory(id);
    return ResponseEntity.ok(
        ApiResponse.success("Lấy biến thể theo danh mục thành công", variants));
  }

  @GetMapping("/home")
  public ResponseEntity<ApiResponse<List<CategoryDto.CategoryWithProductsResponse>>>
      getHomeCategories() {
    List<CategoryDto.CategoryWithProductsResponse> result = categoryService.getHomeCategories();
    return ResponseEntity.ok(ApiResponse.success("Lấy danh mục trang chủ thành công", result));
  }
}
