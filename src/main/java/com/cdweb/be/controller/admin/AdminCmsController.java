package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.CmsContentDto;
import com.cdweb.be.entity.CmsContentType;
import com.cdweb.be.service.CmsContentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/cms")
@RequiredArgsConstructor
@PreAuthorize("hasAnyAuthority('ROLE_ADMIN')")
public class AdminCmsController {

  private final CmsContentService cmsContentService;

  @GetMapping("/banners")
  public ResponseEntity<ApiResponse<List<CmsContentDto.Response>>> listBanners() {
    return ResponseEntity.ok(
        ApiResponse.success("Lấy danh sách banner thành công", cmsContentService.list(CmsContentType.BANNER)));
  }

  @PostMapping("/banners")
  public ResponseEntity<ApiResponse<CmsContentDto.Response>> createBanner(
      @Valid @RequestBody CmsContentDto.SaveRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Tạo banner thành công", cmsContentService.create(CmsContentType.BANNER, request)));
  }

  @PutMapping("/banners/{id}")
  public ResponseEntity<ApiResponse<CmsContentDto.Response>> updateBanner(
      @PathVariable Long id, @Valid @RequestBody CmsContentDto.SaveRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật banner thành công", cmsContentService.update(id, request)));
  }

  @PatchMapping("/banners/{id}/toggle")
  public ResponseEntity<ApiResponse<CmsContentDto.Response>> toggleBanner(@PathVariable Long id) {
    return ResponseEntity.ok(
        ApiResponse.success("Đã đổi trạng thái banner", cmsContentService.toggleActive(id)));
  }

  @DeleteMapping("/banners/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteBanner(@PathVariable Long id) {
    cmsContentService.delete(id);
    return ResponseEntity.ok(ApiResponse.success("Đã xóa banner", null));
  }

  @GetMapping("/posts")
  public ResponseEntity<ApiResponse<List<CmsContentDto.Response>>> listPosts() {
    return ResponseEntity.ok(
        ApiResponse.success("Lấy danh sách bài viết thành công", cmsContentService.list(CmsContentType.POST)));
  }

  @PostMapping("/posts")
  public ResponseEntity<ApiResponse<CmsContentDto.Response>> createPost(
      @Valid @RequestBody CmsContentDto.SaveRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Tạo bài viết thành công", cmsContentService.create(CmsContentType.POST, request)));
  }

  @PutMapping("/posts/{id}")
  public ResponseEntity<ApiResponse<CmsContentDto.Response>> updatePost(
      @PathVariable Long id, @Valid @RequestBody CmsContentDto.SaveRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success("Cập nhật bài viết thành công", cmsContentService.update(id, request)));
  }

  @PatchMapping("/posts/{id}/toggle")
  public ResponseEntity<ApiResponse<CmsContentDto.Response>> togglePost(@PathVariable Long id) {
    return ResponseEntity.ok(
        ApiResponse.success("Đã đổi trạng thái bài viết", cmsContentService.toggleActive(id)));
  }

  @DeleteMapping("/posts/{id}")
  public ResponseEntity<ApiResponse<Void>> deletePost(@PathVariable Long id) {
    cmsContentService.delete(id);
    return ResponseEntity.ok(ApiResponse.success("Đã xóa bài viết", null));
  }
}
