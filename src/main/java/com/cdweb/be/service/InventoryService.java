package com.cdweb.be.service;

import com.cdweb.be.dto.ImeiRequest;
import com.cdweb.be.dto.ImportStockRequest;
import com.cdweb.be.dto.InventoryStatDto;
import com.cdweb.be.dto.ReturnStockRequest;
import com.cdweb.be.dto.VariantAutocompleteDto;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

public interface InventoryService {
  void importStock(ImportStockRequest request);

  void returnStock(ReturnStockRequest request);

  List<InventoryStatDto> getInventoryStats(int lowStockThreshold);

  void addImeiToProduct(ImeiRequest request);

  List<VariantAutocompleteDto> searchVariants(String keyword);

  void importImeiFromExcel(MultipartFile file);

  List<com.cdweb.be.dto.InventoryResponseDto> getInventoryTransactions();

  List<com.cdweb.be.dto.ProductItemListDto> listProductItems(String keyword);

  Page<com.cdweb.be.dto.ProductItemListDto> listProductItems(
      String keyword, Pageable pageable);
}
