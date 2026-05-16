package com.cdweb.be.service;

import com.cdweb.be.dto.CartDto;
import com.cdweb.be.entity.Cart;
import com.cdweb.be.entity.CartItem;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.CartItemRepository;
import com.cdweb.be.repository.CartRepository;
import com.cdweb.be.repository.ProductVariantRepository;
import com.cdweb.be.repository.UserRepository;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor; // Thêm lombok
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor // Sử dụng Constructor Injection (hết báo vàng Field Injection)
public class CartService {

  private final CartRepository cartRepository;
  private final CartItemRepository cartItemRepository;
  private final ProductVariantRepository productVariantRepository;
  private final UserRepository userRepository;

  @Value("${app.server.url:http://localhost:8080}")
  private String serverUrl;

  // ─── GET CART ────────────────────────────────────────────────────────────────
  public CartDto.CartResponse getCart(String username) {
    User user = findUser(username);
    Cart cart = getOrCreateCart(user);
    return mapToCartResponse(cart);
  }

  // ─── ADD ITEM ────────────────────────────────────────────────────────────────
  public CartDto.CartResponse addItem(String username, CartDto.AddItemRequest request) {
    User user = findUser(username);
    Cart cart = getOrCreateCart(user);

    ProductVariant variant =
            productVariantRepository
                    .findById(request.getVariantId())
                    .orElseThrow(
                            () ->
                                    new ResourceNotFoundException("ProductVariant", "id", request.getVariantId()));

    if (!Boolean.TRUE.equals(variant.getIsActive())) {
      throw new BadRequestException("Sản phẩm không còn kinh doanh");
    }

    // Đã khớp Long cho user.getId()
    var existingItem = cartItemRepository.findByUserIdAndVariantId(user.getId(), variant.getId());

    if (existingItem.isPresent()) {
      CartItem item = existingItem.get();
      int newQty = item.getQuantity() + request.getQuantity();
      validateStock(variant, newQty);
      item.setQuantity(newQty);
      item.setUnitPrice(variant.getPrice());
      cartItemRepository.save(item);
    } else {
      validateStock(variant, request.getQuantity());
      CartItem item = new CartItem();
      item.setCart(cart);
      item.setVariant(variant);
      item.setQuantity(request.getQuantity());
      item.setUnitPrice(variant.getPrice());
      cartItemRepository.save(item);
    }

    Cart updatedCart = cartRepository.findById(cart.getId()).orElse(cart);
    return mapToCartResponse(updatedCart);
  }

  // ─── UPDATE ITEM ─────────────────────────────────────────────────────────────
  public CartDto.CartResponse updateItem(
          String username, Integer cartItemId, CartDto.UpdateItemRequest request) {
    User user = findUser(username);
    // Hàm này đã được sửa tham số userId thành Long
    CartItem item = getCartItemWithOwnerCheck(cartItemId, user.getId());

    validateStock(item.getVariant(), request.getQuantity());
    item.setQuantity(request.getQuantity());
    item.setUnitPrice(item.getVariant().getPrice());
    cartItemRepository.save(item);

    Cart cart = cartRepository.findByUserId(user.getId()).orElseThrow();
    return mapToCartResponse(cart);
  }

  // ─── REMOVE ITEM ─────────────────────────────────────────────────────────────
  public CartDto.CartResponse removeItem(String username, Integer cartItemId) {
    User user = findUser(username);
    CartItem item = getCartItemWithOwnerCheck(cartItemId, user.getId());

    Cart cart = cartRepository.findByUserId(user.getId()).orElseThrow();
    cart.getCartItems().remove(item);
    cartItemRepository.delete(item);
    cartItemRepository.flush();

    return mapToCartResponse(cart);
  }

  // ─── CLEAR CART ──────────────────────────────────────────────────────────────
  public void clearCart(String username) {
    User user = findUser(username);
    // Repository cần nhận Long userId
    cartItemRepository.deleteAllByUserId(user.getId());
  }

