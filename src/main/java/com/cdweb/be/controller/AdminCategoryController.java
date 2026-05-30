package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.CategoryDto;
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
@RequestMapping("/api/admin/categories")
@PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'PRODUCT_CREATE', 'PRODUCT_UPDATE', 'ROLE_ADMIN')")
@CrossOrigin(origins = "*")
public class AdminCategoryController {

  @Autowired private CategoryService categoryService;

  @GetMapping
  public ResponseEntity<ApiResponse<Page<CategoryDto.Response>>> getAllCategories(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword) {

    Pageable pageable = PageRequest.of(page, size);

    Page<CategoryDto.Response> categories;
    if (keyword != null && !keyword.trim().isEmpty()) {
      categories = categoryService.searchCategories(keyword, pageable);
    } else {
      categories = categoryService.getAllCategories(pageable);
    }

    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách danh mục thành công", categories));
  }

  /**
   * GET /api/admin/categories/all Trả về tất cả danh mục (không phân trang) để populate dropdown
   * trong Admin form.
   */
  @GetMapping("/all")
  public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> getAllCategoriesAsList() {
    List<CategoryDto.Response> categories = categoryService.getAllCategoriesAsList();
    return ResponseEntity.ok(ApiResponse.success("Lấy tất cả danh mục thành công", categories));
  }

  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<CategoryDto.Response>> getCategoryById(@PathVariable Long id) {
    CategoryDto.Response category = categoryService.getCategoryById(id);
    return ResponseEntity.ok(ApiResponse.success("Chi tiết danh mục thành công", category));
  }

  @PostMapping
  public ResponseEntity<ApiResponse<CategoryDto.Response>> createCategory(
      @Valid @RequestBody CategoryDto.CreateRequest createRequest) {
    CategoryDto.Response category = categoryService.createCategory(createRequest);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Tạo danh mục thành công", category));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<CategoryDto.Response>> updateCategory(
      @PathVariable Long id, @Valid @RequestBody CategoryDto.UpdateRequest updateRequest) {
    CategoryDto.Response category = categoryService.updateCategory(id, updateRequest);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật danh mục thành công", category));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable Long id) {
    categoryService.deleteCategory(id);
    return ResponseEntity.ok(ApiResponse.success("Xoá danh mục thành công", null));
  }

  @PatchMapping("/{id}/toggle-status")
  public ResponseEntity<ApiResponse<CategoryDto.Response>> toggleStatus(@PathVariable Long id) {
    CategoryDto.Response category = categoryService.toggleStatus(id);
    return ResponseEntity.ok(
        ApiResponse.success("Thay đổi trạng thái danh mục thành công", category));
  }
}
