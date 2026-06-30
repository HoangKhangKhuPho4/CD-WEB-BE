package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.PurchaseOrderDto;
import com.cdweb.be.service.PurchaseOrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/suppliers")
@RequiredArgsConstructor
public class AdminSupplierController {

  private final PurchaseOrderService purchaseOrderService;

  @GetMapping
  @PreAuthorize("hasAnyAuthority('PRODUCT_MANAGE', 'STOCK_IMPORT', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<List<PurchaseOrderDto.SupplierResponse>>> list() {
    return ResponseEntity.ok(
        ApiResponse.success("Danh sách nhà cung cấp", purchaseOrderService.listSuppliers()));
  }
}