  // ─── HELPERS ─────────────────────────────────────────────────────────────────
  private User findUser(String username) {
    return userRepository
            .findByUsername(username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));
  }

  private Cart getOrCreateCart(User user) {
    // Repository cần nhận Long userId
    return cartRepository
            .findByUserId(user.getId())
            .orElseGet(
                    () -> {
                      Cart cart = new Cart();
                      cart.setUser(user);
                      return cartRepository.save(cart);
                    });
  }

  // ĐÃ SỬA: Integer userId -> Long userId
  private CartItem getCartItemWithOwnerCheck(Integer cartItemId, Long userId) {
    CartItem item =
            cartItemRepository
                    .findById(cartItemId)
                    .orElseThrow(() -> new ResourceNotFoundException("CartItem", "id", cartItemId));
    if (!item.getCart().getUser().getId().equals(userId)) {
      throw new ResourceNotFoundException("CartItem", "id", cartItemId);
    }
    return item;
  }

  private void validateStock(ProductVariant variant, int requestedQty) {
    if (variant.getStockQuantity() != null && requestedQty > variant.getStockQuantity()) {
      throw new BadRequestException(
              "Không đủ hàng trong kho. Còn lại: "
                      + variant.getStockQuantity()
                      + ", yêu cầu: "
                      + requestedQty);
    }
  }

  // Các hàm mapToResponse giữ nguyên...
  private CartDto.CartResponse mapToCartResponse(Cart cart) {
    List<CartItem> items = cart.getCartItems();
    List<CartDto.CartItemResponse> itemResponses = new ArrayList<>();
    double totalAmount = 0.0;

    if (items != null) {
      for (CartItem item : items) {
        CartDto.CartItemResponse ir = mapToCartItemResponse(item);
        itemResponses.add(ir);
        if (ir.getSubtotal() != null) {
          totalAmount += ir.getSubtotal();
        }
      }
    }

    CartDto.CartResponse response = new CartDto.CartResponse();
    response.setId(cart.getId());
    response.setItems(itemResponses);
    response.setTotalItems(itemResponses.size());
    response.setTotalAmount(Math.round(totalAmount * 100.0) / 100.0);
    return response;
  }

  private CartDto.CartItemResponse mapToCartItemResponse(CartItem item) {
    CartDto.CartItemResponse ir = new CartDto.CartItemResponse();
    ir.setId(item.getId());
    ir.setQuantity(item.getQuantity());

    double unitPrice = 0.0;
    if (item.getUnitPrice() != null) {
      unitPrice = item.getUnitPrice().doubleValue();
    } else if (item.getVariant() != null && item.getVariant().getPrice() != null) {
      unitPrice = item.getVariant().getPrice().doubleValue();
    }
    ir.setUnitPrice(unitPrice);
    ir.setSubtotal(Math.round(unitPrice * item.getQuantity() * 100.0) / 100.0);

    if (item.getVariant() != null) {
      ir.setVariant(mapToCartVariantDto(item.getVariant()));
    }
    return ir;
  }

  private CartDto.CartVariantDto mapToCartVariantDto(ProductVariant variant) {
    CartDto.CartVariantDto vd = new CartDto.CartVariantDto();
    vd.setId(variant.getId());
    vd.setSkuCode(variant.getSkuCode());
    vd.setVariantName(variant.getVariantName());
    vd.setPrice(variant.getPrice() != null ? variant.getPrice().doubleValue() : null);
    vd.setOriginalPrice(
            variant.getOriginalPrice() != null ? variant.getOriginalPrice().doubleValue() : null);
    vd.setStockQuantity(variant.getStockQuantity());
    vd.setIsDefault(variant.getIsDefault());

    if (variant.getProduct() != null) {
      CartDto.CartProductDto pd = new CartDto.CartProductDto();
      pd.setId(variant.getProduct().getId());
      pd.setName(variant.getProduct().getName());
      vd.setProduct(pd);
    }

    String imageUrl = null;
    if (variant.getImages() != null && !variant.getImages().isEmpty()) {
      imageUrl = serverUrl + "/img/" + variant.getImages().get(0).getId();
    } else if (variant.getProduct() != null
            && variant.getProduct().getImages() != null
            && !variant.getProduct().getImages().isEmpty()) {
      imageUrl = serverUrl + "/img/" + variant.getProduct().getImages().get(0).getId();
    }
    vd.setImageUrl(imageUrl);

    return vd;
  }
}