package com.cdweb.be.service.impl;

import com.cdweb.be.dto.ImeiDto;
import com.cdweb.be.entity.Inventory;
import com.cdweb.be.entity.Order;
import com.cdweb.be.entity.OrderDetail;
import com.cdweb.be.entity.OrderItem;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.entity.ProductItem.ProductItemCondition;
import com.cdweb.be.entity.ProductItem.ProductItemStatus;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.InventoryRepository;
import com.cdweb.be.repository.OrderDetailRepository;
import com.cdweb.be.repository.OrderItemRepository;
import com.cdweb.be.repository.ProductItemRepository;
import com.cdweb.be.repository.ProductItemSpecification;
import com.cdweb.be.repository.ProductVariantRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.service.ImeiService;
import com.cdweb.be.util.ProductItemStatusTransitions;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class ImeiServiceImpl implements ImeiService {

  private static final Pattern IMEI_PATTERN = Pattern.compile("^[A-Za-z0-9]{8,20}$");

  private final ProductItemRepository productItemRepository;
  private final ProductVariantRepository productVariantRepository;
  private final OrderItemRepository orderItemRepository;
  private final OrderDetailRepository orderDetailRepository;
  private final InventoryRepository inventoryRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional(readOnly = true)
  public ImeiDto.StatsResponse getStats() {
    return ImeiDto.StatsResponse.builder()
        .total(productItemRepository.count())
        .available(productItemRepository.countByStatus(ProductItemStatus.AVAILABLE))
        .reserved(productItemRepository.countByStatus(ProductItemStatus.RESERVED))
        .sold(productItemRepository.countByStatus(ProductItemStatus.SOLD))
        .inRepair(productItemRepository.countByStatus(ProductItemStatus.IN_REPAIR))
        .defective(productItemRepository.countByStatus(ProductItemStatus.DEFECTIVE))
        .returned(productItemRepository.countByStatus(ProductItemStatus.RETURNED))
        .linkedToOrders(productItemRepository.countDistinctLinkedToOrders())
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ImeiDto.ListItem> list(
      String keyword,
      ProductItemStatus status,
      Integer variantId,
      String orderCode,
      LocalDate fromDate,
      LocalDate toDate,
      Pageable pageable) {
    Specification<ProductItem> spec =
        ProductItemSpecification.adminFilter(keyword, status, variantId, orderCode, fromDate, toDate);
    return productItemRepository.findAll(spec, pageable).map(this::toListItem);
  }

  @Override
  @Transactional(readOnly = true)
  public ImeiDto.DetailResponse getById(Integer id) {
    ProductItem item =
        productItemRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ProductItem", "id", id));
    return toDetail(item);
  }

  @Override
  @Transactional(readOnly = true)
  public ImeiDto.DetailResponse lookupByCode(String code) {
    ProductItem item =
        productItemRepository
            .findByImeiOrSerialNumber(code, code)
            .orElseThrow(() -> new ResourceNotFoundException("ProductItem", "IMEI/Serial", code));
    return toDetail(item);
  }

  @Override
  @Transactional
  public void create(ImeiDto.CreateRequest request) {
    User currentUser = getCurrentUser();
    ProductVariant variant =
        productVariantRepository
            .findById(request.getVariantId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "ProductVariant", "id", request.getVariantId().toString()));

    List<String> normalized = normalizeImeis(request.getImeis());
    if (normalized.isEmpty()) {
      throw new BadRequestException("Danh sách IMEI không hợp lệ");
    }

    ImeiDto.ValidateRequest valReq = new ImeiDto.ValidateRequest();
    valReq.setVariantId(request.getVariantId());
    valReq.setImeis(normalized);
    ImeiDto.ValidateResponse validation = validate(valReq);
    if (!validation.isAllValid()) {
      String errors =
          validation.getResults().stream()
              .filter(r -> !r.isValid())
              .map(r -> r.getImei() + ": " + r.getMessage())
              .collect(Collectors.joining("; "));
      throw new BadRequestException("IMEI không hợp lệ: " + errors);
    }

    List<ProductItem> items = new ArrayList<>();
    for (String imei : normalized) {
      ProductItem item = new ProductItem();
      item.setImei(imei);
      item.setSerialNumber(imei);
      item.setVariant(variant);
      item.setBatchNumber(request.getBatchNumber());
      item.setManufactureDate(LocalDate.now());
      item.setStatus(ProductItemStatus.AVAILABLE);
      item.setCondition(ProductItemCondition.NEW);
      item.setNotes(request.getNote());
      if (request.getImei2() != null && !request.getImei2().isBlank()) {
        item.setImei2(request.getImei2().trim());
      }
      if (request.getMacAddress() != null && !request.getMacAddress().isBlank()) {
        item.setMacAddress(request.getMacAddress().trim());
      }
      items.add(item);
    }

    productItemRepository.saveAll(items);
    variant.setStockQuantity(variant.getStockQuantity() + items.size());
    productVariantRepository.save(variant);

    Inventory history = new Inventory();
    history.setTransactionType(Inventory.TransactionType.IMPORT);
    history.setQuantity(items.size());
    history.setVariant(variant);
    history.setReason(
        request.getNote() != null && !request.getNote().isEmpty()
            ? request.getNote()
            : "Nhập IMEI: " + (request.getBatchNumber() != null ? request.getBatchNumber() : ""));
    history.setUser(currentUser);
    inventoryRepository.save(history);
  }

  @Override
  @Transactional(readOnly = true)
  public ImeiDto.ValidateResponse validate(ImeiDto.ValidateRequest request) {
    List<ImeiDto.ValidateItemResult> results = new ArrayList<>();
    boolean allValid = true;

    for (String raw : request.getImeis()) {
      String imei = raw != null ? raw.trim() : "";
      ImeiDto.ValidateItemResult.ValidateItemResultBuilder builder =
          ImeiDto.ValidateItemResult.builder().imei(imei);

      if (imei.isEmpty()) {
        results.add(builder.valid(false).message("IMEI trống").build());
        allValid = false;
        continue;
      }
      if (!IMEI_PATTERN.matcher(imei).matches()) {
        results.add(
            builder
                .valid(false)
                .message("IMEI phải gồm 8–20 ký tự chữ hoặc số")
                .build());
        allValid = false;
        continue;
      }
      var existing = productItemRepository.findByImeiOrSerialNumber(imei, imei);
      if (existing.isPresent()
          && (request.getExcludeId() == null
              || !existing.get().getId().equals(request.getExcludeId()))) {
        results.add(builder.valid(false).message("IMEI đã tồn tại trong hệ thống").build());
        allValid = false;
        continue;
      }
      results.add(builder.valid(true).message("Hợp lệ").build());
    }

    return ImeiDto.ValidateResponse.builder().allValid(allValid).results(results).build();
  }

  @Override
  @Transactional
  public ImeiDto.ImportResult importFromExcel(MultipartFile file) {
    List<String> errors = new ArrayList<>();
    int imported = 0;
    int skipped = 0;

    try {
      Workbook workbook = WorkbookFactory.create(file.getInputStream());
      Sheet sheet = workbook.getSheetAt(0);
      Map<String, ProductVariant> variantCache = new HashMap<>();
      List<ProductItem> itemsToSave = new ArrayList<>();
      DataFormatter formatter = new DataFormatter();

      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) {
          skipped++;
          continue;
        }
        Cell skuCell = row.getCell(0);
        Cell imeiCell = row.getCell(1);
        if (skuCell == null || imeiCell == null) {
          skipped++;
          continue;
        }

        String skuCode = formatter.formatCellValue(skuCell).trim();
        String imei = formatter.formatCellValue(imeiCell).trim();
        if (skuCode.isEmpty() || imei.isEmpty()) {
          skipped++;
          continue;
        }

        int rowNum = i + 1;
        if (!IMEI_PATTERN.matcher(imei).matches()) {
          errors.add("Dòng " + rowNum + ": IMEI [" + imei + "] không đúng định dạng");
          continue;
        }

        try {
          ProductVariant variant =
              variantCache.computeIfAbsent(
                  skuCode,
                  k ->
                      productVariantRepository
                          .findBySkuCode(k)
                          .orElseThrow(
                              () ->
                                  new IllegalArgumentException(
                                      "Dòng "
                                          + rowNum
                                          + ": SKU ["
                                          + k
                                          + "] không tồn tại")));

          if (productItemRepository.findByImeiOrSerialNumber(imei, imei).isPresent()) {
            errors.add("Dòng " + rowNum + ": IMEI [" + imei + "] đã tồn tại");
            continue;
          }

          ProductItem item = new ProductItem();
          item.setImei(imei);
          item.setSerialNumber(imei);
          item.setVariant(variant);
          item.setManufactureDate(LocalDate.now());
          item.setStatus(ProductItemStatus.AVAILABLE);
          item.setCondition(ProductItemCondition.NEW);
          item.setNotes("Import từ Excel");
          itemsToSave.add(item);
        } catch (IllegalArgumentException e) {
          errors.add(e.getMessage());
        }
      }

      if (!itemsToSave.isEmpty()) {
        productItemRepository.saveAll(itemsToSave);
        imported = itemsToSave.size();
        User currentUser = getCurrentUser();

        Map<ProductVariant, Long> stockCounts =
            itemsToSave.stream()
                .collect(Collectors.groupingBy(ProductItem::getVariant, Collectors.counting()));

        for (Map.Entry<ProductVariant, Long> entry : stockCounts.entrySet()) {
          ProductVariant v = entry.getKey();
          int qty = entry.getValue().intValue();
          v.setStockQuantity(v.getStockQuantity() + qty);
          productVariantRepository.save(v);

          Inventory history = new Inventory();
          history.setTransactionType(Inventory.TransactionType.IMPORT);
          history.setQuantity(qty);
          history.setVariant(v);
          history.setReason("Import lô IMEI từ Excel");
          history.setUser(currentUser);
          inventoryRepository.save(history);
        }
      }

      workbook.close();
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(e.getMessage());
    } catch (Exception e) {
      throw new BadRequestException("Không đọc được file Excel: " + e.getMessage());
    }

    return ImeiDto.ImportResult.builder()
        .importedCount(imported)
        .skippedCount(skipped)
        .errors(errors)
        .build();
  }

  @Override
  @Transactional
  public ImeiDto.ListItem update(Integer id, ImeiDto.UpdateRequest request) {
    ProductItem item =
        productItemRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ProductItem", "id", id));

    if (request.getImei2() != null) {
      String imei2 = request.getImei2().trim();
      if (!imei2.isEmpty()) {
        productItemRepository
            .findByImeiOrSerialNumber(imei2, imei2)
            .filter(p -> !p.getId().equals(id))
            .ifPresent(
                p -> {
                  throw new BadRequestException("IMEI2 đã được sử dụng bởi thiết bị khác");
                });
        item.setImei2(imei2);
      } else {
        item.setImei2(null);
      }
    }
    if (request.getMacAddress() != null) {
      item.setMacAddress(request.getMacAddress().isBlank() ? null : request.getMacAddress().trim());
    }
    if (request.getBatchNumber() != null) {
      item.setBatchNumber(request.getBatchNumber().isBlank() ? null : request.getBatchNumber().trim());
    }
    if (request.getLocation() != null) {
      item.setLocation(request.getLocation().isBlank() ? null : request.getLocation().trim());
    }
    if (request.getNotes() != null) {
      item.setNotes(request.getNotes());
    }
    if (request.getCondition() != null) {
      item.setCondition(request.getCondition());
    }

    productItemRepository.save(item);
    return toListItem(item);
  }

  @Override
  @Transactional
  public ImeiDto.ListItem updateStatus(Integer id, ImeiDto.StatusUpdateRequest request) {
    ProductItem item =
        productItemRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ProductItem", "id", id));

    boolean force = Boolean.TRUE.equals(request.getForce());
    ProductItemStatusTransitions.assertTransition(item.getStatus(), request.getStatus(), force);

    item.setStatus(request.getStatus());
    if (request.getReason() != null && !request.getReason().isBlank()) {
      item.setNotes(request.getReason());
    }
    if (request.getStatus() == ProductItemStatus.SOLD && item.getSoldAt() == null) {
      item.setSoldAt(LocalDateTime.now());
    }
    if (request.getStatus() == ProductItemStatus.AVAILABLE) {
      item.setSoldAt(null);
    }

    productItemRepository.save(item);
    return toListItem(item);
  }

  @Override
  @Transactional
  public ImeiDto.BulkStatusResult bulkStatus(ImeiDto.BulkStatusRequest request) {
    int success = 0;
    int fail = 0;
    List<String> errors = new ArrayList<>();

    for (Integer id : request.getIds()) {
      try {
        ImeiDto.StatusUpdateRequest single = new ImeiDto.StatusUpdateRequest();
        single.setStatus(request.getStatus());
        single.setReason(request.getReason());
        single.setForce(request.getForce());
        updateStatus(id, single);
        success++;
      } catch (Exception e) {
        fail++;
        errors.add("ID " + id + ": " + e.getMessage());
      }
    }

    return new ImeiDto.BulkStatusResult(success, fail, errors);
  }

  @Override
  @Transactional
  public ImeiDto.ReleaseResponse releaseFromOrder(Integer id) {
    ProductItem item =
        productItemRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("ProductItem", "id", id));

    if (item.getStatus() != ProductItemStatus.RESERVED) {
      throw new BadRequestException(
          "Chỉ có thể giải phóng IMEI đang ở trạng thái RESERVED. Hiện tại: " + item.getStatus());
    }

    List<OrderItem> links = orderItemRepository.findByProductItemIdWithOrder(id);
    String orderCode = null;
    if (!links.isEmpty()) {
      Order order = links.get(0).getOrderDetail().getOrder();
      orderCode = order != null ? order.getOrderCode() : null;
      orderItemRepository.deleteAll(links);
    }

    ProductItemStatus prev = item.getStatus();
    item.setStatus(ProductItemStatus.AVAILABLE);
    item.setSoldAt(null);
    productItemRepository.save(item);

    return ImeiDto.ReleaseResponse.builder()
        .productItemId(id)
        .imei(item.getImei())
        .previousStatus(prev)
        .newStatus(ProductItemStatus.AVAILABLE)
        .orderCode(orderCode)
        .message("Đã giải phóng IMEI khỏi đơn hàng")
        .build();
  }

  @Override
  @Transactional
  public void returnStock(ImeiDto.ReturnRequest request) {
    User currentUser = getCurrentUser();
    ProductItem productItem =
        productItemRepository
            .findByImeiOrSerialNumber(request.getImei(), request.getImei())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "ProductItem", "IMEI/Serial", request.getImei()));

    ProductItemStatus current = productItem.getStatus();
    if (current == ProductItemStatus.AVAILABLE) {
      throw new BadRequestException("IMEI đã ở trong kho (AVAILABLE)");
    }
    if (current == ProductItemStatus.RESERVED) {
      throw new BadRequestException("IMEI đang được giữ cho đơn hàng (RESERVED). Hãy hủy đơn hoặc giải phóng trước.");
    }
    if (current == ProductItemStatus.DEFECTIVE) {
      throw new BadRequestException("IMEI đã được đánh dấu lỗi (DEFECTIVE)");
    }
    if (current != ProductItemStatus.SOLD
        && current != ProductItemStatus.RETURNED
        && current != ProductItemStatus.IN_REPAIR) {
      throw new BadRequestException(
          "Không thể trả kho từ trạng thái " + current + ". Cho phép: SOLD, RETURNED, IN_REPAIR");
    }

    boolean wasDefective = Boolean.TRUE.equals(request.getIsDefective());
    if (wasDefective) {
      productItem.setStatus(ProductItemStatus.DEFECTIVE);
      productItem.setCondition(ProductItemCondition.DAMAGED);
    } else {
      productItem.setStatus(ProductItemStatus.AVAILABLE);
      productItem.setCondition(ProductItemCondition.NEW);
      ProductVariant variant = productItem.getVariant();
      variant.setStockQuantity(variant.getStockQuantity() + 1);
      productVariantRepository.save(variant);
    }

    if (request.getReason() != null) {
      productItem.setNotes(request.getReason());
    }
    productItemRepository.save(productItem);

    Inventory history = new Inventory();
    history.setTransactionType(
        wasDefective ? Inventory.TransactionType.ADJUSTMENT : Inventory.TransactionType.RETURN);
    history.setQuantity(1);
    history.setVariant(productItem.getVariant());
    history.setProductItem(productItem);
    history.setReason("Hàng trả lại: " + (request.getReason() != null ? request.getReason() : ""));
    history.setUser(currentUser);
    inventoryRepository.save(history);
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] exportCsv(
      String keyword,
      ProductItemStatus status,
      Integer variantId,
      String orderCode,
      LocalDate fromDate,
      LocalDate toDate) {
    Specification<ProductItem> spec =
        ProductItemSpecification.adminFilter(keyword, status, variantId, orderCode, fromDate, toDate);
    List<ProductItem> items = productItemRepository.findAll(spec);

    StringBuilder sb = new StringBuilder();
    sb.append("id,imei,serialNumber,productName,variantName,skuCode,status,orderCode,createdAt\n");
    for (ProductItem pi : items) {
      ImeiDto.ListItem row = toListItem(pi);
      sb.append(row.getId()).append(",");
      sb.append(csvEscape(row.getImei())).append(",");
      sb.append(csvEscape(row.getSerialNumber())).append(",");
      sb.append(csvEscape(row.getProductName())).append(",");
      sb.append(csvEscape(row.getVariantName())).append(",");
      sb.append(csvEscape(row.getSkuCode())).append(",");
      sb.append(row.getStatus()).append(",");
      sb.append(csvEscape(row.getOrderCode())).append(",");
      sb.append(row.getCreatedAt()).append("\n");
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  @Transactional
  public void markOrderReturnedForInspection(Integer orderId) {
    List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
    for (OrderItem oi : orderItems) {
      ProductItem pi = oi.getProductItem();
      if (pi == null) {
        continue;
      }
      if (pi.getStatus() == ProductItemStatus.SOLD
          || pi.getStatus() == ProductItemStatus.RESERVED) {
        pi.setStatus(ProductItemStatus.RETURNED);
        productItemRepository.save(pi);
      }
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<com.cdweb.be.dto.PendingReturnItemDto> listPendingReturnItems(int limit) {
    int take = Math.max(1, Math.min(limit, 50));
    return orderItemRepository.findReturnedItemsPendingInspection(
            org.springframework.data.domain.PageRequest.of(0, take))
        .stream()
        .map(
            oi -> {
              ProductItem pi = oi.getProductItem();
              String productName = null;
              String sku = null;
              if (pi.getVariant() != null) {
                sku = pi.getVariant().getSkuCode();
                if (pi.getVariant().getProduct() != null) {
                  productName = pi.getVariant().getProduct().getName();
                }
              }
              return com.cdweb.be.dto.PendingReturnItemDto.builder()
                  .productItemId(pi.getId())
                  .imei(pi.getImei())
                  .serialNumber(pi.getSerialNumber())
                  .productName(productName)
                  .skuCode(sku)
                  .orderCode(
                      oi.getOrderDetail() != null && oi.getOrderDetail().getOrder() != null
                          ? oi.getOrderDetail().getOrder().getOrderCode()
                          : null)
                  .updatedAt(
                      pi.getUpdatedAt() != null
                          ? pi.getUpdatedAt()
                              .format(
                                  java.time.format.DateTimeFormatter.ofPattern(
                                      "dd/MM/yyyy HH:mm"))
                          : null)
                  .build();
            })
        .toList();
  }

  @Override
  @Transactional
  public void restoreOrderInventory(Integer orderId) {
    List<OrderDetail> details = orderDetailRepository.findByOrderId(orderId);
    for (OrderDetail detail : details) {
      ProductVariant variant = detail.getVariant();
      if (variant != null) {
        variant.setStockQuantity(variant.getStockQuantity() + detail.getQuantity());
        productVariantRepository.save(variant);
      }
    }

    List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
    for (OrderItem oi : orderItems) {
      ProductItem pi = oi.getProductItem();
      if (pi.getStatus() == ProductItemStatus.RESERVED) {
        pi.setStatus(ProductItemStatus.AVAILABLE);
        pi.setSoldAt(null);
        productItemRepository.save(pi);
      }
      orderItemRepository.delete(oi);
    }
  }

  @Override
  @Transactional
  public void activateWarrantyForOrder(Integer orderId) {
    List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
    LocalDate today = LocalDate.now();

    for (OrderItem oi : orderItems) {
      ProductItem productItem = oi.getProductItem();
      if (productItem.getStatus() == ProductItemStatus.RESERVED
          || productItem.getStatus() == ProductItemStatus.AVAILABLE) {
        productItem.setStatus(ProductItemStatus.SOLD);
        productItem.setSoldAt(LocalDateTime.now());

        if (productItem.getWarrantyStartDate() == null) {
          productItem.setWarrantyStartDate(today);
          OrderDetail od = oi.getOrderDetail();
          if (od != null && od.getWarrantyMonths() != null && od.getWarrantyMonths() > 0) {
            productItem.setWarrantyMonths(od.getWarrantyMonths());
          }
        }
        productItemRepository.save(productItem);
      }
    }
  }

  // ── Mappers ───────────────────────────────────────────────────────────────

  private ImeiDto.ListItem toListItem(ProductItem pi) {
    String orderCode = null;
    Integer orderId = null;
    List<OrderItem> links = orderItemRepository.findByProductItemIdWithOrder(pi.getId());
    if (!links.isEmpty() && links.get(0).getOrderDetail().getOrder() != null) {
      Order order = links.get(0).getOrderDetail().getOrder();
      orderCode = order.getOrderCode();
      orderId = order.getId();
    }

    return ImeiDto.ListItem.builder()
        .id(pi.getId())
        .imei(pi.getImei())
        .serialNumber(pi.getSerialNumber())
        .imei2(pi.getImei2())
        .productName(pi.getVariant().getProduct().getName())
        .variantName(pi.getVariant().getVariantName())
        .skuCode(pi.getVariant().getSkuCode())
        .variantId(pi.getVariant().getId())
        .status(pi.getStatus())
        .condition(pi.getCondition())
        .orderCode(orderCode)
        .orderId(orderId)
        .batchNumber(pi.getBatchNumber())
        .location(pi.getLocation())
        .createdAt(pi.getCreatedAt())
        .warrantyStartDate(pi.getWarrantyStartDate())
        .warrantyMonths(pi.getWarrantyMonths())
        .build();
  }

  private ImeiDto.DetailResponse toDetail(ProductItem pi) {
    ImeiDto.ListItem base = toListItem(pi);
    ImeiDto.OrderLink orderLink = null;

    List<OrderItem> links = orderItemRepository.findByProductItemIdWithOrder(pi.getId());
    if (!links.isEmpty()) {
      OrderDetail od = links.get(0).getOrderDetail();
      Order order = od.getOrder();
      orderLink =
          ImeiDto.OrderLink.builder()
              .orderId(order != null ? order.getId() : null)
              .orderCode(order != null ? order.getOrderCode() : null)
              .orderStatus(order != null ? order.getStatus().name() : null)
              .orderDetailId(od.getId())
              .quantity(od.getQuantity())
              .build();
    }

    ImeiDto.WarrantyInfo warranty = buildWarrantyInfo(pi);

    List<Inventory> allTx = inventoryRepository.findAllByOrderByCreatedAtDesc();
    List<ImeiDto.TransactionItem> transactions =
        allTx.stream()
            .filter(
                t ->
                    t.getProductItem() != null
                        && t.getProductItem().getId().equals(pi.getId()))
            .map(
                t ->
                    ImeiDto.TransactionItem.builder()
                        .id(t.getId())
                        .transactionType(t.getTransactionType().name())
                        .quantity(t.getQuantity())
                        .reason(t.getReason())
                        .createdAt(t.getCreatedAt())
                        .build())
            .toList();

    return ImeiDto.DetailResponse.builder()
        .id(pi.getId())
        .imei(pi.getImei())
        .serialNumber(pi.getSerialNumber())
        .imei2(pi.getImei2())
        .macAddress(pi.getMacAddress())
        .batchNumber(pi.getBatchNumber())
        .location(pi.getLocation())
        .notes(pi.getNotes())
        .status(pi.getStatus())
        .condition(pi.getCondition())
        .variantId(base.getVariantId())
        .variantName(base.getVariantName())
        .skuCode(base.getSkuCode())
        .productName(base.getProductName())
        .productId(pi.getVariant().getProduct().getId())
        .order(orderLink)
        .warranty(warranty)
        .manufactureDate(pi.getManufactureDate())
        .createdAt(pi.getCreatedAt())
        .updatedAt(pi.getUpdatedAt())
        .soldAt(pi.getSoldAt())
        .transactions(transactions)
        .build();
  }

  private ImeiDto.WarrantyInfo buildWarrantyInfo(ProductItem pi) {
    if (pi.getWarrantyStartDate() == null) {
      if (pi.getStatus() == ProductItemStatus.AVAILABLE) {
        return ImeiDto.WarrantyInfo.builder()
            .active(false)
            .message("Thiết bị chính hãng, bảo hành chưa kích hoạt")
            .build();
      }
      return ImeiDto.WarrantyInfo.builder()
          .active(false)
          .message("Chưa xác định thời hạn bảo hành")
          .build();
    }
    int months = pi.getWarrantyMonths() != null ? pi.getWarrantyMonths() : 12;
    LocalDate end = pi.getWarrantyStartDate().plusMonths(months);
    boolean active = !LocalDate.now().isAfter(end);
    return ImeiDto.WarrantyInfo.builder()
        .startDate(pi.getWarrantyStartDate())
        .months(months)
        .active(active)
        .message(active ? "Bảo hành còn hiệu lực" : "Bảo hành đã hết hạn")
        .build();
  }

  private List<String> normalizeImeis(List<String> imeis) {
    if (imeis == null) {
      return List.of();
    }
    return imeis.stream()
        .filter(s -> s != null && !s.isBlank())
        .map(String::trim)
        .distinct()
        .toList();
  }

  private String csvEscape(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new BadRequestException("User not found: " + username));
  }
}
