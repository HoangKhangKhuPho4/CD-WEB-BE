package com.cdweb.be.service.impl;

import com.cdweb.be.dto.CmsContentDto;
import com.cdweb.be.entity.CmsContent;
import com.cdweb.be.entity.CmsContentType;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.CmsContentRepository;
import com.cdweb.be.service.CmsContentService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class CmsContentServiceImpl implements CmsContentService {

  private final CmsContentRepository repository;

  @Override
  @Transactional(readOnly = true)
  public List<CmsContentDto.Response> list(CmsContentType type) {
    return repository.findByContentTypeOrderBySortOrderAscCreatedAtDesc(type).stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public CmsContentDto.Response create(CmsContentType type, CmsContentDto.SaveRequest request) {
    CmsContent entity = new CmsContent();
    entity.setContentType(type);
    apply(entity, request);
    return toResponse(repository.save(entity));
  }

  @Override
  public CmsContentDto.Response update(Long id, CmsContentDto.SaveRequest request) {
    CmsContent entity = getEntity(id);
    apply(entity, request);
    return toResponse(repository.save(entity));
  }

  @Override
  public CmsContentDto.Response toggleActive(Long id) {
    CmsContent entity = getEntity(id);
    entity.setActive(!Boolean.TRUE.equals(entity.getActive()));
    return toResponse(repository.save(entity));
  }

  @Override
  public void delete(Long id) {
    if (!repository.existsById(id)) {
      throw new ResourceNotFoundException("CmsContent", "id", String.valueOf(id));
    }
    repository.deleteById(id);
  }

  private CmsContent getEntity(Long id) {
    return repository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("CmsContent", "id", String.valueOf(id)));
  }

  private void apply(CmsContent entity, CmsContentDto.SaveRequest request) {
    entity.setTitle(request.getTitle().trim());
    entity.setSubtitle(request.getSubtitle());
    entity.setLinkUrl(request.getLinkUrl());
    entity.setImageUrl(request.getImageUrl());
    entity.setBody(request.getBody());
    entity.setAuthor(request.getAuthor());
    if (request.getActive() != null) {
      entity.setActive(request.getActive());
    }
    if (request.getSortOrder() != null) {
      entity.setSortOrder(request.getSortOrder());
    }
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
