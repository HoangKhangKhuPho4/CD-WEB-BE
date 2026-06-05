package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.CmsContentDto;
import com.cdweb.be.entity.CmsContent;
import com.cdweb.be.entity.CmsContentType;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.CmsContentRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** CMS công khai cho storefront (bài viết, banner đang bật). */
@RestController
@RequestMapping("/api/cms")
@RequiredArgsConstructor
public class PublicCmsController {

  private final CmsContentRepository cmsContentRepository;

  @GetMapping("/posts")
  public ResponseEntity<ApiResponse<List<CmsContentDto.Response>>> listActivePosts() {
    List<CmsContentDto.Response> posts =
        cmsContentRepository
            .findByContentTypeAndActiveTrueOrderBySortOrderAscCreatedAtDesc(CmsContentType.POST)
            .stream()
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(ApiResponse.success("Lấy bài viết thành công", posts));
  }

  @GetMapping("/posts/{id}")
  public ResponseEntity<ApiResponse<CmsContentDto.Response>> getActivePost(
      @PathVariable Long id) {
    CmsContent entity =
        cmsContentRepository
            .findById(id)
            .filter(c -> c.getContentType() == CmsContentType.POST)
            .filter(c -> Boolean.TRUE.equals(c.getActive()))
            .orElseThrow(() -> new ResourceNotFoundException("CmsContent", "id", String.valueOf(id)));
    return ResponseEntity.ok(ApiResponse.success("Lấy bài viết thành công", toResponse(entity)));
  }

  @GetMapping("/banners")
  public ResponseEntity<ApiResponse<List<CmsContentDto.Response>>> listActiveBanners() {
    List<CmsContentDto.Response> banners =
        cmsContentRepository
            .findByContentTypeAndActiveTrueOrderBySortOrderAscCreatedAtDesc(CmsContentType.BANNER)
            .stream()
            .map(this::toResponse)
            .toList();
    return ResponseEntity.ok(ApiResponse.success("Lấy banner thành công", banners));
  }

  private CmsContentDto.Response toResponse(CmsContent entity) {
    return new CmsContentDto.Response(
        entity.getId(),
        entity.getTitle(),
        entity.getSubtitle(),
        entity.getLinkUrl(),
        entity.getImageUrl(),
        entity.getBody(),
        entity.getAuthor(),
        entity.getActive(),
        entity.getSortOrder(),
        entity.getCreatedAt(),
        entity.getUpdatedAt());
  }
}
