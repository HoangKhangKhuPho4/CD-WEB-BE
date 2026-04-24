package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.CartDto;
import com.cdweb.be.service.CartService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cart")
@PreAuthorize("isAuthenticated()")
public class CartController {

  @Autowired private CartService cartService;

  /** GET /api/cart — Lấy giỏ hàng của user hiện tại */
  @GetMapping
  public ResponseEntity<ApiResponse<CartDto.CartResponse>> getCart() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    CartDto.CartResponse cart = cartService.getCart(username);
    return ResponseEntity.ok(ApiResponse.success("Cart retrieved successfully", cart));
  }

  /** POST /api/cart — Thêm sản phẩm vào giỏ; nếu variant đã có thì cộng thêm số lượng */
  @PostMapping
  public ResponseEntity<ApiResponse<CartDto.CartResponse>> addItem(
      @Valid @RequestBody CartDto.AddItemRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    CartDto.CartResponse cart = cartService.addItem(username, request);
    return ResponseEntity.ok(ApiResponse.success("Item added to cart successfully", cart));
  }

  /** PUT /api/cart/{id} — Cập nhật số lượng của một cart item */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<CartDto.CartResponse>> updateItem(
      @PathVariable Integer id, @Valid @RequestBody CartDto.UpdateItemRequest request) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    CartDto.CartResponse cart = cartService.updateItem(username, id, request);
    return ResponseEntity.ok(ApiResponse.success("Cart item updated successfully", cart));
  }

  /** DELETE /api/cart/{id} — Xoá một cart item khỏi giỏ */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<CartDto.CartResponse>> removeItem(@PathVariable Integer id) {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    CartDto.CartResponse cart = cartService.removeItem(username, id);
    return ResponseEntity.ok(ApiResponse.success("Item removed from cart successfully", cart));
  }

  /** DELETE /api/cart — Xoá toàn bộ giỏ hàng */
  @DeleteMapping
  public ResponseEntity<ApiResponse<Void>> clearCart() {
    String username = SecurityContextHolder.getContext().getAuthentication().getName();
    cartService.clearCart(username);
    return ResponseEntity.ok(ApiResponse.success("Cart cleared successfully", null));
  }
}
