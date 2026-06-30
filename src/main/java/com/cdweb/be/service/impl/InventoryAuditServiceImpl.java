package com.cdweb.be.service.impl;

import com.cdweb.be.dto.InventoryAuditDto;
import com.cdweb.be.entity.Inventory;
import com.cdweb.be.entity.InventoryAuditSheet;
import com.cdweb.be.entity.InventoryAuditSheet.AuditStatus;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import com.cdweb.be.entity.ProductType;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.repository.InventoryAuditSheetRepository;
import com.cdweb.be.repository.InventoryRepository;
import com.cdweb.be.repository.ProductItemRepository;
import com.cdweb.be.repository.ProductTypeRepository;
import com.cdweb.be.repository.ProductVariantRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.service.InventoryAuditService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InventoryAuditServiceImpl implements InventoryAuditService {

  private static final DateTimeFormatter VI_DATE =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));

  private static final List<AuditStatus> ACTIVE_AUDIT_STATUSES =
      List.of(
          AuditStatus.DRAFT,
          AuditStatus.IN_PROGRESS,
          AuditStatus.SUBMITTED,
          AuditStatus.RECONCILED,
          AuditStatus.PENDING_APPROVAL);

  private final InventoryAuditSheetRepository sheetRepository;
  private final ProductItemRepository productItemRepository;
  private final ProductTypeRepository productTypeRepository;
  private final ProductVariantRepository productVariantRepository;
  private final InventoryRepository inventoryRepository;
  private final UserRepository userRepository;
  private final ObjectMapper objectMapper;

  @Override
  public List<InventoryAuditDto.SheetResponse> listSheets() {
    return sheetRepository.findAllByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
  }

  @Override
  public List<InventoryAuditDto.SheetResponse> listRecentSheets() {
    return sheetRepository.findTop8ByOrderByCreatedAtDesc().stream().map(this::toResponse).toList();
  }

  @Override
  public List<InventoryAuditDto.SheetResponse> listPendingApprovalSheets() {
    return sheetRepository
        .findByStatusInOrderByCreatedAtDesc(
            List.of(AuditStatus.PENDING_APPROVAL, AuditStatus.SUBMITTED))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public List<InventoryAuditDto.SheetResponse> listProcessedSheets() {
    return sheetRepository
        .findTop10ByStatusInOrderByUpdatedAtDesc(List.of(AuditStatus.APPROVED, AuditStatus.REJECTED))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public InventoryAuditDto.StatsResponse getStats() {
    long inProgress =
        sheetRepository.countByStatusIn(
            List.of(AuditStatus.DRAFT, AuditStatus.IN_PROGRESS, AuditStatus.RECONCILED));
    long pending =
        sheetRepository.countByStatusIn(
            List.of(AuditStatus.SUBMITTED, AuditStatus.PENDING_APPROVAL));
    long approved = sheetRepository.countByStatus(AuditStatus.APPROVED);
    long rejected = sheetRepository.countByStatus(AuditStatus.REJECTED);
    return InventoryAuditDto.StatsResponse.builder()
        .inProgressCount(inProgress)
        .pendingApprovalCount(pending)
        .approvedCount(approved)
        .rejectedCount(rejected)
        .draftCount(inProgress)
        .submittedCount(pending)
        .build();
  }

  @Override
  public InventoryAuditDto.SheetResponse getSheet(Integer id) {
    return toResponse(findSheet(id));
  }

  @Override
  public InventoryAuditDto.ScanProgressResponse getScanProgress(Integer id) {
    InventoryAuditSheet sheet = findSheet(id);
    assertScannable(sheet);
    Map<Integer, InventoryAuditDto.ScanProgressLine> lines = new LinkedHashMap<>();
    int orphanSurplus = 0;
    for (String code : sheet.getScannedCodes()) {
      Optional<ProductItem> opt = resolveItem(code);
      if (opt.isEmpty()) {
        orphanSurplus++;
        continue;
      }
      ProductItem item = opt.get();
      if (!isInAuditCategory(item, sheet.getProductTypeId())) {
        orphanSurplus++;
        continue;
      }
      ProductVariant v = item.getVariant();
      if (v == null) continue;
      lines.compute(
          v.getId(),
          (vid, line) -> {
            if (line == null) {
              return InventoryAuditDto.ScanProgressLine.builder()
                  .variantId(vid)
                  .productName(v.getProduct() != null ? v.getProduct().getName() : null)
                  .variantName(v.getVariantName())
                  .skuCode(v.getSkuCode())
                  .actualQty(1)
                  .build();
            }
            line.setActualQty(line.getActualQty() + 1);
            return line;
          });
    }
    if (orphanSurplus > 0) {
      lines.put(
          -1,
          InventoryAuditDto.ScanProgressLine.builder()
              .productName("Mã thừa / ngoài danh mục")
              .variantName(orphanSurplus + " mã")
              .skuCode("—")
              .actualQty(orphanSurplus)
              .build());
    }
    return InventoryAuditDto.ScanProgressResponse.builder()
        .totalScanned(sheet.getScannedCodes().size())
        .expectedCount(sheet.getExpectedCount())
        .hideSystemQty(true)
        .lines(new ArrayList<>(lines.values()))
        .build();
  }

  @Override
  @Transactional
  public InventoryAuditDto.SheetResponse startSheet(
      InventoryAuditDto.StartRequest request, String username) {
    ProductType type =
        productTypeRepository
            .findById(request.getProductTypeId())
            .orElseThrow(() -> new BadRequestException("Danh mục không tồn tại"));

    if (sheetRepository.existsByProductTypeIdAndRetailLockedTrueAndStatusIn(
        type.getId(), ACTIVE_AUDIT_STATUSES)) {
      throw new BadRequestException(
          "Danh mục \"" + type.getName() + "\" đang có phiên kiểm kê chưa hoàn tất");
    }

    User user = loadUser(username);
    List<ProductItem> expected = loadExpectedItems(type.getId());
    boolean lock = request.getRetailLocked() == null || request.getRetailLocked();

    InventoryAuditSheet sheet =
        InventoryAuditSheet.builder()
            .sheetCode(generateSheetCode())
            .status(AuditStatus.IN_PROGRESS)
            .productTypeId(type.getId())
            .categoryName(type.getName())
            .scannedCount(0)
            .expectedCount(expected.size())
            .matchedCount(0)
            .missingCount(0)
            .surplusCount(0)
            .variance(0)
            .retailLocked(lock)
            .note(request.getNote())
            .createdBy(user)
            .scannedCodes(new ArrayList<>())
            .missingCodes(new ArrayList<>())
            .surplusCodes(new ArrayList<>())
            .build();

    sheetRepository.save(sheet);
    return toResponse(sheet);
  }

  @Override
  @Transactional
  public InventoryAuditDto.SheetResponse createSheet(
      InventoryAuditDto.CreateRequest request, String username) {
    List<String> codes = normalizeCodes(request.getScannedCodes());
    if (codes.isEmpty()) {
      throw new BadRequestException("Danh sách mã quét không được rỗng");
    }
    InventoryAuditDto.StartRequest start = new InventoryAuditDto.StartRequest();
    start.setNote(request.getNote());
    start.setRetailLocked(false);
    ProductType firstType =
        productTypeRepository.findAllActive().stream()
            .findFirst()
            .orElseThrow(() -> new BadRequestException("Chưa có danh mục sản phẩm"));
    start.setProductTypeId(firstType.getId());
    InventoryAuditDto.SheetResponse created = startSheet(start, username);
    bulkScan(created.getId(), new InventoryAuditDto.BulkScanRequest(codes));
    completeSheet(created.getId());
    return getSheet(created.getId());
  }

  @Override
  @Transactional
  public InventoryAuditDto.ScanResponse scanCode(Integer id, InventoryAuditDto.ScanRequest request) {
    InventoryAuditSheet sheet = findSheet(id);
    assertScannable(sheet);
    String code = normalizeCode(request.getCode());
    InventoryAuditDto.ScanResponse result = processScan(sheet, code, request.getShelfLocation());
    sheet.setScannedCount(sheet.getScannedCodes().size());
    sheetRepository.save(sheet);
    result.setTotalScanned(sheet.getScannedCount());
    return result;
  }

  @Override
  @Transactional
  public InventoryAuditDto.BulkScanResponse bulkScan(
      Integer id, InventoryAuditDto.BulkScanRequest request) {
    InventoryAuditSheet sheet = findSheet(id);
    assertScannable(sheet);
    int matched = 0;
    int surplus = 0;
    int duplicate = 0;
    int misplacement = 0;
    List<InventoryAuditDto.ScanResponse> results = new ArrayList<>();
    for (String raw : normalizeCodes(request.getCodes())) {
      InventoryAuditDto.ScanResponse r = processScan(sheet, raw, null);
      results.add(r);
      switch (r.getResultType()) {
        case "MATCHED" -> matched++;
        case "MISPLACED" -> {
          matched++;
          misplacement++;
        }
        case "SURPLUS" -> surplus++;
        case "DUPLICATE" -> duplicate++;
        default -> {}
      }
    }
    sheet.setScannedCount(sheet.getScannedCodes().size());
    sheetRepository.save(sheet);
    return InventoryAuditDto.BulkScanResponse.builder()
        .total(results.size())
        .matched(matched)
        .surplus(surplus)
        .duplicate(duplicate)
        .misplacement(misplacement)
        .results(results)
        .totalScanned(sheet.getScannedCount())
        .build();
  }

  @Override
  @Transactional
  public InventoryAuditDto.CompleteResponse completeSheet(Integer id) {
    InventoryAuditSheet sheet = findSheet(id);
    if (!isScannableStatus(sheet.getStatus()) && sheet.getStatus() != AuditStatus.RECONCILED) {
      throw new BadRequestException("Phiếu không ở trạng thái quét đếm");
    }
    if (sheet.getScannedCodes().isEmpty()) {
      throw new BadRequestException("Chưa quét mã nào — không thể hoàn tất kiểm đếm");
    }

    ReconciliationResult result = reconcile(sheet);
    sheet.setMatchedCount(result.matchedCodes.size());
    sheet.setMissingCount(result.missingCodes.size());
    sheet.setSurplusCount(result.surplusCodes.size());
    sheet.setVariance(result.matchedCodes.size() - sheet.getExpectedCount());
    sheet.setMissingCodes(new ArrayList<>(result.missingCodes));
    sheet.setSurplusCodes(new ArrayList<>(result.surplusCodes));
    sheet.setReconciliationJson(writeJson(result.lines));
    sheet.setReconciledAt(LocalDateTime.now());
    sheet.setStatus(AuditStatus.RECONCILED);
    sheetRepository.save(sheet);

    String summary =
        String.format(
            "Đối chiếu xong — Khớp: %d, Thiếu: %d, Thừa: %d",
            result.matchedCodes.size(), result.missingCodes.size(), result.surplusCodes.size());

    return InventoryAuditDto.CompleteResponse.builder()
        .sheet(toResponse(sheet, result.lines, result.discrepancies))
        .summary(summary)
        .build();
  }

  @Override
  @Transactional
  public InventoryAuditDto.SheetResponse submitSheet(
      Integer id, InventoryAuditDto.SubmitRequest request) {
    InventoryAuditSheet sheet = findSheet(id);
    if (sheet.getStatus() != AuditStatus.RECONCILED) {
      throw new BadRequestException("Hoàn tất đối chiếu trước khi gửi duyệt");
    }
    if (request != null && request.getNote() != null && !request.getNote().isBlank()) {
      sheet.setNote(request.getNote().trim());
    }
    sheet.setStatus(AuditStatus.PENDING_APPROVAL);
    sheetRepository.save(sheet);
    return toResponse(sheet);
  }

  @Override
  @Transactional
  public InventoryAuditDto.SheetResponse approveSheet(Integer id, String username) {
    InventoryAuditSheet sheet = findSheet(id);
    if (sheet.getStatus() != AuditStatus.PENDING_APPROVAL
        && sheet.getStatus() != AuditStatus.SUBMITTED) {
      throw new BadRequestException("Chỉ phiếu chờ duyệt mới có thể phê duyệt");
    }

    User approver = loadUser(username);
    Set<Integer> affectedVariants = new HashSet<>();

    for (String code : sheet.getMissingCodes()) {
      resolveItem(code)
          .ifPresent(
              item -> {
                if (item.getStatus() == ProductItemStatus.AVAILABLE) {
                  item.setStatus(ProductItemStatus.MISSING);
                  item.setNotes(
                      "Kiểm kê thất lạc — phiếu "
                          + sheet.getSheetCode()
                          + (sheet.getNote() != null ? " | " + sheet.getNote() : ""));
                  productItemRepository.save(item);
                  if (item.getVariant() != null) {
                    affectedVariants.add(item.getVariant().getId());
                  }
                }
              });
    }

  for (Integer variantId : affectedVariants) {
      syncVariantStockFromSerials(variantId, approver, sheet.getSheetCode());
    }

    List<InventoryAuditDto.ReconciliationLine> lines = readLines(sheet);
    for (InventoryAuditDto.ReconciliationLine line : lines) {
      if (line.getVariance() != 0 && line.getVariantId() != null) {
        syncVariantStockFromSerials(line.getVariantId(), approver, sheet.getSheetCode());
      }
    }

    sheet.setStatus(AuditStatus.APPROVED);
    sheet.setApprovedBy(approver);
    sheet.setApprovedAt(LocalDateTime.now());
    sheet.setRetailLocked(false);
    sheetRepository.save(sheet);
    return toResponse(sheet);
  }

  @Override
  @Transactional
  public InventoryAuditDto.SheetResponse rejectSheet(
      Integer id, InventoryAuditDto.RejectRequest request) {
    InventoryAuditSheet sheet = findSheet(id);
    if (sheet.getStatus() != AuditStatus.PENDING_APPROVAL
        && sheet.getStatus() != AuditStatus.SUBMITTED) {
      throw new BadRequestException("Chỉ phiếu chờ duyệt mới có thể từ chối");
    }
    String reason = request != null ? request.getReason() : null;
    if (reason == null || reason.isBlank()) {
      throw new BadRequestException("Lý do từ chối là bắt buộc");
    }
    sheet.setStatus(AuditStatus.REJECTED);
    sheet.setRejectReason(reason.trim());
    sheet.setRetailLocked(false);
    sheetRepository.save(sheet);
    return toResponse(sheet);
  }

  @Override
  @Transactional
  public InventoryAuditDto.SheetResponse updateNote(
      Integer id, InventoryAuditDto.UpdateNoteRequest request) {
    InventoryAuditSheet sheet = findSheet(id);
    if (sheet.getStatus() == AuditStatus.APPROVED) {
      throw new BadRequestException("Phiếu đã duyệt không thể sửa ghi chú");
    }
    sheet.setNote(request.getNote());
    sheetRepository.save(sheet);
    return toResponse(sheet);
  }

  @Override
  public boolean isProductTypeUnderAudit(Integer productTypeId) {
    if (productTypeId == null) return false;
    return sheetRepository.existsByProductTypeIdAndRetailLockedTrueAndStatusIn(
        productTypeId, ACTIVE_AUDIT_STATUSES);
  }

  @Override
  public void assertNoAuditLockForProductType(Integer productTypeId) {
    if (isProductTypeUnderAudit(productTypeId)) {
      throw new BadRequestException(
          "Danh mục đang kiểm kê — tạm khóa bán lẻ. Vui lòng thử lại sau.");
    }
  }

  private void syncVariantStockFromSerials(Integer variantId, User user, String sheetCode) {
    ProductVariant variant =
        productVariantRepository
            .findById(variantId)
            .orElseThrow(() -> new BadRequestException("Variant không tồn tại: " + variantId));
    int oldQty = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
    int newQty =
        (int)
            productItemRepository.countByVariantIdAndStatus(
                variantId, ProductItemStatus.AVAILABLE);
    if (oldQty == newQty) return;

    variant.setStockQuantity(newQty);
    productVariantRepository.save(variant);

    Inventory tx = new Inventory();
    tx.setTransactionType(Inventory.TransactionType.ADJUSTMENT);
    tx.setQuantity(Math.abs(newQty - oldQty));
    tx.setVariant(variant);
    tx.setReferenceType("INVENTORY_AUDIT");
    tx.setReferenceId(null);
    tx.setReason(
        "Kiểm kê "
            + sheetCode
            + " — điều chỉnh tồn "
            + oldQty
            + " → "
            + newQty);
    tx.setUser(user);
    inventoryRepository.save(tx);
  }

  private InventoryAuditDto.ScanResponse processScan(
      InventoryAuditSheet sheet, String code, String shelfLocation) {
    if (sheet.getStatus() == AuditStatus.REJECTED) {
      sheet.setStatus(AuditStatus.IN_PROGRESS);
      sheet.setRejectReason(null);
    }
    if (sheet.getScannedCodes().contains(code)) {
      return InventoryAuditDto.ScanResponse.builder()
          .code(code)
          .resultType("DUPLICATE")
          .message("Mã đã quét trước đó")
          .build();
    }

    Optional<ProductItem> opt = resolveItem(code);
    if (opt.isEmpty()) {
      sheet.getScannedCodes().add(code);
      return InventoryAuditDto.ScanResponse.builder()
          .code(code)
          .resultType("SURPLUS")
          .message("Không có trên hệ thống (Thừa)")
          .build();
    }

    ProductItem item = opt.get();
    boolean inCategory = isInAuditCategory(item, sheet.getProductTypeId());
    if (!inCategory) {
      sheet.getScannedCodes().add(code);
      return InventoryAuditDto.ScanResponse.builder()
          .code(code)
          .resultType("SURPLUS")
          .message("Serial không thuộc danh mục kiểm kê")
          .productName(item.getVariant().getProduct().getName())
          .skuCode(item.getVariant().getSkuCode())
          .build();
    }

    if (item.getStatus() != ProductItemStatus.AVAILABLE) {
      sheet.getScannedCodes().add(code);
      return InventoryAuditDto.ScanResponse.builder()
          .code(code)
          .resultType("SURPLUS")
          .message("Serial không ở trạng thái AVAILABLE (" + item.getStatus() + ")")
          .productName(item.getVariant().getProduct().getName())
          .skuCode(item.getVariant().getSkuCode())
          .build();
    }

    boolean misplacement = false;
    String expectedLocation = item.getLocation();
    if (shelfLocation != null
        && !shelfLocation.isBlank()
        && expectedLocation != null
        && !expectedLocation.isBlank()
        && !expectedLocation.equalsIgnoreCase(shelfLocation.trim())) {
      misplacement = true;
    }

    sheet.getScannedCodes().add(code);
    return InventoryAuditDto.ScanResponse.builder()
        .code(code)
        .resultType(misplacement ? "MISPLACED" : "MATCHED")
        .message(misplacement ? "Khớp nhưng sai vị trí kệ" : "Serial khớp!")
        .productName(item.getVariant().getProduct().getName())
        .skuCode(item.getVariant().getSkuCode())
        .expectedLocation(expectedLocation)
        .scannedLocation(shelfLocation)
        .build();
  }

  private ReconciliationResult reconcile(InventoryAuditSheet sheet) {
    List<ProductItem> expected = loadExpectedItems(sheet.getProductTypeId());
    Set<String> scanned = new LinkedHashSet<>(sheet.getScannedCodes());
    Map<String, ProductItem> expectedByCode = new HashMap<>();
    for (ProductItem item : expected) {
      String key = displayCode(item);
      if (key != null) expectedByCode.put(key, item);
    }

    Set<String> matchedCodes = new LinkedHashSet<>();
    Set<String> missingCodes = new LinkedHashSet<>();
    Set<String> surplusCodes = new LinkedHashSet<>();

    for (ProductItem item : expected) {
      String code = displayCode(item);
      if (code != null && scanned.contains(code)) {
        matchedCodes.add(code);
      } else if (code != null) {
        missingCodes.add(code);
      }
    }

    for (String code : scanned) {
      if (!expectedByCode.containsKey(code)) {
        surplusCodes.add(code);
      }
    }

    Map<Integer, Integer> systemByVariant = new HashMap<>();
    Map<Integer, Integer> actualByVariant = new HashMap<>();
    Map<Integer, ProductVariant> variantMap = new HashMap<>();

    for (ProductItem item : expected) {
      if (item.getVariant() == null) continue;
      Integer vid = item.getVariant().getId();
      variantMap.put(vid, item.getVariant());
      systemByVariant.merge(vid, 1, Integer::sum);
    }
    for (String code : matchedCodes) {
      ProductItem item = expectedByCode.get(code);
      if (item != null && item.getVariant() != null) {
        actualByVariant.merge(item.getVariant().getId(), 1, Integer::sum);
      }
    }

    Set<Integer> allVariantIds = new HashSet<>();
    allVariantIds.addAll(systemByVariant.keySet());
    allVariantIds.addAll(actualByVariant.keySet());

    List<InventoryAuditDto.ReconciliationLine> lines = new ArrayList<>();
    List<InventoryAuditDto.DiscrepancyDetail> discrepancies = new ArrayList<>();

    for (Integer vid : allVariantIds) {
      int systemQty = systemByVariant.getOrDefault(vid, 0);
      int actualQty = actualByVariant.getOrDefault(vid, 0);
      int variance = actualQty - systemQty;
      ProductVariant v = variantMap.get(vid);
      if (v == null) {
        v = productVariantRepository.findById(vid).orElse(null);
      }
      String status;
      if (variance == 0) status = "MATCHED";
      else if (variance < 0) status = "SHORTAGE";
      else status = "SURPLUS";

      lines.add(
          InventoryAuditDto.ReconciliationLine.builder()
              .variantId(vid)
              .productName(v != null && v.getProduct() != null ? v.getProduct().getName() : null)
              .variantName(v != null ? v.getVariantName() : null)
              .skuCode(v != null ? v.getSkuCode() : null)
              .systemQty(systemQty)
              .actualQty(actualQty)
              .variance(variance)
              .status(status)
              .build());
    }

    for (String code : missingCodes) {
      ProductItem item = expectedByCode.get(code);
      discrepancies.add(
          InventoryAuditDto.DiscrepancyDetail.builder()
              .serial(code)
              .type("MISSING")
              .productName(
                  item != null && item.getVariant() != null && item.getVariant().getProduct() != null
                      ? item.getVariant().getProduct().getName()
                      : null)
              .skuCode(item != null && item.getVariant() != null ? item.getVariant().getSkuCode() : null)
              .expectedLocation(item != null ? item.getLocation() : null)
              .message("Hệ thống có nhưng không quét thấy")
              .build());
    }

    for (String code : surplusCodes) {
      resolveItem(code)
          .ifPresentOrElse(
              item ->
                  discrepancies.add(
                      InventoryAuditDto.DiscrepancyDetail.builder()
                          .serial(code)
                          .type("SURPLUS")
                          .productName(
                              item.getVariant() != null && item.getVariant().getProduct() != null
                                  ? item.getVariant().getProduct().getName()
                                  : null)
                          .skuCode(item.getVariant() != null ? item.getVariant().getSkuCode() : null)
                          .message("Quét thấy nhưng không thuộc tồn AVAILABLE danh mục")
                          .build()),
              () ->
                  discrepancies.add(
                      InventoryAuditDto.DiscrepancyDetail.builder()
                          .serial(code)
                          .type("SURPLUS")
                          .message("Mã không tồn tại trên hệ thống")
                          .build()));
    }

    return new ReconciliationResult(matchedCodes, missingCodes, surplusCodes, lines, discrepancies);
  }

  private List<ProductItem> loadExpectedItems(Integer productTypeId) {
    if (productTypeId == null) {
      return productItemRepository.findAll().stream()
          .filter(pi -> pi.getStatus() == ProductItemStatus.AVAILABLE)
          .toList();
    }
    return productItemRepository.findAvailableByProductTypeId(productTypeId);
  }

  private boolean isInAuditCategory(ProductItem item, Integer productTypeId) {
    if (productTypeId == null) return true;
    return item.getVariant() != null
        && item.getVariant().getProduct() != null
        && item.getVariant().getProduct().getProductType() != null
        && Objects.equals(item.getVariant().getProduct().getProductType().getId(), productTypeId);
  }

  private Optional<ProductItem> resolveItem(String code) {
    return productItemRepository.findByImeiOrSerialNumber(code, code);
  }

  private String displayCode(ProductItem item) {
    if (item.getImei() != null && !item.getImei().isBlank()) return item.getImei().trim();
    if (item.getSerialNumber() != null && !item.getSerialNumber().isBlank()) {
      return item.getSerialNumber().trim();
    }
    return null;
  }

  private InventoryAuditSheet findSheet(Integer id) {
    return sheetRepository
        .findById(id)
        .orElseThrow(() -> new BadRequestException("Phiếu kiểm kê không tồn tại: #" + id));
  }

  private User loadUser(String username) {
    return userRepository
        .findByUsernameOrEmail(username, username)
        .orElseThrow(() -> new BadRequestException("User not found"));
  }

  private void assertScannable(InventoryAuditSheet sheet) {
    if (!isScannableStatus(sheet.getStatus())) {
      throw new BadRequestException("Phiếu không ở trạng thái quét đếm");
    }
  }

  private boolean isScannableStatus(AuditStatus status) {
    return status == AuditStatus.IN_PROGRESS
        || status == AuditStatus.DRAFT
        || status == AuditStatus.REJECTED;
  }

  private String generateSheetCode() {
    long count = sheetRepository.count() + 1;
    return String.format("AUD-%d-%03d", java.time.Year.now().getValue(), count);
  }

  private List<String> normalizeCodes(List<String> raw) {
    if (raw == null) return List.of();
    Set<String> unique = new LinkedHashSet<>();
    for (String code : raw) {
      if (code != null && !code.isBlank()) unique.add(code.trim());
    }
    return new ArrayList<>(unique);
  }

  private String normalizeCode(String code) {
    if (code == null || code.isBlank()) {
      throw new BadRequestException("Mã quét không hợp lệ");
    }
    return code.trim();
  }

  private String writeJson(List<InventoryAuditDto.ReconciliationLine> lines) {
    try {
      return objectMapper.writeValueAsString(lines);
    } catch (JsonProcessingException e) {
      throw new BadRequestException("Không thể lưu báo cáo đối chiếu");
    }
  }

  private List<InventoryAuditDto.ReconciliationLine> readLines(InventoryAuditSheet sheet) {
    if (sheet.getReconciliationJson() == null || sheet.getReconciliationJson().isBlank()) {
      return List.of();
    }
    try {
      return objectMapper.readValue(
          sheet.getReconciliationJson(), new TypeReference<List<InventoryAuditDto.ReconciliationLine>>() {});
    } catch (JsonProcessingException e) {
      return List.of();
    }
  }

  private InventoryAuditDto.SheetResponse toResponse(InventoryAuditSheet sheet) {
    ReconciliationResult empty =
        new ReconciliationResult(Set.of(), Set.of(), Set.of(), readLines(sheet), List.of());
    if (sheet.getStatus() == AuditStatus.RECONCILED
        || sheet.getStatus() == AuditStatus.PENDING_APPROVAL
        || sheet.getStatus() == AuditStatus.SUBMITTED
        || sheet.getStatus() == AuditStatus.APPROVED
        || sheet.getStatus() == AuditStatus.REJECTED) {
      return toResponse(sheet, readLines(sheet), buildDiscrepanciesFromSheet(sheet));
    }
    return toResponse(sheet, empty.lines, empty.discrepancies);
  }

  private List<InventoryAuditDto.DiscrepancyDetail> buildDiscrepanciesFromSheet(
      InventoryAuditSheet sheet) {
    List<InventoryAuditDto.DiscrepancyDetail> list = new ArrayList<>();
    if (sheet.getMissingCodes() != null) {
      for (String code : sheet.getMissingCodes()) {
        list.add(
            InventoryAuditDto.DiscrepancyDetail.builder()
                .serial(code)
                .type("MISSING")
                .message("Thất lạc")
                .build());
      }
    }
    if (sheet.getSurplusCodes() != null) {
      for (String code : sheet.getSurplusCodes()) {
        list.add(
            InventoryAuditDto.DiscrepancyDetail.builder()
                .serial(code)
                .type("SURPLUS")
                .message("Thừa / lệch")
                .build());
      }
    }
    return list;
  }

  private InventoryAuditDto.SheetResponse toResponse(
      InventoryAuditSheet sheet,
      List<InventoryAuditDto.ReconciliationLine> lines,
      List<InventoryAuditDto.DiscrepancyDetail> discrepancies) {
    return InventoryAuditDto.SheetResponse.builder()
        .id(sheet.getId())
        .code(sheet.getSheetCode())
        .createdAt(sheet.getCreatedAt() != null ? sheet.getCreatedAt().format(VI_DATE) : "—")
        .productTypeId(sheet.getProductTypeId())
        .categoryName(sheet.getCategoryName())
        .scanned(sheet.getScannedCount() != null ? sheet.getScannedCount() : 0)
        .expected(sheet.getExpectedCount() != null ? sheet.getExpectedCount() : 0)
        .matched(sheet.getMatchedCount() != null ? sheet.getMatchedCount() : 0)
        .missing(sheet.getMissingCount() != null ? sheet.getMissingCount() : 0)
        .surplus(sheet.getSurplusCount() != null ? sheet.getSurplusCount() : 0)
        .variance(sheet.getVariance() != null ? sheet.getVariance() : 0)
        .status(toFeStatus(sheet.getStatus()))
        .note(sheet.getNote())
        .rejectReason(sheet.getRejectReason())
        .retailLocked(Boolean.TRUE.equals(sheet.getRetailLocked()))
        .wizardStep(resolveWizardStep(sheet.getStatus()))
        .scannedCodes(
            sheet.getScannedCodes() != null ? List.copyOf(sheet.getScannedCodes()) : List.of())
        .missingCodes(
            sheet.getMissingCodes() != null ? List.copyOf(sheet.getMissingCodes()) : List.of())
        .surplusCodes(
            sheet.getSurplusCodes() != null ? List.copyOf(sheet.getSurplusCodes()) : List.of())
        .lines(lines)
        .discrepancies(discrepancies)
        .createdByName(sheet.getCreatedBy() != null ? sheet.getCreatedBy().getUsername() : null)
        .approvedByName(sheet.getApprovedBy() != null ? sheet.getApprovedBy().getUsername() : null)
        .build();
  }

  private static int resolveWizardStep(AuditStatus status) {
    return switch (status) {
      case DRAFT, IN_PROGRESS, REJECTED -> 2;
      case RECONCILED -> 3;
      case SUBMITTED, PENDING_APPROVAL, APPROVED -> 3;
    };
  }

  private static String toFeStatus(AuditStatus status) {
    return switch (status) {
      case DRAFT, IN_PROGRESS -> "in_progress";
      case RECONCILED -> "reconciled";
      case SUBMITTED, PENDING_APPROVAL -> "pending_approval";
      case APPROVED -> "approved";
      case REJECTED -> "rejected";
    };
  }

  private record ReconciliationResult(
      Set<String> matchedCodes,
      Set<String> missingCodes,
      Set<String> surplusCodes,
      List<InventoryAuditDto.ReconciliationLine> lines,
      List<InventoryAuditDto.DiscrepancyDetail> discrepancies) {}
}
