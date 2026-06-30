package com.cdweb.be.service;

import com.cdweb.be.dto.PurchaseOrderDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface PurchaseOrderService {

  List<PurchaseOrderDto.SummaryResponse> listForWarehouse(String statusFilter);

  Page<PurchaseOrderDto.SummaryResponse> listForWarehousePaged(
      String statusFilter, Pageable pageable);

  /** PO chờ quét Serial (APPROVED + RECEIVING). */
  List<PurchaseOrderDto.SummaryResponse> listImeiQueue();

  List<PurchaseOrderDto.SummaryResponse> listForProcurement(String statusFilter);

  List<PurchaseOrderDto.SummaryResponse> listForApproval();

  List<PurchaseOrderDto.SupplierResponse> listSuppliers();

  PurchaseOrderDto.DetailResponse getDetail(Integer id);

  PurchaseOrderDto.DetailResponse getDetailUnrestricted(Integer id);

  PurchaseOrderDto.DetailResponse create(PurchaseOrderDto.CreateRequest request, String username);

  PurchaseOrderDto.SummaryResponse approve(Integer id, String username);

  PurchaseOrderDto.SummaryResponse reject(
      Integer id, PurchaseOrderDto.RejectRequest request, String username);

  PurchaseOrderDto.SummaryResponse startReceiving(Integer id);

  PurchaseOrderDto.ReceiveDetailResponse getReceiveDetail(Integer id);

  PurchaseOrderDto.ValidateScanResponse validateReceiveScan(
      Integer id, PurchaseOrderDto.ValidateScanRequest request);

  PurchaseOrderDto.ReceiveDetailResponse receiveSerial(
      Integer id, PurchaseOrderDto.ReceiveSerialRequest request, String username);

  PurchaseOrderDto.BulkReceiveSerialResponse receiveSerialBulk(
      Integer id, PurchaseOrderDto.BulkReceiveSerialRequest request, String username);

  PurchaseOrderDto.ReceiveDetailResponse receiveQuantity(
      Integer id, PurchaseOrderDto.ReceiveQuantityRequest request, String username);

  PurchaseOrderDto.ReceiveDetailResponse reportDamaged(
      Integer id, PurchaseOrderDto.ReportDamagedRequest request, String username);

  PurchaseOrderDto.CompleteReceivingResponse completeReceiving(
      Integer id, PurchaseOrderDto.CompleteReceivingRequest request, String username);

  long countPendingForWarehouse();
}
