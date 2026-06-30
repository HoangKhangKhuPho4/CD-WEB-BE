package com.cdweb.be.service.impl;

import com.cdweb.be.dto.PurchaseOrderDto;
import com.cdweb.be.entity.Inventory;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.entity.ProductItem.ProductItemCondition;
import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.entity.PurchaseOrder;
import com.cdweb.be.entity.PurchaseOrder.PurchaseOrderStatus;
import com.cdweb.be.entity.PurchaseOrderItem;
import com.cdweb.be.entity.Supplier;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.repository.InventoryRepository;
import com.cdweb.be.repository.ProductItemRepository;
import com.cdweb.be.repository.ProductVariantRepository;
import com.cdweb.be.entity.PurchaseOrderReceiveIssue;
import com.cdweb.be.entity.PurchaseOrderReceiveIssue.IssueType;
import com.cdweb.be.repository.PurchaseOrderReceiveIssueRepository;
import com.cdweb.be.repository.PurchaseOrderRepository;
import com.cdweb.be.repository.SupplierRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.service.PurchaseOrderService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PurchaseOrderServiceImpl implements PurchaseOrderService {

  private static final DateTimeFormatter VI_DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.forLanguageTag("vi-VN"));
  private static final DateTimeFormatter ISO_DATE = DateTimeFormatter.ISO_LOCAL_DATE;

  private static final Set<PurchaseOrderStatus> WAREHOUSE_VISIBLE =
      EnumSet.of(
          PurchaseOrderStatus.APPROVED,
          PurchaseOrderStatus.RECEIVING,
          PurchaseOrderStatus.COMPLETED);

  private static final Set<PurchaseOrderStatus> PENDING_WAREHOUSE =
      EnumSet.of(PurchaseOrderStatus.APPROVED, PurchaseOrderStatus.RECEIVING);

  private static final Set<PurchaseOrderStatus> RECEIVE_VISIBLE =
      EnumSet.of(
          PurchaseOrderStatus.APPROVED,
          PurchaseOrderStatus.RECEIVING,
          PurchaseOrderStatus.COMPLETED);

  private static final Pattern SCAN_CODE_PATTERN = Pattern.compile("^[A-Za-z0-9]{8,20}$");

  private static final String REF_TYPE_PURCHASE_ORDER = "PURCHASE_ORDER";

  private final PurchaseOrderRepository purchaseOrderRepository;
  private final SupplierRepository supplierRepository;
  private final ProductVariantRepository productVariantRepository;
  private final ProductItemRepository productItemRepository;
  private final InventoryRepository inventoryRepository;
  private final PurchaseOrderReceiveIssueRepository receiveIssueRepository;
  private final UserRepository userRepository;

  @Override
  public List<PurchaseOrderDto.SummaryResponse> listImeiQueue() {
    return purchaseOrderRepository.findByStatusInOrderByExpectedDate(PENDING_WAREHOUSE).stream()
        .map(this::toSummary)
        .toList();
  }

  @Override
  public List<PurchaseOrderDto.SummaryResponse> listForWarehouse(String statusFilter) {
    List<PurchaseOrder> orders =
        purchaseOrderRepository.findByStatusInOrderByExpectedDate(WAREHOUSE_VISIBLE);

    if (statusFilter != null && !statusFilter.isBlank()) {
      String normalized = statusFilter.trim().toLowerCase(Locale.ROOT);
      orders =
          orders.stream()
              .filter(po -> toFeStatus(po.getStatus()).equals(normalized))
              .toList();
    }

    return orders.stream().map(this::toSummary).toList();
  }

  @Override
  public Page<PurchaseOrderDto.SummaryResponse> listForWarehousePaged(
      String statusFilter, Pageable pageable) {
    return purchaseOrderRepository
        .findByStatusIn(resolveWarehouseStatuses(statusFilter), pageable)
        .map(this::toSummary);
  }

  private Collection<PurchaseOrderStatus> resolveWarehouseStatuses(String statusFilter) {
    if (statusFilter == null || statusFilter.isBlank()) {
      return WAREHOUSE_VISIBLE;
    }
    return switch (statusFilter.trim().toLowerCase(Locale.ROOT)) {
      case "pending" -> List.of(PurchaseOrderStatus.APPROVED);
      case "receiving" -> List.of(PurchaseOrderStatus.RECEIVING);
      case "completed" -> List.of(PurchaseOrderStatus.COMPLETED);
      default -> WAREHOUSE_VISIBLE;
    };
  }

  @Override
  public List<PurchaseOrderDto.SummaryResponse> listForProcurement(String statusFilter) {
    Set<PurchaseOrderStatus> statuses =
        EnumSet.of(
            PurchaseOrderStatus.DRAFT,
            PurchaseOrderStatus.PENDING,
            PurchaseOrderStatus.APPROVED,
            PurchaseOrderStatus.CANCELLED);
    List<PurchaseOrder> orders =
        purchaseOrderRepository.findByStatusInOrderByExpectedDate(statuses);
    if (statusFilter != null && !statusFilter.isBlank()) {
      String raw = statusFilter.trim().toUpperCase(Locale.ROOT);
      orders =
          orders.stream().filter(po -> po.getStatus().name().equals(raw)).toList();
    }
    return orders.stream().map(this::toSummary).toList();
  }

  @Override
  public List<PurchaseOrderDto.SummaryResponse> listForApproval() {
    return purchaseOrderRepository
        .findByStatusInOrderByExpectedDate(EnumSet.of(PurchaseOrderStatus.PENDING))
        .stream()
        .map(this::toSummary)
        .toList();
  }

  @Override
  public List<PurchaseOrderDto.SupplierResponse> listSuppliers() {
    return supplierRepository.findAll().stream()
        .filter(s -> s.getIsActive() == null || Boolean.TRUE.equals(s.getIsActive()))
        .map(
            s ->
                PurchaseOrderDto.SupplierResponse.builder()
                    .id(s.getId())
                    .name(s.getName())
                    .code(s.getCode())
                    .phone(s.getPhone())
                    .email(s.getEmail())
                    .build())
        .toList();
  }

  @Override
  public PurchaseOrderDto.DetailResponse getDetail(Integer id) {
    PurchaseOrder po = findPo(id);
    if (!WAREHOUSE_VISIBLE.contains(po.getStatus())) {
      throw new BadRequestException("Đơn mua hàng chưa được duyệt hoặc không thuộc hàng đợi kho");
    }
    return toDetail(po);
  }

  @Override
  public PurchaseOrderDto.DetailResponse getDetailUnrestricted(Integer id) {
    return toDetail(findPo(id));
  }

  @Override
  @Transactional
  public PurchaseOrderDto.DetailResponse create(
      PurchaseOrderDto.CreateRequest request, String username) {
    if (request.getSupplierId() == null) {
      throw new BadRequestException("Vui lòng chọn nhà cung cấp");
    }
    if (request.getLines() == null || request.getLines().isEmpty()) {
      throw new BadRequestException("Đơn mua hàng cần ít nhất một dòng sản phẩm");
    }

    Supplier supplier =
        supplierRepository
            .findById(request.getSupplierId())
            .orElseThrow(() -> new BadRequestException("Nhà cung cấp không tồn tại"));

    User creator =
        userRepository
            .findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new BadRequestException("Không tìm thấy user: " + username));

    boolean submit = Boolean.TRUE.equals(request.getSubmitForApproval());
    PurchaseOrder po = new PurchaseOrder();
    po.setPoNumber(generatePoNumber());
    po.setSupplier(supplier);
    po.setCreatedByUser(creator);
    po.setNotes(request.getNotes());
    po.setStatus(submit ? PurchaseOrderStatus.PENDING : PurchaseOrderStatus.DRAFT);
    po.setExpectedDate(parseExpectedDate(request.getExpectedDate()));

    List<PurchaseOrderItem> items = new ArrayList<>();
    BigDecimal total = BigDecimal.ZERO;
    for (PurchaseOrderDto.CreateLineRequest line : request.getLines()) {
      if (line.getVariantId() == null || line.getQuantityOrdered() == null || line.getQuantityOrdered() <= 0) {
        throw new BadRequestException("Mỗi dòng cần variant và số lượng > 0");
      }
      ProductVariant variant =
          productVariantRepository
              .findById(line.getVariantId())
              .orElseThrow(
                  () -> new BadRequestException("Biến thể không tồn tại: #" + line.getVariantId()));
      BigDecimal unitCost =
          line.getUnitCost() != null && line.getUnitCost().signum() > 0
              ? line.getUnitCost()
              : (variant.getPrice() != null ? variant.getPrice() : BigDecimal.ZERO);
      BigDecimal lineTotal = unitCost.multiply(BigDecimal.valueOf(line.getQuantityOrdered()));

      PurchaseOrderItem item = new PurchaseOrderItem();
      item.setPurchaseOrder(po);
      item.setVariant(variant);
      item.setQuantityOrdered(line.getQuantityOrdered());
      item.setQuantityReceived(0);
      item.setUnitCost(unitCost);
      item.setTotalCost(lineTotal);
      items.add(item);
      total = total.add(lineTotal);
    }

    po.setItems(items);
    po.setTotalAmount(total);
    purchaseOrderRepository.save(po);
    return toDetail(po);
  }

  @Override
  @Transactional
  public PurchaseOrderDto.SummaryResponse approve(Integer id, String username) {
    PurchaseOrder po = findPo(id);
    if (po.getStatus() != PurchaseOrderStatus.PENDING) {
      throw new BadRequestException("Chỉ duyệt được PO ở trạng thái PENDING");
    }
    po.setStatus(PurchaseOrderStatus.APPROVED);
    purchaseOrderRepository.save(po);
    return toSummary(po);
  }

  @Override
  @Transactional
  public PurchaseOrderDto.SummaryResponse reject(
      Integer id, PurchaseOrderDto.RejectRequest request, String username) {
    if (request == null
        || request.getRejectReason() == null
        || request.getRejectReason().isBlank()) {
      throw new BadRequestException("Vui lòng nhập lý do từ chối");
    }
    PurchaseOrder po = findPo(id);
    if (po.getStatus() != PurchaseOrderStatus.PENDING) {
      throw new BadRequestException("Chỉ từ chối được PO ở trạng thái PENDING");
    }
    po.setStatus(PurchaseOrderStatus.CANCELLED);
    po.setRejectReason(request.getRejectReason().trim());
    purchaseOrderRepository.save(po);
    return toSummary(po);
  }

  @Override
  @Transactional
  public PurchaseOrderDto.SummaryResponse startReceiving(Integer id) {
    PurchaseOrder po = findPo(id);

    if (po.getStatus() == PurchaseOrderStatus.COMPLETED
        || po.getStatus() == PurchaseOrderStatus.CANCELLED) {
      throw new BadRequestException("Không thể nhận hàng cho PO ở trạng thái " + po.getStatus());
    }
    if (po.getStatus() == PurchaseOrderStatus.RECEIVING) {
      return toSummary(po);
    }
    if (po.getStatus() != PurchaseOrderStatus.APPROVED) {
      throw new BadRequestException("Chỉ PO đã duyệt (APPROVED) mới có thể bắt đầu kiểm đếm nhập kho");
    }

    po.setStatus(PurchaseOrderStatus.RECEIVING);
    purchaseOrderRepository.save(po);
    return toSummary(po);
  }

  @Override
  @Transactional(readOnly = true)
  public PurchaseOrderDto.ReceiveDetailResponse getReceiveDetail(Integer id) {
    PurchaseOrder po = findDetailedPo(id);
    if (!RECEIVE_VISIBLE.contains(po.getStatus())) {
      throw new BadRequestException("PO chưa sẵn sàng cho kiểm đếm nhập kho");
    }
    return buildReceiveDetail(po);
  }

  @Override
  @Transactional(readOnly = true)
  public PurchaseOrderDto.ValidateScanResponse validateReceiveScan(
      Integer id, PurchaseOrderDto.ValidateScanRequest request) {
    PurchaseOrder po = findDetailedPo(id);
    assertReceiving(po);
    PurchaseOrderItem line = resolvePoLine(po, request.getPoLineId());
    String code = normalizeScanCode(request.getScannedCode());
    return validateInboundScan(line, code);
  }

  @Override
  @Transactional
  public PurchaseOrderDto.ReceiveDetailResponse receiveSerial(
      Integer id, PurchaseOrderDto.ReceiveSerialRequest request, String username) {
    PurchaseOrder po = findDetailedPo(id);
    assertReceiving(po);
    PurchaseOrderItem line = resolvePoLine(po, request.getPoLineId());
    User user = loadUser(username);
    return receiveSerialOnLine(po, line, request, user);
  }

  @Override
  @Transactional
  public PurchaseOrderDto.BulkReceiveSerialResponse receiveSerialBulk(
      Integer id, PurchaseOrderDto.BulkReceiveSerialRequest request, String username) {
    if (request == null || request.getPoLineId() == null) {
      throw new BadRequestException("Thiếu mã dòng PO");
    }
    if (request.getSerials() == null || request.getSerials().isEmpty()) {
      throw new BadRequestException("Danh sách Serial trống");
    }

    PurchaseOrder po = findDetailedPo(id);
    assertReceiving(po);
    PurchaseOrderItem line = resolvePoLine(po, request.getPoLineId());
    User user = loadUser(username);

    List<String> normalized = dedupeSerials(request.getSerials());
    int remaining = remainingQty(line);
    if (normalized.size() > remaining) {
      throw new BadRequestException(
          "Vượt quá SL còn lại trên dòng PO (" + remaining + " — gửi " + normalized.size() + ")");
    }

    List<PurchaseOrderDto.BulkReceiveItemResult> results = new ArrayList<>();
    int successCount = 0;
    int failCount = 0;

    for (String code : normalized) {
      try {
        PurchaseOrderDto.ReceiveSerialRequest single =
            PurchaseOrderDto.ReceiveSerialRequest.builder()
                .poLineId(request.getPoLineId())
                .scannedCode(code)
                .batchNumber(request.getBatchNumber())
                .shelfLocation(request.getShelfLocation())
                .build();
        po = findDetailedPo(id);
        line = resolvePoLine(po, request.getPoLineId());
        receiveSerialOnLine(po, line, single, user);
        results.add(
            PurchaseOrderDto.BulkReceiveItemResult.builder()
                .serial(code)
                .success(true)
                .message("Đã nhập kho")
                .build());
        successCount++;
      } catch (Exception ex) {
        String msg = ex.getMessage() != null ? ex.getMessage() : "Lỗi nhập serial";
        results.add(
            PurchaseOrderDto.BulkReceiveItemResult.builder()
                .serial(code)
                .success(false)
                .message(msg)
                .build());
        failCount++;
      }
    }

    po = findDetailedPo(id);
    boolean autoCompleted = tryAutoCompleteReceiving(po);
    if (autoCompleted) {
      po = findDetailedPo(id);
    }
    PurchaseOrderDto.ReceiveDetailResponse detail = buildReceiveDetail(po);
    detail.setAutoCompleted(autoCompleted);

    String message =
        autoCompleted
            ? "Đã nhập "
                + successCount
                + " serial — PO tự động hoàn tất 100%"
            : "Đã nhập thành công "
                + successCount
                + (failCount > 0 ? ", lỗi " + failCount : "");

    return PurchaseOrderDto.BulkReceiveSerialResponse.builder()
        .detail(detail)
        .results(results)
        .successCount(successCount)
        .failCount(failCount)
        .autoCompleted(autoCompleted)
        .message(message)
        .build();
  }

  private PurchaseOrderDto.ReceiveDetailResponse receiveSerialOnLine(
      PurchaseOrder po,
      PurchaseOrderItem line,
      PurchaseOrderDto.ReceiveSerialRequest request,
      User user) {
    String code = normalizeScanCode(request.getScannedCode());
    PurchaseOrderDto.ValidateScanResponse validation = validateInboundScan(line, code);
    if (!validation.isValid()) {
      throw new BadRequestException(validation.getMessage());
    }

    ProductVariant variant = line.getVariant();
    String batch = resolveBatchNumber(po, request.getBatchNumber());
    String location = normalizeLocation(request.getShelfLocation());

    ProductItem item = new ProductItem();
    item.setImei(code);
    item.setSerialNumber(code);
    item.setVariant(variant);
    item.setPurchaseOrder(po);
    item.setBatchNumber(batch);
    item.setLocation(location);
    item.setManufactureDate(LocalDate.now());
    item.setStatus(ProductItemStatus.AVAILABLE);
    item.setCondition(ProductItemCondition.NEW);
    if (request.getImei2() != null && !request.getImei2().isBlank()) {
      item.setImei2(request.getImei2().trim());
    }
    if (request.getMacAddress() != null && !request.getMacAddress().isBlank()) {
      item.setMacAddress(request.getMacAddress().trim());
    }
    productItemRepository.save(item);

    incrementLineReceived(line, 1);
    variant.setStockQuantity(variant.getStockQuantity() + 1);
    productVariantRepository.save(variant);

    Inventory txn = new Inventory();
    txn.setTransactionType(Inventory.TransactionType.IMPORT);
    txn.setQuantity(1);
    txn.setVariant(variant);
    txn.setProductItem(item);
    txn.setReferenceType(REF_TYPE_PURCHASE_ORDER);
    txn.setReferenceId(po.getId());
    txn.setReason(
        "Nhập kho PO "
            + po.getPoNumber()
            + " · LOT "
            + batch
            + (location != null ? " · Kệ " + location : ""));
    txn.setUser(user);
    inventoryRepository.save(txn);

    purchaseOrderRepository.save(po);

    boolean autoCompleted = tryAutoCompleteReceiving(po);
    PurchaseOrder refreshed = autoCompleted ? findDetailedPo(po.getId()) : po;
    PurchaseOrderDto.ReceiveDetailResponse detail = buildReceiveDetail(refreshed);
    detail.setAutoCompleted(autoCompleted);
    return detail;
  }

  private List<String> dedupeSerials(List<String> raw) {
    LinkedHashSet<String> set = new LinkedHashSet<>();
    for (String s : raw) {
      if (s == null) continue;
      String t = s.trim();
      if (!t.isEmpty()) set.add(t);
    }
    return new ArrayList<>(set);
  }

  /** Tự đóng PO khi đã quét đủ 100% (không thiếu). */
  private boolean tryAutoCompleteReceiving(PurchaseOrder po) {
    if (po.getStatus() != PurchaseOrderStatus.RECEIVING) {
      return false;
    }
    int totalOrdered = 0;
    int totalReceived = 0;
    int totalDamaged = 0;
    if (po.getItems() != null) {
      for (PurchaseOrderItem line : po.getItems()) {
        totalOrdered += line.getQuantityOrdered() != null ? line.getQuantityOrdered() : 0;
        totalReceived += line.getQuantityReceived() != null ? line.getQuantityReceived() : 0;
        totalDamaged += line.getQuantityDamaged() != null ? line.getQuantityDamaged() : 0;
      }
    }
    int totalRemaining = Math.max(0, totalOrdered - totalReceived - totalDamaged);
    if (totalOrdered <= 0 || totalRemaining > 0) {
      return false;
    }
    po.setStatus(PurchaseOrderStatus.COMPLETED);
    po.setReceivedDate(LocalDateTime.now());
    purchaseOrderRepository.save(po);
    return true;
  }

  private List<PurchaseOrderDto.StockLotSummary> buildStockLots(PurchaseOrder po, int totalRemaining) {
    String activeLot = resolveActiveBatchNumber(po, totalRemaining);
    List<PurchaseOrderDto.StockLotSummary> lots = new ArrayList<>();

    List<Object[]> grouped =
        productItemRepository.countGroupedByBatchForPurchaseOrder(po.getId());
    for (Object[] row : grouped) {
      String batch = (String) row[0];
      int scanned = ((Number) row[1]).intValue();
      boolean isActive = batch.equals(activeLot) && totalRemaining > 0;
      lots.add(
          PurchaseOrderDto.StockLotSummary.builder()
              .lotNumber(batch)
              .itemsScanned(scanned)
              .itemsRequired(isActive ? scanned + totalRemaining : scanned)
              .status(isActive ? "OPEN" : "CLOSED")
              .build());
    }

    boolean hasActive = lots.stream().anyMatch(l -> "OPEN".equals(l.getStatus()));
    if (!hasActive && totalRemaining > 0) {
      long activeScanned =
          productItemRepository.countByPurchaseOrder_IdAndBatchNumber(po.getId(), activeLot);
      lots.add(
          PurchaseOrderDto.StockLotSummary.builder()
              .lotNumber(activeLot)
              .itemsScanned((int) activeScanned)
              .itemsRequired((int) activeScanned + totalRemaining)
              .status("OPEN")
              .build());
    }
    return lots;
  }

  @Override
  @Transactional
  public PurchaseOrderDto.ReceiveDetailResponse receiveQuantity(
      Integer id, PurchaseOrderDto.ReceiveQuantityRequest request, String username) {
    PurchaseOrder po = findDetailedPo(id);
    assertReceiving(po);
    PurchaseOrderItem line = resolvePoLine(po, request.getPoLineId());
    int qty = request.getQuantity() != null ? request.getQuantity() : 0;
    if (qty <= 0) {
      throw new BadRequestException("Số lượng nhập phải lớn hơn 0");
    }
    int remaining = remainingQty(line);
    if (qty > remaining) {
      throw new BadRequestException("Vượt quá SL còn thiếu trên dòng PO (" + remaining + ")");
    }

    User user = loadUser(username);
    ProductVariant variant = line.getVariant();
    String batch = resolveBatchNumber(po, request.getBatchNumber());
    String location = normalizeLocation(request.getShelfLocation());

    incrementLineReceived(line, qty);
    variant.setStockQuantity(variant.getStockQuantity() + qty);
    productVariantRepository.save(variant);

    Inventory txn = new Inventory();
    txn.setTransactionType(Inventory.TransactionType.IMPORT);
    txn.setQuantity(qty);
    txn.setVariant(variant);
    txn.setReferenceType(REF_TYPE_PURCHASE_ORDER);
    txn.setReferenceId(po.getId());
    txn.setReason(
        request.getNote() != null && !request.getNote().isBlank()
            ? request.getNote().trim() + (location != null ? " · Kệ " + location : "")
            : "Nhập kho PO " + po.getPoNumber() + " · LOT " + batch
                + (location != null ? " · Kệ " + location : "")
                + " (SL)");
    txn.setUser(user);
    inventoryRepository.save(txn);

    purchaseOrderRepository.save(po);
    return buildReceiveDetail(po);
  }

  @Override
  @Transactional
  public PurchaseOrderDto.ReceiveDetailResponse reportDamaged(
      Integer id, PurchaseOrderDto.ReportDamagedRequest request, String username) {
    if (request == null
        || request.getReason() == null
        || request.getReason().isBlank()) {
      throw new BadRequestException("Vui lòng nhập lý do báo cáo hàng lỗi/sai lệch");
    }
    PurchaseOrder po = findDetailedPo(id);
    assertReceiving(po);
    PurchaseOrderItem line = resolvePoLine(po, request.getPoLineId());
    int qty = request.getQuantity() != null && request.getQuantity() > 0 ? request.getQuantity() : 1;
    if (remainingQty(line) < qty) {
      throw new BadRequestException("Vượt quá SL còn lại trên dòng PO");
    }

    User user = loadUser(username);
    String batch = resolveBatchNumber(po, request.getBatchNumber());
    String location = normalizeLocation(request.getShelfLocation());
    String serial =
        request.getSerialCode() != null && !request.getSerialCode().isBlank()
            ? request.getSerialCode().trim()
            : null;

    if (serial != null) {
      if (!SCAN_CODE_PATTERN.matcher(serial).matches()) {
        throw new BadRequestException("Mã serial không hợp lệ");
      }
      if (productItemRepository.findByImeiOrSerialNumber(serial, serial).isPresent()) {
        throw new BadRequestException("Mã serial đã tồn tại trong hệ thống");
      }
      ProductVariant variant = line.getVariant();
      ProductItem item = new ProductItem();
      item.setImei(serial);
      item.setSerialNumber(serial);
      item.setVariant(variant);
      item.setPurchaseOrder(po);
      item.setBatchNumber(batch);
      item.setLocation(location);
      item.setManufactureDate(LocalDate.now());
      item.setStatus(ProductItemStatus.DEFECTIVE);
      item.setCondition(ProductItemCondition.DAMAGED);
      item.setNotes("Hàng lỗi PO " + po.getPoNumber() + ": " + request.getReason().trim());
      productItemRepository.save(item);
      qty = 1;
    }

    incrementLineDamaged(line, qty);
    saveReceiveIssue(
        po,
        line,
        user,
        IssueType.DAMAGED,
        serial,
        qty,
        request.getReason().trim(),
        request.getEvidenceUrl());

    purchaseOrderRepository.save(po);
    return buildReceiveDetail(po);
  }

  @Override
  @Transactional
  public PurchaseOrderDto.CompleteReceivingResponse completeReceiving(
      Integer id, PurchaseOrderDto.CompleteReceivingRequest request, String username) {
    PurchaseOrder po = findDetailedPo(id);
    assertReceiving(po);

    int totalOrdered = 0;
    int totalReceived = 0;
    int totalDamaged = 0;
    if (po.getItems() != null) {
      for (PurchaseOrderItem line : po.getItems()) {
        totalOrdered += line.getQuantityOrdered() != null ? line.getQuantityOrdered() : 0;
        totalReceived += line.getQuantityReceived() != null ? line.getQuantityReceived() : 0;
        totalDamaged += line.getQuantityDamaged() != null ? line.getQuantityDamaged() : 0;
      }
    }
    int totalAccounted = totalReceived + totalDamaged;
    int totalMissing = Math.max(0, totalOrdered - totalAccounted);

    if (totalMissing > 0) {
      String note =
          request != null && request.getDiscrepancyNote() != null
              ? request.getDiscrepancyNote().trim()
              : "";
      if (note.isEmpty()) {
        throw new BadRequestException(
            "Còn thiếu "
                + totalMissing
                + " sản phẩm — bắt buộc nhập lý do sai lệch trước khi khóa đơn");
      }
      User user = loadUser(username);
      saveReceiveIssue(
          po,
          null,
          user,
          IssueType.DISCREPANCY,
          null,
          totalMissing,
          note,
          null);
      String existingNotes = po.getNotes() != null ? po.getNotes() : "";
      po.setNotes(
          (existingNotes.isBlank() ? "" : existingNotes + "\n")
              + "[Khóa đơn] Sai lệch: "
              + note);
    } else if (totalAccounted == 0) {
      throw new BadRequestException("Chưa ghi nhận hàng nào — không thể khóa đơn");
    }

    po.setStatus(PurchaseOrderStatus.COMPLETED);
    po.setReceivedDate(LocalDateTime.now());
    purchaseOrderRepository.save(po);

    String message =
        totalMissing > 0
            ? "Đã khóa PO với sai lệch — nhập "
                + totalReceived
                + ", lỗi "
                + totalDamaged
                + ", thiếu "
                + totalMissing
            : "Đã hoàn tất & khóa PO — nhập "
                + totalReceived
                + " nguyên vẹn"
                + (totalDamaged > 0 ? ", lỗi " + totalDamaged : "");

    return PurchaseOrderDto.CompleteReceivingResponse.builder()
        .id(po.getId())
        .code(po.getPoNumber())
        .status(toFeStatus(po.getStatus()))
        .totalOrdered(totalOrdered)
        .totalReceived(totalReceived)
        .totalDamaged(totalDamaged)
        .totalMissing(totalMissing)
        .message(message)
        .build();
  }

  @Override
  public long countPendingForWarehouse() {
    return purchaseOrderRepository.countByStatusIn(PENDING_WAREHOUSE);
  }

  private PurchaseOrder findPo(Integer id) {
    return purchaseOrderRepository
        .findById(id)
        .orElseThrow(() -> new BadRequestException("Đơn mua hàng không tồn tại: #" + id));
  }

  private PurchaseOrder findDetailedPo(Integer id) {
    return purchaseOrderRepository
        .findDetailedById(id)
        .orElseThrow(() -> new BadRequestException("Đơn mua hàng không tồn tại: #" + id));
  }

  private User loadUser(String username) {
    return userRepository
        .findByUsernameOrEmail(username, username)
        .orElseThrow(() -> new BadRequestException("Không tìm thấy user: " + username));
  }

  private void assertReceiving(PurchaseOrder po) {
    if (po.getStatus() != PurchaseOrderStatus.RECEIVING) {
      throw new BadRequestException("PO phải ở trạng thái RECEIVING để quét nhập kho");
    }
  }

  private PurchaseOrderItem resolvePoLine(PurchaseOrder po, Integer poLineId) {
    if (poLineId == null) {
      throw new BadRequestException("Thiếu mã dòng PO");
    }
    if (po.getItems() == null) {
      throw new BadRequestException("PO không có dòng hàng");
    }
    return po.getItems().stream()
        .filter(i -> poLineId.equals(i.getId()))
        .findFirst()
        .orElseThrow(() -> new BadRequestException("Dòng PO không tồn tại: #" + poLineId));
  }

  private String normalizeScanCode(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new BadRequestException("Mã serial/IMEI không được để trống");
    }
    return raw.trim();
  }

  private PurchaseOrderDto.ValidateScanResponse validateInboundScan(
      PurchaseOrderItem line, String code) {
    if (!SCAN_CODE_PATTERN.matcher(code).matches()) {
      return PurchaseOrderDto.ValidateScanResponse.builder()
          .valid(false)
          .message("Mã không hợp lệ (8–20 ký tự chữ/số)")
          .scannedCode(code)
          .build();
    }
    if (productItemRepository.findByImeiOrSerialNumber(code, code).isPresent()) {
      return PurchaseOrderDto.ValidateScanResponse.builder()
          .valid(false)
          .message("Mã đã tồn tại trong kho — không thể nhập trùng")
          .scannedCode(code)
          .build();
    }
    if (remainingQty(line) <= 0) {
      return PurchaseOrderDto.ValidateScanResponse.builder()
          .valid(false)
          .message("Dòng PO đã nhập đủ số lượng")
          .scannedCode(code)
          .build();
    }
    return PurchaseOrderDto.ValidateScanResponse.builder()
        .valid(true)
        .message("Hợp lệ — sẵn sàng nhập kho")
        .scannedCode(code)
        .build();
  }

  private int remainingQty(PurchaseOrderItem line) {
    int ordered = line.getQuantityOrdered() != null ? line.getQuantityOrdered() : 0;
    int received = line.getQuantityReceived() != null ? line.getQuantityReceived() : 0;
    int damaged = line.getQuantityDamaged() != null ? line.getQuantityDamaged() : 0;
    return Math.max(0, ordered - received - damaged);
  }

  private void incrementLineReceived(PurchaseOrderItem line, int delta) {
    int current = line.getQuantityReceived() != null ? line.getQuantityReceived() : 0;
    line.setQuantityReceived(current + delta);
  }

  private void incrementLineDamaged(PurchaseOrderItem line, int delta) {
    int current = line.getQuantityDamaged() != null ? line.getQuantityDamaged() : 0;
    line.setQuantityDamaged(current + delta);
  }

  private String resolveBatchNumber(PurchaseOrder po, String requested) {
    if (requested != null && !requested.isBlank()) {
      return requested.trim();
    }
    return resolveActiveBatchNumber(po, computeTotalRemaining(po));
  }

  private int computeTotalRemaining(PurchaseOrder po) {
    if (po.getItems() == null || po.getItems().isEmpty()) {
      return 0;
    }
    int totalOrdered = 0;
    int totalReceived = 0;
    int totalDamaged = 0;
    for (PurchaseOrderItem line : po.getItems()) {
      totalOrdered += line.getQuantityOrdered() != null ? line.getQuantityOrdered() : 0;
      totalReceived += line.getQuantityReceived() != null ? line.getQuantityReceived() : 0;
      totalDamaged += line.getQuantityDamaged() != null ? line.getQuantityDamaged() : 0;
    }
    return Math.max(0, totalOrdered - totalReceived - totalDamaged);
  }

  /** Lô đang nhận: tiếp tục WAVE hiện tại hoặc mở WAVE mới. */
  private String resolveActiveBatchNumber(PurchaseOrder po, int totalRemaining) {
    List<Object[]> grouped =
        productItemRepository.countGroupedByBatchForPurchaseOrder(po.getId());
    if (!grouped.isEmpty()) {
      String lastBatch = (String) grouped.get(grouped.size() - 1)[0];
      if (totalRemaining > 0) {
        return lastBatch;
      }
      return lastBatch;
    }
    if (totalRemaining > 0) {
      return generateDefaultBatchNumber(po);
    }
    return generateDefaultBatchNumber(po);
  }

  private String generateDefaultBatchNumber(PurchaseOrder po) {
    String slug = po.getPoNumber().replace("-", "").replace(" ", "");
    long wave = productItemRepository.countDistinctBatchNumbersByPurchaseOrderId(po.getId()) + 1;
    return "LOT-" + slug + "-WAVE" + wave;
  }

  private String normalizeLocation(String raw) {
    if (raw == null || raw.isBlank()) {
      return null;
    }
    return raw.trim();
  }

  private void saveReceiveIssue(
      PurchaseOrder po,
      PurchaseOrderItem line,
      User user,
      IssueType type,
      String serial,
      int qty,
      String reason,
      String evidenceUrl) {
    PurchaseOrderReceiveIssue issue = new PurchaseOrderReceiveIssue();
    issue.setPurchaseOrder(po);
    issue.setPoLine(line);
    issue.setCreatedBy(user);
    issue.setIssueType(type);
    issue.setSerialCode(serial);
    issue.setQuantity(qty);
    issue.setReason(reason);
    issue.setEvidenceUrl(evidenceUrl);
    receiveIssueRepository.save(issue);
  }

  private PurchaseOrderDto.ReceiveDetailResponse buildReceiveDetail(PurchaseOrder po) {
    List<PurchaseOrderDto.ReceiveLineProgress> lines = new ArrayList<>();
    int totalOrdered = 0;
    int totalReceived = 0;
    int totalDamaged = 0;

    if (po.getItems() != null) {
      for (PurchaseOrderItem item : po.getItems()) {
        int ordered = item.getQuantityOrdered() != null ? item.getQuantityOrdered() : 0;
        int received = item.getQuantityReceived() != null ? item.getQuantityReceived() : 0;
        int damaged = item.getQuantityDamaged() != null ? item.getQuantityDamaged() : 0;
        totalOrdered += ordered;
        totalReceived += received;
        totalDamaged += damaged;

        List<ProductItem> lineItems =
            productItemRepository.findByPurchaseOrder_IdAndVariant_IdOrderByCreatedAtAsc(
                po.getId(), item.getVariant().getId());

        List<String> serials =
            lineItems.stream()
                .filter(pi -> pi.getStatus() == ProductItemStatus.AVAILABLE)
                .map(
                    pi ->
                        pi.getImei() != null && !pi.getImei().isBlank()
                            ? pi.getImei()
                            : pi.getSerialNumber())
                .filter(s -> s != null && !s.isBlank())
                .toList();

        List<String> damagedSerials =
            lineItems.stream()
                .filter(pi -> pi.getStatus() == ProductItemStatus.DEFECTIVE)
                .map(
                    pi ->
                        pi.getImei() != null && !pi.getImei().isBlank()
                            ? pi.getImei()
                            : pi.getSerialNumber())
                .filter(s -> s != null && !s.isBlank())
                .toList();

        String productName = null;
        String variantName = null;
        String sku = null;
        if (item.getVariant() != null) {
          sku = item.getVariant().getSkuCode();
          variantName = item.getVariant().getVariantName();
          if (item.getVariant().getProduct() != null) {
            productName = item.getVariant().getProduct().getName();
          }
        }

        lines.add(
            PurchaseOrderDto.ReceiveLineProgress.builder()
                .poLineId(item.getId())
                .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                .skuCode(sku)
                .productName(productName)
                .variantName(variantName)
                .quantityOrdered(ordered)
                .quantityReceived(received)
                .quantityDamaged(damaged)
                .remaining(Math.max(0, ordered - received - damaged))
                .receivedSerials(serials)
                .damagedSerials(damagedSerials)
                .build());
      }
    }

    int totalRemaining = Math.max(0, totalOrdered - totalReceived - totalDamaged);
    boolean complete = totalOrdered > 0 && totalRemaining <= 0;
    boolean receiving = po.getStatus() == PurchaseOrderStatus.RECEIVING;
    boolean hasActivity = totalReceived + totalDamaged > 0;

    return PurchaseOrderDto.ReceiveDetailResponse.builder()
        .id(po.getId())
        .code(po.getPoNumber())
        .supplier(po.getSupplier() != null ? po.getSupplier().getName() : "—")
        .status(toFeStatus(po.getStatus()))
        .rawStatus(po.getStatus() != null ? po.getStatus().name() : null)
        .expectedDate(po.getExpectedDate() != null ? po.getExpectedDate().format(VI_DATE) : "—")
        .notes(po.getNotes())
        .defaultBatchNumber(resolveActiveBatchNumber(po, totalRemaining))
        .canStartReceiving(po.getStatus() == PurchaseOrderStatus.APPROVED)
        .canScan(receiving && !complete)
        .canComplete(receiving && complete)
        .canLockOrder(receiving && hasActivity)
        .stockLots(buildStockLots(po, totalRemaining))
        .progress(
            PurchaseOrderDto.ReceiveProgress.builder()
                .totalOrdered(totalOrdered)
                .totalReceived(totalReceived)
                .totalDamaged(totalDamaged)
                .totalRemaining(totalRemaining)
                .complete(complete)
                .lines(lines)
                .build())
        .build();
  }

  private LocalDate parseExpectedDate(String value) {
    if (value == null || value.isBlank()) {
      return LocalDate.now().plusDays(7);
    }
    try {
      return LocalDate.parse(value.trim(), ISO_DATE);
    } catch (Exception e) {
      throw new BadRequestException("Ngày hẹn giao không hợp lệ (yyyy-MM-dd)");
    }
  }

  private String generatePoNumber() {
    long seq = purchaseOrderRepository.count() + 1;
    return String.format("PO-%d-%04d", LocalDate.now().getYear(), seq);
  }

  private PurchaseOrderDto.SummaryResponse toSummary(PurchaseOrder po) {
    return PurchaseOrderDto.SummaryResponse.builder()
        .id(po.getId())
        .code(po.getPoNumber())
        .supplier(po.getSupplier() != null ? po.getSupplier().getName() : "—")
        .supplierId(po.getSupplier() != null ? po.getSupplier().getId() : null)
        .items(po.getItems() != null ? po.getItems().size() : 0)
        .totalQuantity(sumOrderedQuantity(po))
        .expectedDate(po.getExpectedDate() != null ? po.getExpectedDate().format(VI_DATE) : "—")
        .status(toFeStatus(po.getStatus()))
        .rawStatus(po.getStatus() != null ? po.getStatus().name() : null)
        .totalAmount(po.getTotalAmount())
        .notes(po.getNotes())
        .rejectReason(po.getRejectReason())
        .build();
  }

  private int sumOrderedQuantity(PurchaseOrder po) {
    if (po.getItems() == null || po.getItems().isEmpty()) {
      return 0;
    }
    return po.getItems().stream()
        .mapToInt(i -> i.getQuantityOrdered() != null ? i.getQuantityOrdered() : 0)
        .sum();
  }

  private PurchaseOrderDto.DetailResponse toDetail(PurchaseOrder po) {
    List<PurchaseOrderDto.LineItem> lines =
        po.getItems() == null
            ? List.of()
            : po.getItems().stream().map(this::toLineItem).toList();

    return PurchaseOrderDto.DetailResponse.builder()
        .id(po.getId())
        .code(po.getPoNumber())
        .supplier(po.getSupplier() != null ? po.getSupplier().getName() : "—")
        .items(lines.size())
        .expectedDate(po.getExpectedDate() != null ? po.getExpectedDate().format(VI_DATE) : "—")
        .status(toFeStatus(po.getStatus()))
        .rawStatus(po.getStatus() != null ? po.getStatus().name() : null)
        .totalAmount(po.getTotalAmount())
        .totalQuantity(sumOrderedQuantity(po))
        .notes(po.getNotes())
        .rejectReason(po.getRejectReason())
        .orderDate(
            po.getOrderDate() != null
                ? po.getOrderDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : null)
        .receivedDate(
            po.getReceivedDate() != null
                ? po.getReceivedDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                : null)
        .lineItems(lines)
        .build();
  }

  private PurchaseOrderDto.LineItem toLineItem(PurchaseOrderItem item) {
    String productName = null;
    String sku = null;
    Integer variantId = null;
    if (item.getVariant() != null) {
      variantId = item.getVariant().getId();
      sku = item.getVariant().getSkuCode();
      if (item.getVariant().getProduct() != null) {
        productName = item.getVariant().getProduct().getName();
      }
    }
    return PurchaseOrderDto.LineItem.builder()
        .id(item.getId())
        .variantId(variantId)
        .skuCode(sku)
        .productName(productName)
        .quantityOrdered(item.getQuantityOrdered())
        .quantityReceived(item.getQuantityReceived() != null ? item.getQuantityReceived() : 0)
        .unitCost(item.getUnitCost())
        .build();
  }

  static String toFeStatus(PurchaseOrderStatus status) {
    return switch (status) {
      case APPROVED -> "pending";
      case RECEIVING -> "receiving";
      case COMPLETED -> "completed";
      default -> "pending";
    };
  }
}
