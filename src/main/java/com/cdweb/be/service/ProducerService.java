package com.cdweb.be.service;

import com.cdweb.be.dto.ProducerDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProducerService {
  Page<ProducerDto.Response> getAllProducers(Pageable pageable, String keyword, Boolean isActive);

  List<ProducerDto.SlimResponse> getAllProducersSlim(Boolean isActive);

  ProducerDto.Response getProducerById(Integer id);

  ProducerDto.Response createProducer(ProducerDto.Request request);

  ProducerDto.Response updateProducer(Integer id, ProducerDto.Request request);

  void deleteProducer(Integer id);

  ProducerDto.Response toggleStatus(Integer id);
}
