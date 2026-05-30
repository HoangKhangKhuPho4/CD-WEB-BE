package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.CategoryDto;
import com.cdweb.be.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Alias tương thích FE cũ — danh mục = product_types trong DB.
 * Ưu tiên dùng GET /api/categories/list trên FE mới.
 */
@RestController
@RequestMapping("/api/product-types")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class ProductTypePublicController {

  private final CategoryService categoryService;

  @GetMapping
  public ResponseEntity<ApiResponse<List<CategoryDto.Response>>> listActiveProductTypes() {
    List<CategoryDto.Response> categories = categoryService.getAllCategoriesAsList();
    return ResponseEntity.ok(
        ApiResponse.success("Lấy danh sách loại sản phẩm thành công", categories));
  }
}
