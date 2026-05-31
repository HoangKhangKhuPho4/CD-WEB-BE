package com.cdweb.be.repository;

import com.cdweb.be.entity.CmsContent;
import com.cdweb.be.entity.CmsContentType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CmsContentRepository extends JpaRepository<CmsContent, Long> {

  List<CmsContent> findByContentTypeOrderBySortOrderAscCreatedAtDesc(CmsContentType contentType);

  List<CmsContent> findByContentTypeAndActiveTrueOrderBySortOrderAscCreatedAtDesc(
      CmsContentType contentType);
}
