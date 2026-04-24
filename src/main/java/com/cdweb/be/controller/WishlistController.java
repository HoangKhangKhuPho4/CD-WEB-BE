package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.WishlistDto;
import com.cdweb.be.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/wishlist")
@CrossOrigin(origins = "*")
public class WishlistController {

  @Autowired private WishlistService wishlistService;

  @GetMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Page<WishlistDto.Response>>> getWishlist(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    Pageable pageable = PageRequest.of(page, size);
    Page<WishlistDto.Response> wishlistPage = wishlistService.getUserWishlist(username, pageable);

    return ResponseEntity.ok(ApiResponse.success("Wishlist retrieved successfully", wishlistPage));
  }

  @PostMapping
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<WishlistDto.Response>> addToWishlist(
      @Valid @RequestBody WishlistDto.Request request) {

    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    WishlistDto.Response response = wishlistService.addToWishlist(username, request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Added to wishlist successfully", response));
  }

  @DeleteMapping("/product/{productId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Void>> removeFromWishlistByProductId(
      @PathVariable Integer productId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    wishlistService.removeFromWishlist(username, productId);
    return ResponseEntity.ok(ApiResponse.success("Removed from wishlist successfully", null));
  }

  @DeleteMapping("/{id}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Void>> removeFromWishlistById(@PathVariable Integer id) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    wishlistService.removeWishlistItem(username, id);
    return ResponseEntity.ok(ApiResponse.success("Removed from wishlist successfully", null));
  }

  @GetMapping("/check/{productId}")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Boolean>> checkWishlistStatus(@PathVariable Integer productId) {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    boolean status = wishlistService.checkWishlistStatus(username, productId);
    return ResponseEntity.ok(ApiResponse.success("Wishlist status checked successfully", status));
  }

  @DeleteMapping("/clear")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<Void>> clearWishlist() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    String username = authentication.getName();

    wishlistService.clearWishlist(username);
    return ResponseEntity.ok(ApiResponse.success("Wishlist cleared successfully", null));
  }
}
