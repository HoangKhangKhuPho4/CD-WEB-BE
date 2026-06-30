package com.cdweb.be.service;

import com.cdweb.be.dto.InventoryAuditDto;
import java.util.List;

public interface InventoryAuditService {

  List<InventoryAuditDto.SheetResponse> listSheets();

  List<InventoryAuditDto.SheetResponse> listRecentSheets();

  List<InventoryAuditDto.SheetResponse> listPendingApprovalSheets();

  List<InventoryAuditDto.SheetResponse> listProcessedSheets();

  InventoryAuditDto.StatsResponse getStats();

  InventoryAuditDto.SheetResponse getSheet(Integer id);

  InventoryAuditDto.ScanProgressResponse getScanProgress(Integer id);

  InventoryAuditDto.SheetResponse startSheet(InventoryAuditDto.StartRequest request, String username);

  /** @deprecated dùng start + scan + complete */
  InventoryAuditDto.SheetResponse createSheet(InventoryAuditDto.CreateRequest request, String username);

  InventoryAuditDto.ScanResponse scanCode(Integer id, InventoryAuditDto.ScanRequest request);

  InventoryAuditDto.BulkScanResponse bulkScan(Integer id, InventoryAuditDto.BulkScanRequest request);

  InventoryAuditDto.CompleteResponse completeSheet(Integer id);

  InventoryAuditDto.SheetResponse submitSheet(Integer id, InventoryAuditDto.SubmitRequest request);

  InventoryAuditDto.SheetResponse approveSheet(Integer id, String username);

  InventoryAuditDto.SheetResponse rejectSheet(Integer id, InventoryAuditDto.RejectRequest request);

  InventoryAuditDto.SheetResponse updateNote(Integer id, InventoryAuditDto.UpdateNoteRequest request);

  boolean isProductTypeUnderAudit(Integer productTypeId);

  void assertNoAuditLockForProductType(Integer productTypeId);
}
