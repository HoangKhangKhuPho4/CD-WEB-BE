package com.cdweb.be.service.impl;

import com.cdweb.be.dto.ImeiDto;
import com.cdweb.be.dto.ReturnInspectionDto;
import com.cdweb.be.entity.Order;
import com.cdweb.be.entity.OrderItem;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import com.cdweb.be.entity.ReturnInspectionSheet;
import com.cdweb.be.entity.ReturnInspectionSheet.DefectCause;
import com.cdweb.be.entity.ReturnInspectionSheet.ReturnJudgment;
import com.cdweb.be.entity.ReturnInspectionSheet.SheetStatus;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.repository.OrderItemRepository;
import com.cdweb.be.repository.OrderRepository;
import com.cdweb.be.repository.ProductItemRepository;
import com.cdweb.be.repository.ReturnInspectionSheetRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.service.ImeiService;
import com.cdweb.be.service.ReturnInspectionService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReturnInspectionServiceImpl implements ReturnInspectionService {

  private static final DateTimeFormatter VI_DT =
      DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("vi-VN"));

  private final ReturnInspectionSheetRepository sheetRepository;
  private final ProductItemRepository productItemRepository;
  private final OrderRepository orderRepository;
  private final OrderItemRepository orderItemRepository;
  private final UserRepository userRepository;
  private final ImeiService imeiService;

  @Override
  public List<ReturnInspectionDto.SheetSummary> listPending() {
    return sheetRepository.findByStatusOrderByCreatedAtDesc(SheetStatus.PENDING).stream()
        .map(this::toSummary)
        .toList();
  }

  @Override
  public List<ReturnInspectionDto.SheetSummary> listDrafts() {
    return sheetRepository.findByStatusOrderByUpdatedAtDesc(SheetStatus.DRAFT).stream()
        .map(this::toSummary)
        .toList();
  }

  @Override
  public List<ReturnInspectionDto.SheetSummary> listProcessed() {
    return sheetRepository
        .findTop20ByStatusInOrderByProcessedAtDesc(
            List.of(SheetStatus.PROCESSED, SheetStatus.REJECTED, SheetStatus.CANCELLED))
        .stream()
        .map(this::toSummary)
        .toList();
  }

  @Override
  public ReturnInspectionDto.DefectLabelResponse getDefectLabel(Integer id) {
    ReturnInspectionSheet sheet = findSheet(id);
    if (sheet.getJudgment() != ReturnJudgment.DEFECTIVE) {
      throw new BadRequestException("Chỉ in nhãn cho hàng lỗi (DEFECTIVE)");
    }
    String zone =
        sheet.getDefectCause() == DefectCause.SHIPPING
            ? "KHU CÁCH LY — LỖI VẬN CHUYỂN"
            : sheet.getDefectCause() == DefectCause.MANUFACTURER
                ? "KHU CÁCH LY — LỖI SẢN XUẤT"
                : "KHU CÁCH LY — HÀNG LỖI";
    return ReturnInspectionDto.DefectLabelResponse.builder()
        .sheetCode(sheet.getSheetCode())
        .serialCode(sheet.getSerialCode())
        .productName(sheet.getProductName())
        .variantName(sheet.getVariantName())
        .skuCode(sheet.getSkuCode())
        .orderCode(sheet.getOrderCode())
        .defectCause(sheet.getDefectCause() != null ? sheet.getDefectCause().name() : null)
        .detailReason(sheet.getDetailReason())
        .processedAt(formatDt(sheet.getProcessedAt() != null ? sheet.getProcessedAt() : LocalDateTime.now()))
        .zoneLabel(zone)
        .build();
  }

  @Override
  @Transactional
  public ReturnInspectionDto.SheetDetail saveDraft(
      Integer id, ReturnInspectionDto.DraftRequest request, String username) {
    ReturnInspectionSheet sheet = findSheet(id);
    assertEditable(sheet);
    loadUser(username);

    applyDraftFields(sheet, request);
    sheet.setStatus(SheetStatus.DRAFT);
    sheetRepository.save(sheet);
    return toDetail(sheet);
  }

  @Override
  @Transactional
  public ReturnInspectionDto.SheetDetail cancel(
      Integer id, ReturnInspectionDto.CancelRequest request, String username) {
    ReturnInspectionSheet sheet = findSheet(id);
    assertEditable(sheet);
    User user = loadUser(username);

    if (request == null
        || request.getCancelReason() == null
        || request.getCancelReason().isBlank()) {
      throw new BadRequestException("Vui lòng nhập lý do hủy phiếu");
    }

    sheet.setStatus(SheetStatus.CANCELLED);
    sheet.setCancelReason(request.getCancelReason().trim());
    sheet.setProcessedBy(user);
    sheet.setProcessedAt(LocalDateTime.now());
    sheetRepository.save(sheet);
    maybeConfirmOrderWarehouse(sheet.getOrderId());
    return toDetail(sheet);
  }

  @Override
  public ReturnInspectionDto.SheetDetail getDetail(Integer id) {
    ReturnInspectionSheet sheet = findSheet(id);
    return toDetail(sheet);
  }

  @Override
  @Transactional
  public ReturnInspectionDto.IntakeResponse intake(String code, String username) {
    if (code == null || code.isBlank()) {
      throw new BadRequestException("Nhập Serial/IMEI hoặc mã vận đơn");
    }
    String trimmed = code.trim();
    User user = loadUser(username);

    Optional<ProductItem> bySerial =
        productItemRepository.findByImeiOrSerialNumber(trimmed, trimmed);
    if (bySerial.isPresent()) {
      ReturnInspectionSheet sheet = intakeByProductItem(bySerial.get(), user);
      return ReturnInspectionDto.IntakeResponse.builder()
          .redirectSheetId(sheet.getId())
          .createdCount(1)
          .message("Đã tiếp nhận phiếu " + sheet.getSheetCode())
          .build();
    }

    Order order =
        orderRepository
            .findByTrackingCode(trimmed)
            .or(() -> orderRepository.findByGhnOrderCode(trimmed))
            .or(() -> orderRepository.findByOrderCode(trimmed))
            .orElseThrow(
                () ->
                    new BadRequestException(
                        "Không tìm thấy đơn/serial — kiểm tra mã vận đơn hoặc IMEI"));

    imeiService.markOrderReturnedForInspection(order.getId());
    int created = createSheetsForOrderInternal(order.getId(), user);
    List<ReturnInspectionSheet> pending =
        sheetRepository.findByOrderIdAndStatus(order.getId(), SheetStatus.PENDING);
    if (pending.isEmpty()) {
      throw new BadRequestException(
          "Đơn "
              + order.getOrderCode()
              + " không có serial chờ kiểm định (có thể đã xử lý hoặc không có IMEI)");
    }
    return ReturnInspectionDto.IntakeResponse.builder()
        .redirectSheetId(pending.get(0).getId())
        .createdCount(created)
        .message(
            "Đã tiếp nhận "
                + created
                + " phiếu từ đơn "
                + order.getOrderCode())
        .build();
  }

  @Override
  @Transactional
  public ReturnInspectionDto.SheetDetail process(
      Integer id, ReturnInspectionDto.ProcessRequest request, String username) {
    ReturnInspectionSheet sheet = findSheet(id);
    assertEditable(sheet);
    User user = loadUser(username);

    if (Boolean.TRUE.equals(request.getRejectMismatch())) {
      String reason =
          request.getRejectReason() != null && !request.getRejectReason().isBlank()
              ? request.getRejectReason().trim()
              : "IMEI không khớp — từ chối nhận hàng";
      sheet.setStatus(SheetStatus.REJECTED);
      sheet.setRejectReason(reason);
      sheet.setProcessedBy(user);
      sheet.setProcessedAt(LocalDateTime.now());
      sheetRepository.save(sheet);
      return toDetail(sheet);
    }

    assertSerialMatch(sheet, request.getScannedSerial());

    ReturnJudgment judgment = parseJudgment(request.getJudgment());
    DefectCause cause = null;
    if (judgment == ReturnJudgment.DEFECTIVE) {
      cause = parseDefectCause(request.getDefectCause());
    }

    boolean defective = judgment == ReturnJudgment.DEFECTIVE;
    String detail =
        request.getDetailReason() != null ? request.getDetailReason().trim() : "";
    if (defective && detail.isEmpty()) {
      throw new BadRequestException("Vui lòng mô tả tình trạng hàng lỗi/hư hỏng");
    }
    if (defective
        && (request.getEvidenceUrl() == null || request.getEvidenceUrl().isBlank())) {
      throw new BadRequestException("Hàng lỗi — bắt buộc tải ảnh minh chứng");
    }

    String reason = buildStockReason(judgment, cause, detail, request.getWarehouseNote());
    ImeiDto.ReturnRequest stockReq = new ImeiDto.ReturnRequest();
    stockReq.setImei(sheet.getSerialCode());
    stockReq.setIsDefective(defective);
    stockReq.setReason(reason);
    imeiService.returnStock(stockReq);

    sheet.setStatus(SheetStatus.PROCESSED);
    sheet.setJudgment(judgment);
    sheet.setDefectCause(cause);
    sheet.setDetailReason(detail.isEmpty() ? null : detail);
    sheet.setWarehouseNote(
        request.getWarehouseNote() != null && !request.getWarehouseNote().isBlank()
            ? request.getWarehouseNote().trim()
            : null);
    if (defective) {
      sheet.setEvidenceUrl(request.getEvidenceUrl().trim());
    }
    sheet.setProcessedBy(user);
    sheet.setProcessedAt(LocalDateTime.now());
    sheetRepository.save(sheet);

    maybeConfirmOrderWarehouse(sheet.getOrderId());
    return toDetail(sheet);
  }

  @Override
  @Transactional
  public void createSheetsForOrder(Integer orderId, String username) {
    User user = null;
    if (username != null && !username.isBlank() && !username.startsWith("system")) {
      user = loadUser(username);
    }
    createSheetsForOrderInternal(orderId, user);
  }

  private int createSheetsForOrderInternal(Integer orderId, User user) {
    List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
    int created = 0;
    for (OrderItem oi : items) {
      ProductItem pi = oi.getProductItem();
      if (pi == null) {
        continue;
      }
      if (pi.getStatus() != ProductItemStatus.RETURNED
          && pi.getStatus() != ProductItemStatus.SOLD) {
        continue;
      }
      if (sheetRepository.existsByProductItemIdAndStatusIn(
          pi.getId(), List.of(SheetStatus.PENDING, SheetStatus.DRAFT))) {
        continue;
      }
      if (sheetRepository.findByProductItemIdAndStatus(pi.getId(), SheetStatus.PROCESSED)
          .isPresent()) {
        continue;
      }
      buildSheetFromOrderItem(oi, user);
      created++;
    }
    return created;
  }

  private ReturnInspectionSheet intakeByProductItem(ProductItem pi, User user) {
    ProductItemStatus st = pi.getStatus();
    if (st != ProductItemStatus.SOLD
        && st != ProductItemStatus.RETURNED
        && st != ProductItemStatus.IN_REPAIR) {
      throw new BadRequestException(
          "Serial đang ở trạng thái "
              + st
              + " — không thể tiếp nhận hàng hoàn (cần SOLD/RETURNED/IN_REPAIR)");
    }

    Optional<ReturnInspectionSheet> existingOpen =
        sheetRepository.findByProductItemIdAndStatus(pi.getId(), SheetStatus.PENDING);
    if (existingOpen.isEmpty()) {
      existingOpen =
          sheetRepository.findByProductItemIdAndStatus(pi.getId(), SheetStatus.DRAFT);
    }
    if (existingOpen.isPresent()) {
      return existingOpen.get();
    }

    if (st == ProductItemStatus.SOLD) {
      pi.setStatus(ProductItemStatus.RETURNED);
      productItemRepository.save(pi);
    }

    List<OrderItem> links = orderItemRepository.findByProductItemIdWithOrder(pi.getId());
    if (links.isEmpty()) {
      throw new BadRequestException("Không tìm thấy đơn hàng gắn với serial này");
    }
    return buildSheetFromOrderItem(links.get(0), user);
  }

  private ReturnInspectionSheet buildSheetFromOrderItem(OrderItem oi, User user) {
    ProductItem pi = oi.getProductItem();
    Order order = oi.getOrderDetail().getOrder();
    String serial =
        pi.getImei() != null && !pi.getImei().isBlank()
            ? pi.getImei()
            : pi.getSerialNumber();

    ReturnInspectionSheet sheet =
        ReturnInspectionSheet.builder()
            .sheetCode(generateSheetCode())
            .status(SheetStatus.PENDING)
            .orderId(order.getId())
            .productItemId(pi.getId())
            .serialCode(serial)
            .orderCode(order.getOrderCode())
            .customerName(order.getShippingName())
            .customerPhone(order.getShippingPhone())
            .trackingCode(
                order.getTrackingCode() != null && !order.getTrackingCode().isBlank()
                    ? order.getTrackingCode()
                    : order.getGhnOrderCode())
            .createdBy(user)
            .build();

    if (pi.getVariant() != null) {
      sheet.setSkuCode(pi.getVariant().getSkuCode());
      sheet.setVariantName(pi.getVariant().getVariantName());
      if (pi.getVariant().getProduct() != null) {
        sheet.setProductName(pi.getVariant().getProduct().getName());
      }
    }

    return sheetRepository.save(sheet);
  }

  private void assertSerialMatch(ReturnInspectionSheet sheet, String scanned) {
    if (scanned == null || scanned.isBlank()) {
      throw new BadRequestException("Quét lại Serial/IMEI trên thiết bị để đối chiếu");
    }
    String expected = sheet.getSerialCode();
    if (expected == null || expected.isBlank()) {
      return;
    }
    if (!expected.trim().equalsIgnoreCase(scanned.trim())) {
      throw new BadRequestException(
          "IMEI không khớp! Hệ thống: "
              + expected
              + " — quét được: "
              + scanned.trim()
              + ". Từ chối nhận hàng nếu khách tráo máy.");
    }
  }

  private void assertEditable(ReturnInspectionSheet sheet) {
    if (sheet.getStatus() != SheetStatus.PENDING && sheet.getStatus() != SheetStatus.DRAFT) {
      throw new BadRequestException("Phiếu không thể chỉnh sửa ở trạng thái " + sheet.getStatus());
    }
  }

  private void applyDraftFields(ReturnInspectionSheet sheet, ReturnInspectionDto.DraftRequest request) {
    if (request == null) {
      return;
    }
    if (request.getScannedSerial() != null) {
      sheet.setDraftScannedSerial(request.getScannedSerial().trim());
    }
    if (request.getJudgment() != null && !request.getJudgment().isBlank()) {
      sheet.setJudgment(parseJudgmentOptional(request.getJudgment()));
    }
    if (request.getDefectCause() != null && !request.getDefectCause().isBlank()) {
      sheet.setDefectCause(parseDefectCause(request.getDefectCause()));
    }
    if (request.getDetailReason() != null) {
      sheet.setDetailReason(request.getDetailReason().trim());
    }
    if (request.getWarehouseNote() != null) {
      sheet.setWarehouseNote(request.getWarehouseNote().trim());
    }
    if (request.getEvidenceUrl() != null && !request.getEvidenceUrl().isBlank()) {
      sheet.setEvidenceUrl(request.getEvidenceUrl().trim());
    }
  }

  private ReturnJudgment parseJudgmentOptional(String raw) {
    return switch (raw.trim().toUpperCase(Locale.ROOT)) {
      case "GOOD", "AVAILABLE" -> ReturnJudgment.GOOD;
      case "DEFECTIVE", "DAMAGED" -> ReturnJudgment.DEFECTIVE;
      default -> null;
    };
  }

  private void maybeConfirmOrderWarehouse(Integer orderId) {
    if (orderId == null) {
      return;
    }
    long open =
        sheetRepository.countByOrderIdAndStatusIn(
            orderId, List.of(SheetStatus.PENDING, SheetStatus.DRAFT));
    if (open > 0) {
      return;
    }
    orderRepository
        .findById(orderId)
        .ifPresent(
            order -> {
              if (order.getWarehouseReturnConfirmedAt() == null) {
                order.setWarehouseReturnConfirmedAt(LocalDateTime.now());
                orderRepository.save(order);
              }
            });
  }

  private ReturnJudgment parseJudgment(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new BadRequestException("Chọn đánh giá tình trạng hàng");
    }
    return switch (raw.trim().toUpperCase(Locale.ROOT)) {
      case "GOOD", "AVAILABLE" -> ReturnJudgment.GOOD;
      case "DEFECTIVE", "DAMAGED" -> ReturnJudgment.DEFECTIVE;
      default -> throw new BadRequestException("Đánh giá không hợp lệ: " + raw);
    };
  }

  private DefectCause parseDefectCause(String raw) {
    if (raw == null || raw.isBlank()) {
      throw new BadRequestException("Hàng lỗi — chọn nguyên nhân: Vận chuyển hoặc Sản xuất");
    }
    return switch (raw.trim().toUpperCase(Locale.ROOT)) {
      case "SHIPPING", "CARRIER", "GHN", "GHTK" -> DefectCause.SHIPPING;
      case "MANUFACTURER", "FACTORY", "OEM" -> DefectCause.MANUFACTURER;
      default -> throw new BadRequestException("Nguyên nhân lỗi không hợp lệ: " + raw);
    };
  }

  private String buildStockReason(
      ReturnJudgment judgment, DefectCause cause, String detail, String warehouseNote) {
    StringBuilder sb = new StringBuilder();
    if (judgment == ReturnJudgment.GOOD) {
      sb.append("Hoàn kho — hàng nguyên vẹn");
    } else {
      sb.append("Hoàn kho — hàng lỗi (");
      sb.append(cause == DefectCause.SHIPPING ? "Vận chuyển" : "Sản xuất");
      sb.append(")");
    }
    if (!detail.isEmpty()) {
      sb.append(": ").append(detail);
    }
    if (warehouseNote != null && !warehouseNote.isBlank()) {
      sb.append(" | Kho: ").append(warehouseNote.trim());
    }
    return sb.toString();
  }

  private String generateSheetCode() {
    long seq = sheetRepository.count() + 1;
    return String.format("RT-%d-%05d", LocalDate.now().getYear(), seq);
  }

  private ReturnInspectionSheet findSheet(Integer id) {
    return sheetRepository
        .findById(id)
        .orElseThrow(() -> new BadRequestException("Phiếu hoàn trả không tồn tại: #" + id));
  }

  private User loadUser(String username) {
    return userRepository
        .findByUsernameOrEmail(username, username)
        .orElseThrow(() -> new BadRequestException("Không tìm thấy user: " + username));
  }

  private ReturnInspectionDto.SheetSummary toSummary(ReturnInspectionSheet s) {
    boolean confirmed = false;
    if (s.getOrderId() != null) {
      confirmed =
          orderRepository
              .findById(s.getOrderId())
              .map(o -> o.getWarehouseReturnConfirmedAt() != null)
              .orElse(false);
    }
    return ReturnInspectionDto.SheetSummary.builder()
        .id(s.getId())
        .sheetCode(s.getSheetCode())
        .status(feStatus(s.getStatus()))
        .serialCode(s.getSerialCode())
        .orderCode(s.getOrderCode())
        .customerName(s.getCustomerName())
        .customerPhone(s.getCustomerPhone())
        .productName(s.getProductName())
        .variantName(s.getVariantName())
        .skuCode(s.getSkuCode())
        .trackingCode(s.getTrackingCode())
        .judgment(s.getJudgment() != null ? s.getJudgment().name() : null)
        .defectCause(s.getDefectCause() != null ? s.getDefectCause().name() : null)
        .createdAt(formatDt(s.getCreatedAt()))
        .processedAt(formatDt(s.getProcessedAt()))
        .warehouseConfirmed(confirmed)
        .build();
  }

  private ReturnInspectionDto.SheetDetail toDetail(ReturnInspectionSheet s) {
    boolean orderConfirmed = false;
    if (s.getOrderId() != null) {
      orderConfirmed =
          orderRepository
              .findById(s.getOrderId())
              .map(o -> o.getWarehouseReturnConfirmedAt() != null)
              .orElse(false);
    }
    return ReturnInspectionDto.SheetDetail.builder()
        .id(s.getId())
        .sheetCode(s.getSheetCode())
        .status(feStatus(s.getStatus()))
        .serialCode(s.getSerialCode())
        .orderCode(s.getOrderCode())
        .orderId(s.getOrderId())
        .customerName(s.getCustomerName())
        .customerPhone(s.getCustomerPhone())
        .productName(s.getProductName())
        .variantName(s.getVariantName())
        .skuCode(s.getSkuCode())
        .trackingCode(s.getTrackingCode())
        .judgment(s.getJudgment() != null ? s.getJudgment().name() : null)
        .defectCause(s.getDefectCause() != null ? s.getDefectCause().name() : null)
        .detailReason(s.getDetailReason())
        .warehouseNote(s.getWarehouseNote())
        .rejectReason(s.getRejectReason())
        .cancelReason(s.getCancelReason())
        .evidenceUrl(s.getEvidenceUrl())
        .draftScannedSerial(s.getDraftScannedSerial())
        .createdAt(formatDt(s.getCreatedAt()))
        .processedAt(formatDt(s.getProcessedAt()))
        .orderWarehouseConfirmed(orderConfirmed)
        .build();
  }

  private String feStatus(SheetStatus status) {
    if (status == null) {
      return "pending";
    }
    return switch (status) {
      case PENDING -> "pending";
      case DRAFT -> "draft";
      case PROCESSED -> "processed";
      case REJECTED -> "rejected";
      case CANCELLED -> "cancelled";
    };
  }

  private String formatDt(LocalDateTime dt) {
    return dt != null ? dt.format(VI_DT) : null;
  }
}
