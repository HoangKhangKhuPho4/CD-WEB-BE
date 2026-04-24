package com.cdweb.be.service.impl;

import com.cdweb.be.dto.ImeiRequest;
import com.cdweb.be.dto.ImportStockItemDto;
import com.cdweb.be.dto.ImportStockRequest;
import com.cdweb.be.dto.InventoryResponseDto;
import com.cdweb.be.dto.InventoryStatDto;
import com.cdweb.be.dto.ReturnStockRequest;
import com.cdweb.be.dto.VariantAutocompleteDto;
import com.cdweb.be.entity.Inventory;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.InventoryRepository;
import com.cdweb.be.repository.ProductItemRepository;
import com.cdweb.be.repository.ProductVariantRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.service.InventoryService;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
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

  private final ProductVariantRepository productVariantRepository;
  private final ProductItemRepository productItemRepository;
  private final InventoryRepository inventoryRepository;
  private final UserRepository userRepository;

  @Override
  @Transactional
  public void importStock(ImportStockRequest request) {
    User currentUser = getCurrentUser();
    for (ImportStockItemDto itemDto : request.getItems()) {
      ProductVariant variant =
          productVariantRepository
              .findById(itemDto.getVariantId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "ProductVariant", "id", itemDto.getVariantId().toString()));

      // Tăng số lượng stock quantity của variant
      variant.setStockQuantity(variant.getStockQuantity() + itemDto.getQuantity());
      productVariantRepository.save(variant);

      // Lưu lịch sử
      Inventory history = new Inventory();
      history.setTransactionType(Inventory.TransactionType.IMPORT);
      history.setQuantity(itemDto.getQuantity());
      history.setVariant(variant);
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
    User currentUser = getCurrentUser();
    ProductItem productItem =
        productItemRepository
            .findByImeiOrSerialNumber(request.getImei(), request.getImei())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("ProductItem", "IMEI/Serial", request.getImei()));

    boolean wasDefective = Boolean.TRUE.equals(request.getIsDefective());
    if (wasDefective) {
      productItem.setStatus(ProductItem.ProductItemStatus.DEFECTIVE);
      productItem.setCondition(ProductItem.ProductItemCondition.DAMAGED);
    } else {
      productItem.setStatus(ProductItem.ProductItemStatus.AVAILABLE);
      productItem.setCondition(ProductItem.ProductItemCondition.NEW);

      // Trả lại kho -> cộng lại số lượng cho variant
      ProductVariant variant = productItem.getVariant();
      variant.setStockQuantity(variant.getStockQuantity() + 1);
      productVariantRepository.save(variant);
    }

    productItem.setNotes(request.getReason());
    productItemRepository.save(productItem);

    // Lưu lịch sử
    Inventory history = new Inventory();
    history.setTransactionType(
        wasDefective ? Inventory.TransactionType.ADJUSTMENT : Inventory.TransactionType.RETURN);
    history.setQuantity(1);
    history.setVariant(productItem.getVariant());
    history.setProductItem(productItem);
    history.setReason("Hàng trả lại: " + request.getReason());
    history.setUser(currentUser);
    inventoryRepository.save(history);
  }

  @Override
  public List<InventoryStatDto> getInventoryStats(int lowStockThreshold) {
    List<ProductVariant> variants =
        productVariantRepository.findAll().stream()
            .filter(
                v ->
                    v.getStockQuantity() <= lowStockThreshold
                        && Boolean.TRUE.equals(v.getIsActive()))
            .collect(Collectors.toList());

    return variants.stream()
        .map(
            v ->
                InventoryStatDto.builder()
                    .variantId(v.getId())
                    .variantName(v.getVariantName())
                    .skuCode(v.getSkuCode())
                    .stockQuantity(v.getStockQuantity())
                    .status("LOW_STOCK")
                    .build())
        .collect(Collectors.toList());
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
  public List<InventoryResponseDto> getInventoryTransactions() {
    return inventoryRepository.findAllByOrderByCreatedAtDesc().stream()
        .map(
            i ->
                InventoryResponseDto.builder()
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
                    .userName(i.getUser().getName())
                    .build())
        .collect(Collectors.toList());
  }

  private User getCurrentUser() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    return userRepository
        .findByUsername(username)
        .orElseThrow(() -> new IllegalArgumentException("User not found: " + username));
  }
}
