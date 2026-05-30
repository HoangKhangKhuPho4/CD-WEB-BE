package com.cdweb.be.service;

import com.cdweb.be.dto.CmsContentDto;
import com.cdweb.be.entity.CmsContentType;
import java.util.List;

public interface CmsContentService {

  List<CmsContentDto.Response> list(CmsContentType type);

  CmsContentDto.Response create(CmsContentType type, CmsContentDto.SaveRequest request);

  CmsContentDto.Response update(Long id, CmsContentDto.SaveRequest request);

  CmsContentDto.Response toggleActive(Long id);

  void delete(Long id);
}
