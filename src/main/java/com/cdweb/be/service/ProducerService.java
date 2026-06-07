package com.cdweb.be.service;

import com.cdweb.be.dto.ProducerDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProducerService {

  Page<ProducerDto.Response> getAllProducers(
      Pageable pageable, String keyword, Boolean isActive, String country, Boolean hasProducts);

  ProducerDto.AdminStatsResponse getAdminStats();

  List<ProducerDto.SlimResponse> getAllProducersSlim(Boolean isActive);

  ProducerDto.Response getProducerById(Integer id);

  ProducerDto.Response getProducerByCode(String code);

  ProducerDto.Response createProducer(ProducerDto.Request request);

  ProducerDto.Response updateProducer(Integer id, ProducerDto.UpdateRequest request);

  void deleteProducer(Integer id);

  void hardDeleteProducer(Integer id);

  ProducerDto.Response toggleStatus(Integer id);

  List<ProducerDto.Response> bulkUpdateStatus(ProducerDto.BulkStatusRequest request);

  ProducerDto.ValidateCodeResponse validateCode(ProducerDto.ValidateCodeRequest request);

  Page<ProducerDto.ProductSummary> getProducerProducts(Integer id, Pageable pageable);
}
