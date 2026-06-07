package com.cdweb.be.service.impl;

import com.cdweb.be.dto.ProducerDto;
import com.cdweb.be.entity.Producer;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.ProducerRepository;
import com.cdweb.be.repository.ProducerSpecification;
import com.cdweb.be.repository.ProductRepository;
import com.cdweb.be.service.AuditLogService;
import com.cdweb.be.service.ProducerService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProducerServiceImpl implements ProducerService {

  private final ProducerRepository producerRepository;
  private final ProductRepository productRepository;
  private final AuditLogService auditLogService;

  @Override
  @Transactional(readOnly = true)
  public Page<ProducerDto.Response> getAllProducers(
      Pageable pageable, String keyword, Boolean isActive, String country, Boolean hasProducts) {
    Specification<Producer> spec =
        ProducerSpecification.adminFilter(keyword, isActive, country, hasProducts);
    return producerRepository.findAll(spec, pageable).map(this::mapToResponse);
  }

  @Override
  @Transactional(readOnly = true)
  public ProducerDto.AdminStatsResponse getAdminStats() {
    long total = producerRepository.count();
    long active = producerRepository.countByIsActiveTrue();
    long inactive = producerRepository.countByIsActiveFalse();
    long withProducts = productRepository.countDistinctProducersWithProducts();
    long totalLinkedProducts = productRepository.count();

    ProducerDto.AdminStatsResponse stats = new ProducerDto.AdminStatsResponse();
    stats.setTotal(total);
    stats.setActive(active);
    stats.setInactive(inactive);
    stats.setWithProducts(withProducts);
    stats.setWithoutProducts(Math.max(0, total - withProducts));
    stats.setTotalLinkedProducts(totalLinkedProducts);
    return stats;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProducerDto.SlimResponse> getAllProducersSlim(Boolean isActive) {
    List<Producer> producers;
    if (isActive != null) {
      producers = producerRepository.findByIsActive(isActive);
    } else {
      producers = producerRepository.findAll();
    }
    return producers.stream().map(this::mapToSlim).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ProducerDto.Response getProducerById(Integer id) {
    return mapToResponse(findProducerOrThrow(id));
  }

  @Override
  @Transactional(readOnly = true)
  public ProducerDto.Response getProducerByCode(String code) {
    return mapToResponse(findProducerByCodeOrThrow(normalizeCode(code)));
  }

  @Override
  @Transactional
  public ProducerDto.Response createProducer(ProducerDto.Request request) {
    validateCreateRequest(request);
    String code = normalizeCode(request.getCode());

    if (producerRepository.existsByCode(code)) {
      throw new BadRequestException("Mã nhà sản xuất đã tồn tại: " + code);
    }
    if (producerRepository.existsByName(request.getName().trim())) {
      throw new BadRequestException("Tên nhà sản xuất đã tồn tại: " + request.getName());
    }

    Producer producer = new Producer();
    mapCreateToEntity(request, producer);
    producer.setCode(code);

    Producer saved = producerRepository.save(producer);
    auditLogService.log(
        "CREATE", "Producer", saved.getId().toString(), "Tạo mới nhà sản xuất: " + saved.getName());
    return mapToResponse(saved);
  }

  @Override
  @Transactional
  public ProducerDto.Response updateProducer(Integer id, ProducerDto.UpdateRequest request) {
    Producer producer = findProducerOrThrow(id);

    if (request.getCode() != null && !normalizeCode(request.getCode()).equals(producer.getCode())) {
      String newCode = normalizeCode(request.getCode());
      if (producerRepository.existsByCode(newCode)) {
        throw new BadRequestException("Mã nhà sản xuất mới đã tồn tại: " + newCode);
      }
      producer.setCode(newCode);
    }

    if (request.getName() != null && !request.getName().trim().equals(producer.getName())) {
      if (producerRepository.existsByName(request.getName().trim())) {
        throw new BadRequestException("Tên nhà sản xuất đã tồn tại: " + request.getName());
      }
      producer.setName(request.getName().trim());
    }

    applyUpdate(request, producer);
    Producer updated = producerRepository.save(producer);

    auditLogService.log(
        "UPDATE", "Producer", updated.getId().toString(), "Cập nhật nhà sản xuất: " + updated.getName());
    return mapToResponse(updated);
  }

  @Override
  @Transactional
  public void deleteProducer(Integer id) {
    Producer producer = findProducerOrThrow(id);
    long productCount = productRepository.countByProducerId(id);

    if (productCount > 0) {
      producer.setIsActive(false);
      producerRepository.save(producer);
      auditLogService.log(
          "SOFT_DELETE",
          "Producer",
          id.toString(),
          "Vô hiệu hóa nhà sản xuất do còn " + productCount + " sản phẩm: " + producer.getName());
    } else {
      producerRepository.delete(producer);
      auditLogService.log(
          "DELETE", "Producer", id.toString(), "Xóa vĩnh viễn nhà sản xuất: " + producer.getName());
    }
  }

  @Override
  @Transactional
  public void hardDeleteProducer(Integer id) {
    Producer producer = findProducerOrThrow(id);
    if (productRepository.countByProducerId(id) > 0) {
      throw new BadRequestException("Không thể xóa cứng thương hiệu đang có sản phẩm liên kết");
    }
    producerRepository.delete(producer);
    auditLogService.log(
        "HARD_DELETE", "Producer", id.toString(), "Xóa cứng nhà sản xuất: " + producer.getName());
  }

  @Override
  @Transactional
  public ProducerDto.Response toggleStatus(Integer id) {
    Producer producer = findProducerOrThrow(id);
    producer.setIsActive(producer.getIsActive() == null || !producer.getIsActive());
    Producer saved = producerRepository.save(producer);

    auditLogService.log(
        "TOGGLE_STATUS",
        "Producer",
        id.toString(),
        "Thay đổi trạng thái: " + (Boolean.TRUE.equals(saved.getIsActive()) ? "ACTIVE" : "INACTIVE"));
    return mapToResponse(saved);
  }

  @Override
  @Transactional
  public List<ProducerDto.Response> bulkUpdateStatus(ProducerDto.BulkStatusRequest request) {
    if (request.getIds() == null || request.getIds().isEmpty()) {
      throw new BadRequestException("Danh sách ID không được trống");
    }
    List<Producer> producers = producerRepository.findAllById(request.getIds());
    if (producers.size() != request.getIds().size()) {
      throw new BadRequestException("Một hoặc nhiều nhà sản xuất không tồn tại");
    }
    producers.forEach(p -> p.setIsActive(request.getIsActive()));
    return producerRepository.saveAll(producers).stream().map(this::mapToResponse).toList();
  }

  @Override
  @Transactional(readOnly = true)
  public ProducerDto.ValidateCodeResponse validateCode(ProducerDto.ValidateCodeRequest request) {
    String code = normalizeCode(request.getCode());
    ProducerDto.ValidateCodeResponse response = new ProducerDto.ValidateCodeResponse();
    response.setCode(code);

    boolean exists = producerRepository.existsByCode(code);
    if (exists && request.getExcludeId() != null) {
      exists =
          producerRepository
              .findByCode(code)
              .map(p -> !p.getId().equals(request.getExcludeId()))
              .orElse(false);
    }

    if (exists) {
      response.setAvailable(false);
      response.setMessage("Mã đã được sử dụng");
    } else {
      response.setAvailable(true);
      response.setMessage("Mã khả dụng");
    }
    return response;
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProducerDto.ProductSummary> getProducerProducts(Integer id, Pageable pageable) {
    findProducerOrThrow(id);
    return productRepository
        .findByProducerId(id, pageable)
        .map(
            p ->
                new ProducerDto.ProductSummary(
                    p.getId(), p.getName(), p.getIsActive(), p.getBasePrice()));
  }

  private Producer findProducerOrThrow(Integer id) {
    return producerRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Producer", "id", id));
  }

  private Producer findProducerByCodeOrThrow(String code) {
    return producerRepository
        .findByCode(code)
        .orElseThrow(() -> new ResourceNotFoundException("Producer", "code", code));
  }

  private String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      throw new BadRequestException("Mã nhà sản xuất không được để trống");
    }
    String normalized = code.trim().toUpperCase();
    if (normalized.length() > 10) {
      throw new BadRequestException("Mã nhà sản xuất tối đa 10 ký tự");
    }
    return normalized;
  }

  private void validateCreateRequest(ProducerDto.Request request) {
    normalizeCode(request.getCode());
    if (request.getName() == null || request.getName().isBlank()) {
      throw new BadRequestException("Tên nhà sản xuất không được để trống");
    }
  }

  private void mapCreateToEntity(ProducerDto.Request request, Producer producer) {
    producer.setName(request.getName().trim());
    producer.setLogoUrl(request.getLogoUrl());
    producer.setDescription(request.getDescription());
    producer.setCountry(request.getCountry());
    producer.setWebsite(request.getWebsite());
    producer.setIsActive(request.getIsActive() == null || request.getIsActive());
  }

  private void applyUpdate(ProducerDto.UpdateRequest request, Producer producer) {
    if (request.getLogoUrl() != null) producer.setLogoUrl(request.getLogoUrl());
    if (request.getDescription() != null) producer.setDescription(request.getDescription());
    if (request.getCountry() != null) producer.setCountry(request.getCountry());
    if (request.getWebsite() != null) producer.setWebsite(request.getWebsite());
    if (request.getIsActive() != null) producer.setIsActive(request.getIsActive());
  }

  private ProducerDto.Response mapToResponse(Producer producer) {
    long productCount = productRepository.countByProducerId(producer.getId());
    long activeProductCount =
        productRepository.countByProducerIdAndIsActive(producer.getId(), true);
    return new ProducerDto.Response(
        producer.getId(),
        producer.getName(),
        producer.getCode(),
        producer.getLogoUrl(),
        producer.getDescription(),
        producer.getCountry(),
        producer.getWebsite(),
        producer.getIsActive(),
        producer.getCreatedAt(),
        productCount,
        activeProductCount);
  }

  private ProducerDto.SlimResponse mapToSlim(Producer producer) {
    return new ProducerDto.SlimResponse(
        producer.getId(),
        producer.getName(),
        producer.getCode(),
        producer.getLogoUrl(),
        producer.getIsActive());
  }
}
