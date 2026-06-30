package com.cdweb.be.service;

import com.cdweb.be.dto.OrderDto;
import com.cdweb.be.dto.OrderManagementDto;
import com.cdweb.be.dto.WarehouseFulfillmentDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseFulfillmentService {

  Page<WarehouseFulfillmentDto.QueueItem> fulfillmentQueue(
      String keyword, String statusFilter, Pageable pageable);

  WarehouseFulfillmentDto.FulfillmentDetail getFulfillmentDetail(Integer orderId);

  WarehouseFulfillmentDto.FulfillmentDetail startPicking(Integer orderId, String username);

  WarehouseFulfillmentDto.FifoSerialsResponse getFifoSerials(Integer variantId, int limit);

  WarehouseFulfillmentDto.ValidateScanResponse validateScan(
      Integer orderId, WarehouseFulfillmentDto.ValidateScanRequest request);

  WarehouseFulfillmentDto.PickingProgress assignSerial(
      Integer orderId, WarehouseFulfillmentDto.AssignSerialRequest request, String username);

  WarehouseFulfillmentDto.PickingProgress getPickingProgress(Integer orderId);

  WarehouseFulfillmentDto.DispatchResponse dispatch(Integer orderId, String username);

  /** Gọi trước khi chuyển SHIPPING (dùng chung từ OrderManagementService). */
  void assertReadyForDispatch(Integer orderId);
}
