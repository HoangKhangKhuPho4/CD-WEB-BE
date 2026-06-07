package com.cdweb.be.service;

import com.cdweb.be.dto.AdjustStockRequest;
import com.cdweb.be.dto.ImeiRequest;
import com.cdweb.be.dto.ImportStockRequest;
import com.cdweb.be.dto.InventoryDto;
import com.cdweb.be.dto.InventoryResponseDto;
import com.cdweb.be.dto.InventoryStatDto;
import com.cdweb.be.dto.ProductItemListDto;
import com.cdweb.be.dto.ReturnQuantityRequest;
import com.cdweb.be.dto.ReturnStockRequest;
import com.cdweb.be.dto.VariantAutocompleteDto;
import com.cdweb.be.entity.Inventory.TransactionType;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface InventoryService {
  void importStock(ImportStockRequest request);

  void returnStock(ReturnStockRequest request);

  void returnQuantity(ReturnQuantityRequest request);

  void adjustStock(AdjustStockRequest request);

  InventoryDto.ValidateImportResponse validateImport(ImportStockRequest request);

  List<InventoryStatDto> getInventoryStats(int lowStockThreshold);

  void addImeiToProduct(ImeiRequest request);

  List<VariantAutocompleteDto> searchVariants(String keyword);

  void importImeiFromExcel(MultipartFile file);

  List<InventoryResponseDto> getInventoryTransactions(
      Integer variantId,
      TransactionType transactionType,
      String referenceType,
      Integer referenceId,
      LocalDate fromDate,
      LocalDate toDate);

  Page<InventoryResponseDto> getInventoryTransactionsPaged(
      Integer variantId,
      TransactionType transactionType,
      String referenceType,
      Integer referenceId,
      LocalDate fromDate,
      LocalDate toDate,
      Pageable pageable);

  InventoryResponseDto getTransactionById(Integer id);

  byte[] exportStatsCsv(int lowStockThreshold);

  byte[] exportTransactionsCsv(
      Integer variantId,
      TransactionType transactionType,
      String referenceType,
      Integer referenceId,
      LocalDate fromDate,
      LocalDate toDate);

  List<ProductItemListDto> listProductItems(String keyword);

  Page<ProductItemListDto> listProductItems(String keyword, Pageable pageable);
}
