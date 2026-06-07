package com.cdweb.be.service.impl;

import com.cdweb.be.dto.AdjustStockRequest;
import com.cdweb.be.dto.ImeiRequest;
import com.cdweb.be.dto.ImportStockItemDto;
import com.cdweb.be.dto.ImportStockRequest;
import com.cdweb.be.dto.InventoryDto;
import com.cdweb.be.dto.InventoryResponseDto;
import com.cdweb.be.dto.InventoryStatDto;
import com.cdweb.be.dto.ProductItemListDto;
import com.cdweb.be.dto.ReturnQuantityRequest;
import com.cdweb.be.dto.ReturnStockRequest;
import com.cdweb.be.dto.VariantAutocompleteDto;
import com.cdweb.be.entity.Inventory;
import com.cdweb.be.entity.Inventory.TransactionType;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.entity.OrderItem;
import com.cdweb.be.repository.InventoryRepository;
import com.cdweb.be.repository.InventoryTransactionSpecification;
import com.cdweb.be.repository.OrderItemRepository;
import com.cdweb.be.repository.ProductItemRepository;
import com.cdweb.be.repository.ProductVariantRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.dto.ImeiDto;
import com.cdweb.be.service.ImeiService;
import com.cdweb.be.service.InventoryService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

  private final OrderItemRepository orderItemRepository;
  private final ProductVariantRepository productVariantRepository;
  private final ProductItemRepository productItemRepository;
  private final InventoryRepository inventoryRepository;
  private final UserRepository userRepository;
  private final ImeiService imeiService;

  @Override
  @Transactional
  public void importStock(ImportStockRequest request) {
    User currentUser = getCurrentUser();
    int batchRef = (int) ((System.nanoTime() / 1000) % Integer.MAX_VALUE);
    for (ImportStockItemDto itemDto : request.getItems()) {
      ProductVariant variant =
          productVariantRepository
              .findById(itemDto.getVariantId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "ProductVariant", "id", itemDto.getVariantId().toString()));

      variant.setStockQuantity(variant.getStockQuantity() + itemDto.getQuantity());
      productVariantRepository.save(variant);

      Inventory history = new Inventory();
      history.setTransactionType(Inventory.TransactionType.IMPORT);
      history.setQuantity(itemDto.getQuantity());
      history.setVariant(variant);
      history.setReferenceType("IMPORT_BATCH");
      history.setReferenceId(batchRef);
      history.setReason(
          request.getNote() != null && !request.getNote().isEmpty()
              ? request.getNote()
              : "Nhập hàng từ: " + request.getSupplier());
      history.setUser(currentUser);
      inventoryRepository.save(history);
    }
  }

  @Override
  @Transactional
  public void returnStock(ReturnStockRequest request) {
    ImeiDto.ReturnRequest r = new ImeiDto.ReturnRequest();
    r.setImei(request.getImei());
    r.setReason(request.getReason());
    r.setIsDefective(request.getIsDefective());
    imeiService.returnStock(r);
  }

  @Override
  @Transactional
  public void returnQuantity(ReturnQuantityRequest request) {
    User currentUser = getCurrentUser();
    ProductVariant variant =
        productVariantRepository
            .findById(request.getVariantId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "ProductVariant", "id", request.getVariantId().toString()));

    variant.setStockQuantity(variant.getStockQuantity() + request.getQuantity());
    productVariantRepository.save(variant);

    String reason =
        request.getReason() != null && !request.getReason().isEmpty()
            ? request.getReason()
            : Boolean.TRUE.equals(request.getIsDefective())
                ? "Trả hàng lỗi (theo số lượng)"
                : "Trả hàng (theo số lượng)";

    Inventory history = new Inventory();
    history.setTransactionType(TransactionType.RETURN);
    history.setQuantity(request.getQuantity());
    history.setVariant(variant);
    history.setReason(reason);
    history.setUser(currentUser);
    inventoryRepository.save(history);
  }

  @Override
  @Transactional
  public void adjustStock(AdjustStockRequest request) {
    User currentUser = getCurrentUser();
    ProductVariant variant =
        productVariantRepository
            .findById(request.getVariantId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "ProductVariant", "id", request.getVariantId().toString()));

    int current = variant.getStockQuantity() != null ? variant.getStockQuantity() : 0;
    int qty = request.getQuantity();
    boolean decrease = "DECREASE".equalsIgnoreCase(request.getDirection());

    if (decrease && current < qty) {
      throw new BadRequestException(
          "Tồn kho không đủ để trừ. Hiện có: " + current + ", yêu cầu trừ: " + qty);
    }

    variant.setStockQuantity(decrease ? current - qty : current + qty);
    productVariantRepository.save(variant);

    String defaultReason = decrease ? "Điều chỉnh giảm tồn kho" : "Điều chỉnh tăng tồn kho";
    Inventory history = new Inventory();
    history.setTransactionType(TransactionType.ADJUSTMENT);
    history.setQuantity(qty);
    history.setVariant(variant);
    history.setReason(
        request.getReason() != null && !request.getReason().isEmpty()
            ? request.getReason()
            : defaultReason);
    history.setUser(currentUser);
    inventoryRepository.save(history);
  }

  @Override
  @Transactional(readOnly = true)
  public InventoryDto.ValidateImportResponse validateImport(ImportStockRequest request) {
    List<InventoryDto.ValidateImportItemResult> results = new ArrayList<>();
    boolean allValid = true;

    for (ImportStockItemDto item : request.getItems()) {
      if (item.getVariantId() == null) {
        results.add(
            InventoryDto.ValidateImportItemResult.builder()
                .variantId(null)
                .valid(false)
                .message("Variant ID không được để trống")
                .build());
        allValid = false;
        continue;
      }
      if (item.getQuantity() == null || item.getQuantity() < 1) {
        results.add(
            InventoryDto.ValidateImportItemResult.builder()
                .variantId(item.getVariantId())
                .requestedQuantity(item.getQuantity())
                .valid(false)
                .message("Số lượng nhập phải lớn hơn 0")
                .build());
        allValid = false;
        continue;
      }

      var opt = productVariantRepository.findById(item.getVariantId());
      if (opt.isEmpty()) {
        results.add(
            InventoryDto.ValidateImportItemResult.builder()
                .variantId(item.getVariantId())
                .requestedQuantity(item.getQuantity())
                .valid(false)
                .message("Variant không tồn tại")
                .build());
        allValid = false;
        continue;
      }

      ProductVariant v = opt.get();
      results.add(
          InventoryDto.ValidateImportItemResult.builder()
              .variantId(v.getId())
              .skuCode(v.getSkuCode())
              .productName(v.getProduct() != null ? v.getProduct().getName() : null)
              .variantName(v.getVariantName())
              .currentStock(v.getStockQuantity())
              .requestedQuantity(item.getQuantity())
              .valid(true)
              .message("OK")
              .build());
    }

    return InventoryDto.ValidateImportResponse.builder().allValid(allValid).results(results).build();
  }

  @Override
  public List<InventoryStatDto> getInventoryStats(int lowStockThreshold) {
    return productVariantRepository.findAllActiveWithProduct().stream()
        .map(v -> toInventoryStat(v, lowStockThreshold))
        .collect(Collectors.toList());
  }

  private InventoryStatDto toInventoryStat(ProductVariant v, int lowStockThreshold) {
    int qty = v.getStockQuantity() != null ? v.getStockQuantity() : 0;
    int threshold =
        v.getLowStockThreshold() != null ? v.getLowStockThreshold() : lowStockThreshold;
    double unitPrice = v.getPrice() != null ? v.getPrice().doubleValue() : 0.0;
    String productName =
        v.getProduct() != null ? v.getProduct().getName() : null;
    String status;
    if (qty <= 0) {
      status = "OUT_OF_STOCK";
    } else if (qty <= threshold) {
      status = "LOW_STOCK";
    } else {
      status = "IN_STOCK";
    }
    return InventoryStatDto.builder()
        .variantId(v.getId())
        .productName(productName)
        .variantName(v.getVariantName())
        .skuCode(v.getSkuCode())
        .stockQuantity(qty)
        .lowStockThreshold(threshold)
        .unitPrice(unitPrice)
        .stockValue(Math.round(unitPrice * qty * 100.0) / 100.0)
        .status(status)
        .build();
  }

  @Override
  @Transactional
  public void addImeiToProduct(ImeiRequest request) {
    User currentUser = getCurrentUser();
    ProductVariant variant =
        productVariantRepository
            .findById(request.getVariantId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "ProductVariant", "id", request.getVariantId().toString()));

    List<ProductItem> itemsToSave = new ArrayList<>();

    for (String imei : request.getImeis()) {
      productItemRepository
          .findByImeiOrSerialNumber(imei, imei)
          .ifPresent(
              p -> {
                throw new IllegalStateException("IMEI " + imei + " đã tồn tại trong hệ thống");
              });

      ProductItem item = new ProductItem();
      item.setImei(imei);
      item.setSerialNumber(imei);
      item.setVariant(variant);
      item.setBatchNumber(request.getBatchNumber());
      item.setManufactureDate(LocalDate.now());
      item.setStatus(ProductItem.ProductItemStatus.AVAILABLE);
      item.setCondition(ProductItem.ProductItemCondition.NEW);
      item.setNotes(request.getNote());
      itemsToSave.add(item);
    }

    productItemRepository.saveAll(itemsToSave);

    // Cộng số lượng IMEI mới vào tồn kho của variant
    variant.setStockQuantity(variant.getStockQuantity() + itemsToSave.size());
    productVariantRepository.save(variant);

    // Lưu lịch sử
    Inventory history = new Inventory();
    history.setTransactionType(Inventory.TransactionType.IMPORT);
    history.setQuantity(itemsToSave.size());
    history.setVariant(variant);
    history.setReason(
        request.getNote() != null && !request.getNote().isEmpty()
            ? request.getNote()
            : "Nhập hàng theo lô IMEI: " + request.getBatchNumber());
    history.setUser(currentUser);
    inventoryRepository.save(history);
  }

  @Override
  public List<VariantAutocompleteDto> searchVariants(String keyword) {
    if (keyword == null || keyword.trim().isEmpty()) {
      return new ArrayList<>();
    }
    List<ProductVariant> variants = productVariantRepository.searchVariants(keyword.trim());
    return variants.stream()
        .map(
            v ->
                VariantAutocompleteDto.builder()
                    .id(v.getId())
                    .skuCode(v.getSkuCode())
                    .variantName(v.getVariantName())
                    .productName(v.getProduct().getName())
                    .build())
        .collect(Collectors.toList());
  }

  @Override
  @Transactional
  public void importImeiFromExcel(MultipartFile file) {
    try {
      Workbook workbook = WorkbookFactory.create(file.getInputStream());
      Sheet sheet = workbook.getSheetAt(0);

      List<ProductItem> itemsToSave = new ArrayList<>();
      Map<String, ProductVariant> variantCache = new HashMap<>();

      // Bỏ qua dòng tiêu đề, đọc từ dòng thứ 2 (index = 1)
      for (int i = 1; i <= sheet.getLastRowNum(); i++) {
        Row row = sheet.getRow(i);
        if (row == null) continue;

        Cell skuCell = row.getCell(0);
        Cell imeiCell = row.getCell(1);

        if (skuCell == null || imeiCell == null) continue;

        // Lấy data dạng text
        DataFormatter dataFormatter = new DataFormatter();
        String skuCode = dataFormatter.formatCellValue(skuCell).trim();
        String imei = dataFormatter.formatCellValue(imeiCell).trim();

        if (skuCode.isEmpty() || imei.isEmpty()) continue;

        final int rowIndex = i;
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
                                        + (rowIndex + 1)
                                        + ": SKU ["
                                        + k
                                        + "] không tồn tại trong hệ thống.")));

        // Kiểm tra IMEI đã tồn tại chưa
        if (productItemRepository.findByImeiOrSerialNumber(imei, imei).isPresent()) {
          throw new IllegalArgumentException(
              "Dòng " + (rowIndex + 1) + ": IMEI [" + imei + "] đã tồn tại trong hệ thống.");
        }

        ProductItem item = new ProductItem();
        item.setImei(imei);
        item.setSerialNumber(imei);
        item.setVariant(variant);
        item.setManufactureDate(LocalDate.now());
        item.setStatus(ProductItem.ProductItemStatus.AVAILABLE);
        item.setCondition(ProductItem.ProductItemCondition.NEW);
        item.setNotes("Import từ Excel");
        itemsToSave.add(item);
      }

      productItemRepository.saveAll(itemsToSave);

      User currentUser = getCurrentUser();

      // Cập nhật số lượng tồn kho theo số sản phẩm vừa thêm
      Map<ProductVariant, Long> stockCounts =
          itemsToSave.stream()
              .collect(Collectors.groupingBy(ProductItem::getVariant, Collectors.counting()));

      for (Map.Entry<ProductVariant, Long> entry : stockCounts.entrySet()) {
        ProductVariant v = entry.getKey();
        int qty = entry.getValue().intValue();
        v.setStockQuantity(v.getStockQuantity() + qty);
        productVariantRepository.save(v);

        // Lưu lịch sử
        Inventory history = new Inventory();
        history.setTransactionType(Inventory.TransactionType.IMPORT);
        history.setQuantity(qty);
        history.setVariant(v);
        history.setReason("Import lô IMEI từ Excel");
        history.setUser(currentUser);
        inventoryRepository.save(history);
      }

      workbook.close();

    } catch (IllegalArgumentException e) {
      throw e; // Ném thẳng các lỗi validation
    } catch (Exception e) {
      throw new RuntimeException(
          "Lỗi khi đọc file Excel, vui lòng kiểm tra lại định dạng file. (" + e.getMessage() + ")",
          e);
    }
  }

  @Override
  @Transactional(readOnly = true)
  public List<InventoryResponseDto> getInventoryTransactions(
      Integer variantId,
      TransactionType transactionType,
      String referenceType,
      Integer referenceId,
      LocalDate fromDate,
      LocalDate toDate) {
    Specification<Inventory> spec =
        InventoryTransactionSpecification.adminFilter(
            variantId, transactionType, referenceType, referenceId, fromDate, toDate);
    return inventoryRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "createdAt")).stream()
        .map(this::toResponseDto)
        .collect(Collectors.toList());
  }

  @Override
  @Transactional(readOnly = true)
  public Page<InventoryResponseDto> getInventoryTransactionsPaged(
      Integer variantId,
      TransactionType transactionType,
      String referenceType,
      Integer referenceId,
      LocalDate fromDate,
      LocalDate toDate,
      Pageable pageable) {
    Specification<Inventory> spec =
        InventoryTransactionSpecification.adminFilter(
            variantId, transactionType, referenceType, referenceId, fromDate, toDate);
    return inventoryRepository.findAll(spec, pageable).map(this::toResponseDto);
  }

  @Override
  @Transactional(readOnly = true)
  public InventoryResponseDto getTransactionById(Integer id) {
    Inventory tx =
        inventoryRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory", "id", id.toString()));
    return toResponseDto(tx);
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] exportStatsCsv(int lowStockThreshold) {
    List<InventoryStatDto> stats = getInventoryStats(lowStockThreshold);
    StringBuilder sb = new StringBuilder();
    sb.append(
        "variantId,productName,variantName,skuCode,stockQuantity,lowStockThreshold,unitPrice,stockValue,status\n");
    for (InventoryStatDto row : stats) {
      sb.append(row.getVariantId()).append(",");
      sb.append(csvEscape(row.getProductName())).append(",");
      sb.append(csvEscape(row.getVariantName())).append(",");
      sb.append(csvEscape(row.getSkuCode())).append(",");
      sb.append(row.getStockQuantity()).append(",");
      sb.append(row.getLowStockThreshold()).append(",");
      sb.append(row.getUnitPrice()).append(",");
      sb.append(row.getStockValue()).append(",");
      sb.append(row.getStatus()).append("\n");
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  @Override
  @Transactional(readOnly = true)
  public byte[] exportTransactionsCsv(
      Integer variantId,
      TransactionType transactionType,
      String referenceType,
      Integer referenceId,
      LocalDate fromDate,
      LocalDate toDate) {
    List<InventoryResponseDto> rows =
        getInventoryTransactions(
            variantId, transactionType, referenceType, referenceId, fromDate, toDate);
    StringBuilder sb = new StringBuilder();
    sb.append(
        "id,transactionType,quantity,variantId,skuCode,variantName,reason,userName,createdAt,referenceType,referenceId\n");
    for (InventoryResponseDto row : rows) {
      sb.append(row.getId()).append(",");
      sb.append(row.getTransactionType()).append(",");
      sb.append(row.getQuantity()).append(",");
      sb.append(row.getVariantId()).append(",");
      sb.append(csvEscape(row.getSkuCode())).append(",");
      sb.append(csvEscape(row.getVariantName())).append(",");
      sb.append(csvEscape(row.getReason())).append(",");
      sb.append(csvEscape(row.getUserName())).append(",");
      sb.append(row.getCreatedAt()).append(",");
      sb.append(csvEscape(row.getReferenceType())).append(",");
      sb.append(row.getReferenceId() != null ? row.getReferenceId() : "").append("\n");
    }
    return sb.toString().getBytes(StandardCharsets.UTF_8);
  }

  private InventoryResponseDto toResponseDto(Inventory i) {
    return InventoryResponseDto.builder()
        .id(i.getId())
        .transactionType(i.getTransactionType().name())
        .quantity(i.getQuantity())
        .referenceType(i.getReferenceType())
        .referenceId(i.getReferenceId())
        .reason(i.getReason())
        .createdAt(i.getCreatedAt())
        .variantId(i.getVariant().getId())
        .variantName(i.getVariant().getVariantName())
        .skuCode(i.getVariant().getSkuCode())
        .productItemId(i.getProductItem() != null ? i.getProductItem().getId() : null)
        .imei(i.getProductItem() != null ? i.getProductItem().getImei() : null)
        .userId(i.getUser().getId())
        .userName(i.getUser().getFullName())
        .build();
  }

  private String csvEscape(String value) {
    if (value == null) {
      return "";
    }
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
      return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
  }

  @Override
  @Transactional(readOnly = true)
  public List<ProductItemListDto> listProductItems(String keyword) {
    return listProductItems(keyword, Pageable.unpaged()).getContent();
  }

  @Override
  @Transactional(readOnly = true)
  public Page<ProductItemListDto> listProductItems(String keyword, Pageable pageable) {
    String kw = keyword == null ? "" : keyword.trim();
    return productItemRepository
        .searchItems(kw, pageable)
        .map(
            pi -> {
              String orderCode = null;
              List<OrderItem> links = orderItemRepository.findByProductItemIdWithOrder(pi.getId());
              if (!links.isEmpty() && links.get(0).getOrderDetail().getOrder() != null) {
                orderCode = links.get(0).getOrderDetail().getOrder().getOrderCode();
              }
              return ProductItemListDto.builder()
                  .id(pi.getId())
                  .imei(pi.getImei())
                  .serialNumber(pi.getSerialNumber())
                  .productName(pi.getVariant().getProduct().getName())
                  .variantName(pi.getVariant().getVariantName())
                  .skuCode(pi.getVariant().getSkuCode())
                  .status(pi.getStatus())
                  .orderCode(orderCode)
                  .createdAt(pi.getCreatedAt())
                  .build();
            });
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
  }
}
