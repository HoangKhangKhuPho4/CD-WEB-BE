package com.cdweb.be.service.impl;

import com.cdweb.be.dto.ProducerDto;
import com.cdweb.be.entity.Producer;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.ProducerRepository;
import com.cdweb.be.service.AuditLogService;
import com.cdweb.be.service.ProducerService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProducerServiceImpl implements ProducerService {

  private final ProducerRepository producerRepository;
  private final AuditLogService auditLogService;

  @Override
  public Page<ProducerDto.Response> getAllProducers(
      Pageable pageable, String keyword, Boolean isActive) {
    Page<Producer> producers;
    if (keyword != null && !keyword.isEmpty()) {
      if (isActive != null) {
        producers =
            producerRepository
                .findByIsActiveAndNameContainingIgnoreCaseOrIsActiveAndCodeContainingIgnoreCase(
                    isActive, keyword, isActive, keyword, pageable);
      } else {
        producers =
            producerRepository.findByNameContainingIgnoreCaseOrCodeContainingIgnoreCase(
                keyword, keyword, pageable);
      }
    } else {
      producers = producerRepository.findAll(pageable);
    }

    return producers.map(this::mapToResponse);
  }

  @Override
  public List<ProducerDto.SlimResponse> getAllProducersSlim(Boolean isActive) {
    List<Producer> producers = producerRepository.findAll();
    if (isActive != null) {
      producers =
          producers.stream()
              .filter(p -> p.getIsActive().equals(isActive))
              .collect(Collectors.toList());
    }
    return producers.stream()
        .map(p -> new ProducerDto.SlimResponse(p.getId(), p.getName(), p.getCode()))
        .collect(Collectors.toList());
  }

  @Override
  public ProducerDto.Response getProducerById(Integer id) {
    Producer producer =
        producerRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy Nhà sản xuất với ID: " + id));
    return mapToResponse(producer);
  }

  @Override
  @Transactional
  public ProducerDto.Response createProducer(ProducerDto.Request request) {
    if (producerRepository.existsByCode(request.getCode())) {
      throw new RuntimeException("Mã nhà sản xuất đã tồn tại: " + request.getCode());
    }

    Producer producer = new Producer();
    mapToEntity(request, producer);

    Producer savedProducer = producerRepository.save(producer);

    auditLogService.log(
        "CREATE",
        "Producer",
        savedProducer.getId().toString(),
        "Tạo mới nhà sản xuất: " + savedProducer.getName());

    return mapToResponse(savedProducer);
  }

  @Override
  @Transactional
  public ProducerDto.Response updateProducer(Integer id, ProducerDto.Request request) {
    Producer producer =
        producerRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy Nhà sản xuất với ID: " + id));

    // Check if code is changed and if new code already exists
    if (!producer.getCode().equals(request.getCode())
        && producerRepository.existsByCode(request.getCode())) {
      throw new RuntimeException("Mã nhà sản xuất mới đã tồn tại: " + request.getCode());
    }

    mapToEntity(request, producer);
    Producer updatedProducer = producerRepository.save(producer);

    auditLogService.log(
        "UPDATE",
        "Producer",
        updatedProducer.getId().toString(),
        "Cập nhật nhà sản xuất: " + updatedProducer.getName());

    return mapToResponse(updatedProducer);
  }

  @Override
  @Transactional
  public void deleteProducer(Integer id) {
    Producer producer =
        producerRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy Nhà sản xuất với ID: " + id));

    // Soft delete or hard delete? Usually better to Soft delete or check products
    if (producer.getProducts() != null && !producer.getProducts().isEmpty()) {
      // If has products, toggle to inactive instead of deleting or throw error
      producer.setIsActive(false);
      producerRepository.save(producer);
      auditLogService.log(
          "SOFT_DELETE",
          "Producer",
          id.toString(),
          "Vô hiệu hóa nhà sản xuất do còn sản phẩm: " + producer.getName());
    } else {
      producerRepository.delete(producer);
      auditLogService.log(
          "DELETE", "Producer", id.toString(), "Xóa vĩnh viễn nhà sản xuất: " + producer.getName());
    }
  }

  @Override
  @Transactional
  public ProducerDto.Response toggleStatus(Integer id) {
    Producer producer =
        producerRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Không tìm thấy Nhà sản xuất với ID: " + id));

    producer.setIsActive(!producer.getIsActive());
    Producer saved = producerRepository.save(producer);

    auditLogService.log(
        "TOGGLE_STATUS",
        "Producer",
        id.toString(),
        "Thay đổi trạng thái nhà sản xuất: " + (saved.getIsActive() ? "ACTIVE" : "INACTIVE"));

    return mapToResponse(saved);
  }

  private void mapToEntity(ProducerDto.Request request, Producer producer) {
    producer.setName(request.getName());
    producer.setCode(request.getCode());
    producer.setLogoUrl(request.getLogoUrl());
    producer.setDescription(request.getDescription());
    producer.setCountry(request.getCountry());
    producer.setWebsite(request.getWebsite());
    if (request.getIsActive() != null) {
      producer.setIsActive(request.getIsActive());
    }
  }

  private ProducerDto.Response mapToResponse(Producer producer) {
    return new ProducerDto.Response(
        producer.getId(),
        producer.getName(),
        producer.getCode(),
        producer.getLogoUrl(),
        producer.getDescription(),
        producer.getCountry(),
        producer.getWebsite(),
        producer.getIsActive(),
        producer.getCreatedAt());
  }
}
