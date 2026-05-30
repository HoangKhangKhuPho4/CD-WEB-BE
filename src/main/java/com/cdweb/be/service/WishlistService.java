package com.cdweb.be.service;

import com.cdweb.be.dto.ProductDto;
import com.cdweb.be.dto.WishlistDto;
import com.cdweb.be.entity.Product;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.entity.User;
import com.cdweb.be.entity.Wishlist;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.ProductRepository;
import com.cdweb.be.repository.ProductVariantRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.repository.WishlistRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor; // Sử dụng Lombok
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor // Constructor Injection giúp code sạch và dễ test
public class WishlistService {

  private final WishlistRepository wishlistRepository;
  private final UserRepository userRepository;
  private final ProductRepository productRepository;
  private final ProductVariantRepository productVariantRepository;
  private final ModelMapper modelMapper;

  @Value("${app.server.url:http://localhost:8080}")
  private String serverUrl;

  public Page<WishlistDto.Response> getUserWishlist(String username, Pageable pageable) {
    User user = findUser(username);
    // Repository giờ đã nhận Long userId
    return wishlistRepository.findByUserId(user.getId(), pageable).map(this::mapToResponse);
  }

  public WishlistDto.Response addToWishlist(String username, WishlistDto.Request request) {
    User user = findUser(username);

    Product product = productRepository.findById(request.getProductId())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

    ProductVariant variant = null;
    if (request.getVariantId() != null) {
      variant = productVariantRepository.findById(request.getVariantId())
              .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "id", request.getVariantId()));
    }

    // Check if already in wishlist - Đã khớp Long cho user.getId()
    Optional<Wishlist> existing;
    if (variant != null) {
      existing = wishlistRepository.findByUserIdAndProductIdAndVariantId(
              user.getId(), product.getId(), variant.getId());
    } else {
      existing = wishlistRepository.findByUserIdAndProductIdAndVariantIsNull(
              user.getId(), product.getId());
    }

    if (existing.isPresent()) {
      throw new BadRequestException("Sản phẩm đã nằm trong danh sách yêu thích của bạn.");
    }

    Wishlist wishlist = new Wishlist();
    wishlist.setUser(user);
    wishlist.setProduct(product);
    wishlist.setVariant(variant);

    Wishlist saved = wishlistRepository.save(wishlist);
    return mapToResponse(saved);
  }

  public void removeFromWishlist(String username, Integer productId) {
    User user = findUser(username);

    // Đã khớp Long cho user.getId()
    Wishlist wishlist = wishlistRepository
            .findByUserIdAndProductId(user.getId(), productId)
            .orElseThrow(() -> new ResourceNotFoundException("Wishlist", "productId", productId));

    wishlistRepository.delete(wishlist);
  }

  public void removeWishlistItem(String username, Integer id) {
    User user = findUser(username);

    Wishlist wishlist = wishlistRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Wishlist", "id", id));

    // So sánh 2 giá trị Long
    if (!wishlist.getUser().getId().equals(user.getId())) {
      throw new BadRequestException("Bạn không có quyền xóa mục này");
    }

    wishlistRepository.delete(wishlist);
  }

  public boolean checkWishlistStatus(String username, Integer productId) {
    User user = findUser(username);
    // Đã khớp Long cho user.getId()
    return wishlistRepository.existsByUserIdAndProductId(user.getId(), productId);
  }

  public void clearWishlist(String username) {
    User user = findUser(username);
    // Đã khớp Long cho user.getId()
    wishlistRepository.deleteByUserId(user.getId());
  }

  // --- HELPERS ---
  private User findUser(String usernameOrEmail) {
    return userRepository
            .findByUsernameOrEmail(usernameOrEmail, usernameOrEmail)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", usernameOrEmail));
  }

  private WishlistDto.Response mapToResponse(Wishlist wishlist) {
    WishlistDto.Response response = new WishlistDto.Response();
    response.setId(wishlist.getId());
    response.setCreatedAt(wishlist.getCreatedAt());

    if (wishlist.getProduct() != null) {
      response.setProduct(mapProductToResponse(wishlist.getProduct()));
    }

    if (wishlist.getVariant() != null) {
      response.setVariant(modelMapper.map(wishlist.getVariant(), ProductDto.VariantDto.class));
    }

    return response;
  }

  private ProductDto.Response mapProductToResponse(Product product) {
    ProductDto.Response response = modelMapper.map(product, ProductDto.Response.class);
    if (product.getBasePrice() != null) {
      response.setPrice(product.getBasePrice().doubleValue());
    }
    // Đồng bộ logic Boolean -> 1/0 cho Frontend cũ
    response.setActive(Boolean.TRUE.equals(product.getIsActive()) ? 1 : 0);
    response.setAverageRating(0.0);
    response.setReviewCount(0);

    if (product.getImages() != null && !product.getImages().isEmpty()) {
      java.util.List<ProductDto.ImageDto> imgs = new java.util.ArrayList<>();
      for (var img : product.getImages()) {
        ProductDto.ImageDto imgDto = new ProductDto.ImageDto();
        imgDto.setId(img.getId());
        imgDto.setLinkImage(serverUrl + "/img/" + img.getId());
        imgDto.setVariantId(img.getVariant() != null ? img.getVariant().getId() : null);
        imgs.add(imgDto);
      }
      response.setImages(imgs);
    }
    return response;
  }
}