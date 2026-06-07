package com.cdweb.be.service;

import com.cdweb.be.dto.ImeiDto;
import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface ImeiService {

  ImeiDto.StatsResponse getStats();

  Page<ImeiDto.ListItem> list(
      String keyword,
      ProductItemStatus status,
      Integer variantId,
      String orderCode,
      LocalDate fromDate,
      LocalDate toDate,
      Pageable pageable);

  ImeiDto.DetailResponse getById(Integer id);

  ImeiDto.DetailResponse lookupByCode(String code);

  void create(ImeiDto.CreateRequest request);

  ImeiDto.ValidateResponse validate(ImeiDto.ValidateRequest request);

  ImeiDto.ImportResult importFromExcel(MultipartFile file);

  ImeiDto.ListItem update(Integer id, ImeiDto.UpdateRequest request);

  ImeiDto.ListItem updateStatus(Integer id, ImeiDto.StatusUpdateRequest request);

  ImeiDto.BulkStatusResult bulkStatus(ImeiDto.BulkStatusRequest request);

  ImeiDto.ReleaseResponse releaseFromOrder(Integer id);

  void returnStock(ImeiDto.ReturnRequest request);

  byte[] exportCsv(
      String keyword,
      ProductItemStatus status,
      Integer variantId,
      String orderCode,
      LocalDate fromDate,
      LocalDate toDate);

  /** Hoàn kho đơn hàng: cộng stock + giải phóng IMEI RESERVED. */
  void restoreOrderInventory(Integer orderId);

  /** Kích hoạt bảo hành khi đơn DELIVERED. */
  void activateWarrantyForOrder(Integer orderId);
}
