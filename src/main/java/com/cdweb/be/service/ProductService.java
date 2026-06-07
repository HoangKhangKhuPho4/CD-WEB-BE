package com.cdweb.be.service;

import com.cdweb.be.dto.ProductDto;
import com.cdweb.be.entity.AttributeValue;
import com.cdweb.be.entity.Image;
import com.cdweb.be.entity.Producer;
import com.cdweb.be.entity.Product;
import com.cdweb.be.entity.ProductType;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.AttributeValueRepository;
import com.cdweb.be.repository.CouponRepository;
import com.cdweb.be.repository.ImageRepository;
import com.cdweb.be.repository.OrderDetailRepository;
import com.cdweb.be.repository.ProducerRepository;
import com.cdweb.be.repository.ProductRepository;
import com.cdweb.be.repository.ProductSpecification;
import com.cdweb.be.repository.ProductTypeRepository;
import com.cdweb.be.repository.ProductVariantRepository;
import com.cdweb.be.repository.ReviewRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class ProductService {

  @Autowired private ProductRepository productRepository;

  @Autowired private ProductTypeRepository productTypeRepository;

  @Autowired private ProducerRepository producerRepository;

  @Autowired private CouponRepository couponRepository;

  @Autowired private ProductVariantRepository productVariantRepository;

  @Autowired private OrderDetailRepository orderDetailRepository;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private AttributeValueRepository attributeValueRepository;
  @Autowired private ImageRepository imageRepository;
  @Autowired private ModelMapper modelMapper;

  @Value("${app.server.url:http://localhost:8080}")
  private String serverUrl;

  public Page<ProductDto.Response> getAllProducts(Pageable pageable) {
    return productRepository.findActiveProductsPage(pageable).map(this::mapToResponse);
  }

  public ProductDto.Response getProductById(Long id) {
    Product product =
        productRepository
            .findById(id.intValue())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    if (product.getIsActive() == null || !product.getIsActive()) {
      throw new ResourceNotFoundException("Product", "id", id);
    }
    return mapToResponse(product);
  }

  public ProductDto.Response getProductBySlug(String slug) {
    Product product =
        productRepository
            .findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
    if (product.getIsActive() == null || !product.getIsActive()) {
      throw new ResourceNotFoundException("Product", "slug", slug);
    }
    return mapToResponse(product);
  }

  public ProductDto.Response getProductBySku(String sku) {
    ProductVariant variant =
        productVariantRepository
            .findBySkuCode(sku)
            .orElseThrow(() -> new ResourceNotFoundException("ProductVariant", "skuCode", sku));
    if (variant.getIsActive() == null || !variant.getIsActive()) {
      throw new ResourceNotFoundException("ProductVariant", "skuCode", sku);
    }
    Product product =
        productRepository
            .findById(variant.getProduct().getId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Product", "id", variant.getProduct().getId()));
    if (product.getIsActive() == null || !product.getIsActive()) {
      throw new ResourceNotFoundException("Product", "id", product.getId());
    }
    return mapToResponse(product);
  }

  public ProductDto.Response createProduct(ProductDto.CreateRequest createRequest) {
    ProductType productType =
        productTypeRepository
            .findById(createRequest.getProductTypeId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "ProductType", "id", createRequest.getProductTypeId()));

    Producer producer =
        producerRepository
            .findById(createRequest.getProducerId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Producer", "id", createRequest.getProducerId()));

    Product product = new Product();
    product.setName(createRequest.getName());
    if (createRequest.getPrice() != null) {
      product.setBasePrice(BigDecimal.valueOf(createRequest.getPrice()));
    }
    product.setDescription(createRequest.getDetail());
    product.setIsActive(true); // Active
    product.setProductType(productType);
    product.setProducer(producer);
    if (createRequest.getIsFeatured() != null) {
      product.setIsFeatured(createRequest.getIsFeatured());
    }

    // Create a default variant to hold the initial quantity and price
    List<ProductVariant> variants = new ArrayList<>();
    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setVariantName("Default");
    variant.setSkuCode("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    variant.setPrice(product.getBasePrice() != null ? product.getBasePrice() : BigDecimal.ZERO);
    variant.setStockQuantity(createRequest.getQuantity() != null ? createRequest.getQuantity() : 0);
    variant.setIsDefault(true);
    variant.setIsActive(true);
    variants.add(variant);
    product.setVariants(variants);

    Product savedProduct = productRepository.save(product);
    return mapToResponse(savedProduct);
  }

  public ProductDto.Response updateProduct(Long id, ProductDto.UpdateRequest updateRequest) {
    Product product =
        productRepository
            .findById(id.intValue())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

    if (updateRequest.getName() != null) {
      product.setName(updateRequest.getName());
    }
    if (updateRequest.getPrice() != null) {
      product.setBasePrice(BigDecimal.valueOf(updateRequest.getPrice()));
      if (product.getVariants() != null) {
        product.getVariants().stream()
            .filter(v -> v.getIsDefault() != null && v.getIsDefault())
            .findFirst()
            .ifPresent(v -> v.setPrice(BigDecimal.valueOf(updateRequest.getPrice())));
      }
    }
    if (updateRequest.getQuantity() != null && product.getVariants() != null) {
      product.getVariants().stream()
          .filter(v -> v.getIsDefault() != null && v.getIsDefault())
          .findFirst()
          .ifPresent(v -> v.setStockQuantity(updateRequest.getQuantity()));
    }
    if (updateRequest.getStatus() != null) {
      product.setIsActive(
          "1".equals(updateRequest.getStatus())
              || "Active".equalsIgnoreCase(updateRequest.getStatus()));
    }
    if (updateRequest.getDetail() != null) {
      product.setDescription(updateRequest.getDetail());
    }
    if (updateRequest.getProductTypeId() != null) {
      ProductType productType =
          productTypeRepository
              .findById(updateRequest.getProductTypeId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "ProductType", "id", updateRequest.getProductTypeId()));
      product.setProductType(productType);
    }
    if (updateRequest.getProducerId() != null) {
      Producer producer =
          producerRepository
              .findById(updateRequest.getProducerId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Producer", "id", updateRequest.getProducerId()));
      product.setProducer(producer);
    }

    if (updateRequest.getIsFeatured() != null) {
      product.setIsFeatured(updateRequest.getIsFeatured());
    }

    Product updatedProduct = productRepository.save(product);
    return mapToResponse(updatedProduct);
  }

  public void deleteProduct(Long id) {
    Product product =
        productRepository
            .findById(id.intValue())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    product.setIsActive(false); // Set as inactive
    productRepository.save(product);
  }

  public Page<ProductDto.Response> getFeaturedProducts(Pageable pageable) {
    return productRepository.findFeaturedProductsPage(pageable).map(this::mapToResponse);
  }

  public Page<ProductDto.Response> getBestSellingProducts(Pageable pageable) {
    return productRepository.findBestSellingProductsPage(pageable).map(this::mapToResponse);
  }

  public Page<ProductDto.Response> searchProducts(ProductDto.SearchRequest searchRequest) {
    Sort sort = createSort(searchRequest.getSortBy(), searchRequest.getSortDir());
    Pageable pageable = PageRequest.of(searchRequest.getPage(), searchRequest.getSize(), sort);

    Specification<Product> spec = ProductSpecification.buildSearchSpec(searchRequest);
    return productRepository.findAll(spec, pageable).map(this::mapToResponse);
  }

  /** Gợi ý sản phẩm cho ô tìm kiếm — keyword tối thiểu 2 ký tự. */
  public List<ProductDto.SuggestResponse> suggestProducts(
      String keyword, Integer productTypeId, int limit) {
    if (keyword == null || keyword.trim().length() < 2) {
      return List.of();
    }
    int capped = Math.min(Math.max(limit, 1), 20);
    ProductDto.SearchRequest req = new ProductDto.SearchRequest();
    req.setKeyword(keyword.trim());
    req.setProductTypeId(productTypeId);
    req.setPage(0);
    req.setSize(capped);
    req.setSortBy("name");
    req.setSortDir("asc");

    return searchProducts(req).getContent().stream()
        .map(
            p -> {
              ProductDto.SuggestResponse s = new ProductDto.SuggestResponse();
              s.setId(p.getId());
              s.setName(p.getName());
              s.setPrice(p.getPrice());
              if (p.getImages() != null && !p.getImages().isEmpty()) {
                s.setImageUrl(p.getImages().get(0).getLinkImage());
              }
              if (p.getProductType() != null) {
                s.setCategoryId(p.getProductType().getId());
                s.setCategoryName(p.getProductType().getName());
              }
              return s;
            })
        .collect(Collectors.toList());
  }

  public Page<ProductDto.Response> getProductsByCategory(Long categoryId, Pageable pageable) {
    return productRepository
        .findByProductTypeIdAndIsActiveTrue(categoryId.intValue(), pageable)
        .map(this::mapToResponse);
  }

  public List<ProductDto.VariantDto> getVariantsByProductId(Long productId) {
    // Verify product exists
    productRepository
        .findById(productId.intValue())
        .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

    return productVariantRepository.findByProductId(productId.intValue()).stream()
        .filter(
            v ->
                v.getIsActive() != null
                    && v.getIsActive()) // Bắt buộc lọc để Public API không lộ variant đã ngưng bán
        .map(this::mapVariantToDto)
        .collect(Collectors.toList());
  }

  private ProductDto.VariantDto mapVariantToDto(ProductVariant variant) {
    ProductDto.VariantDto vDto = new ProductDto.VariantDto();
    vDto.setId(variant.getId());
    if (variant.getProduct() != null) {
      vDto.setProductId(variant.getProduct().getId());
      vDto.setProductName(variant.getProduct().getName());
    }
    vDto.setSkuCode(variant.getSkuCode());
    vDto.setVariantName(variant.getVariantName());
    vDto.setPrice(variant.getPrice() != null ? variant.getPrice().doubleValue() : 0.0);
    vDto.setOriginalPrice(
        variant.getOriginalPrice() != null ? variant.getOriginalPrice().doubleValue() : null);
    vDto.setStockQuantity(variant.getStockQuantity());
    vDto.setIsActive(variant.getIsActive());
    vDto.setIsDefault(variant.getIsDefault());

    // Map structured attribute values (Color, Storage, etc.)
    if (variant.getAttributeValues() != null) {
      List<ProductDto.AttributeValueResponse> attrValues = new ArrayList<>();
      for (var attr : variant.getAttributeValues()) {
        ProductDto.AttributeValueResponse avResponse = new ProductDto.AttributeValueResponse();
        avResponse.setId(attr.getId());
        if (attr.getAttribute() != null) {
          avResponse.setAttributeName(attr.getAttribute().getName());
        }
        avResponse.setValue(attr.getValue());
        attrValues.add(avResponse);
      }
      vDto.setAttributeValues(attrValues);
    }
    return vDto;
  }

  public Page<ProductDto.Response> getProductsByBrand(String brand, Pageable pageable) {
    final int producerId;
    try {
      producerId = Integer.parseInt(brand);
    } catch (NumberFormatException e) {
      throw new BadRequestException("producerId must be a valid integer");
    }
    return productRepository
        .findByProducerIdAndIsActive(producerId, true, pageable)
        .map(this::mapToResponse);
  }

  public List<String> getAllBrands() {
    return productRepository.findAllProducerNames();
  }

  public void updateStock(Long productId, Integer quantity) {
    Product product =
        productRepository
            .findById(productId.intValue())
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
      ProductVariant defaultVariant =
          product.getVariants().stream()
              .filter(v -> v.getIsDefault() != null && v.getIsDefault())
              .findFirst()
              .orElse(product.getVariants().get(0));

      if (defaultVariant.getStockQuantity() < quantity) {
        throw new BadRequestException(
            "Insufficient stock. Available: " + defaultVariant.getStockQuantity());
      }

      defaultVariant.setStockQuantity(defaultVariant.getStockQuantity() - quantity);
      productRepository.save(product);
    } else {
      throw new BadRequestException("Product has no variants. Cannot update stock.");
    }
  }

  private ProductDto.Response mapToResponse(Product product) {
    ProductDto.Response response = modelMapper.map(product, ProductDto.Response.class);

    response.setPrice(product.getBasePrice() != null ? product.getBasePrice().doubleValue() : 0.0);
    response.setDetail(product.getDescription());
    response.setStatus(product.getIsActive() != null && product.getIsActive() ? "1" : "0");
    response.setActive(product.getIsActive() != null && product.getIsActive() ? 1 : 0);
    response.setImportDate(
        product.getCreatedAt() != null ? product.getCreatedAt().toLocalDate() : null);

    // Tính số lượng đã bán hiển thị cho User Card
    Long soldCount = orderDetailRepository.sumQuantityByProductId(product.getId());
    response.setSoldQuantity(soldCount != null ? soldCount.intValue() : 0);

    // Lọc chỉ lấy variant đang active (Public API không hiển thị variant đã bị vô hiệu hóa)
    Set<Integer> activeVariantIds = new HashSet<>();
    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
      for (var variant : product.getVariants()) {
        if (variant.getIsActive() != null && variant.getIsActive()) {
          activeVariantIds.add(variant.getId());
        }
      }
    }

    int totalQuantity = 0;
    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
      totalQuantity =
          product.getVariants().stream()
              .filter(v -> v.getIsActive() != null && v.getIsActive())
              .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
              .sum();
    }
    response.setQuantity(totalQuantity);

    Double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
    Integer reviewCount = reviewRepository.countApprovedByProductId(product.getId());
    response.setAverageRating(avgRating != null ? Math.round(avgRating * 10.0) / 10.0 : 0.0);
    response.setReviewCount(reviewCount != null ? reviewCount : 0);

    // Map images: CHỈ trả ảnh chung (variant_id = null) hoặc ảnh thuộc variant ACTIVE
    if (product.getImages() != null && !product.getImages().isEmpty()) {
      List<ProductDto.ImageDto> imgs = new ArrayList<>();
      for (var img : product.getImages()) {
        Integer imgVariantId = img.getVariant() != null ? img.getVariant().getId() : null;

        // Chỉ trả ảnh nếu: (1) ảnh chung (variant_id = null) hoặc (2) variant đang active
        if (imgVariantId == null || activeVariantIds.contains(imgVariantId)) {
          ProductDto.ImageDto imgDto = new ProductDto.ImageDto();
          imgDto.setId(img.getId());
          imgDto.setLinkImage(serverUrl + "/img/" + img.getId());
          imgDto.setVariantId(imgVariantId);
          imgs.add(imgDto);
        }
      }
      response.setImages(imgs);
    }

    // Map variants — CHỈ trả variant ACTIVE cho Public API
    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
      List<ProductDto.VariantDto> variantDtos = new ArrayList<>();
      for (var variant : product.getVariants()) {
        // Lọc variant inactive — khách hàng KHÔNG thấy variant đã ngưng bán
        if (variant.getIsActive() == null || !variant.getIsActive()) {
          continue;
        }
        ProductDto.VariantDto vDto = new ProductDto.VariantDto();
        vDto.setId(variant.getId());
        vDto.setProductId(product.getId());
        vDto.setProductName(product.getName());
        vDto.setSkuCode(variant.getSkuCode());
        vDto.setVariantName(variant.getVariantName());
        vDto.setPrice(variant.getPrice() != null ? variant.getPrice().doubleValue() : 0.0);
        vDto.setOriginalPrice(
            variant.getOriginalPrice() != null ? variant.getOriginalPrice().doubleValue() : null);
        vDto.setStockQuantity(variant.getStockQuantity());
        vDto.setIsActive(variant.getIsActive());
        vDto.setIsDefault(variant.getIsDefault());

        // Map structured attribute values (Color, Storage, etc.)
        if (variant.getAttributeValues() != null) {
          List<ProductDto.AttributeValueResponse> attrValues = new ArrayList<>();
          for (var attr : variant.getAttributeValues()) {
            ProductDto.AttributeValueResponse avResponse = new ProductDto.AttributeValueResponse();
            avResponse.setId(attr.getId());
            if (attr.getAttribute() != null) {
              avResponse.setAttributeName(attr.getAttribute().getName());
            }
            avResponse.setValue(attr.getValue());
            attrValues.add(avResponse);
          }
          vDto.setAttributeValues(attrValues);
        }

        variantDtos.add(vDto);
      }
      response.setVariants(variantDtos);

      // Group attributes for selection UI (Options) — chỉ từ variant active
      Map<String, Set<String>> groupedOptions = new HashMap<>();
      for (var vDto : variantDtos) {
        if (vDto.getAttributeValues() != null) {
          for (var attr : vDto.getAttributeValues()) {
            groupedOptions
                .computeIfAbsent(attr.getAttributeName(), k -> new LinkedHashSet<>())
                .add(attr.getValue());
          }
        }
      }

      List<ProductDto.ProductOptionDto> options = new ArrayList<>();
      groupedOptions.forEach(
          (name, values) -> {
            options.add(new ProductDto.ProductOptionDto(name, new ArrayList<>(values)));
          });
      response.setOptions(options);
    }

    return response;
  }

  private Sort createSort(String sortBy, String sortDir) {
    Sort.Direction direction =
        sortDir.equalsIgnoreCase("desc") ? Sort.Direction.DESC : Sort.Direction.ASC;

    return switch (sortBy.toLowerCase()) {
      case "price" -> Sort.by(direction, "basePrice");
      case "importdate" -> Sort.by(direction, "createdAt");
      case "name" -> Sort.by(direction, "name");
      default -> Sort.by(direction, "name");
    };
  }

  // --- ADMIN PRODUCT MANAGEMENT ---

  public Page<ProductDto.AdminProductListResponse> adminGetAllProducts(
      String keyword,
      Boolean isActive,
      Integer productTypeId,
      Integer producerId,
      Boolean isFeatured,
      Pageable pageable) {
    Page<Product> products =
        productRepository.adminSearchProducts(
            keyword, isActive, productTypeId, producerId, isFeatured, pageable);
    return products.map(
        product -> {
          ProductDto.AdminProductListResponse response =
              modelMapper.map(product, ProductDto.AdminProductListResponse.class);
          response.setBasePrice(
              product.getBasePrice() != null ? product.getBasePrice().doubleValue() : 0.0);
          response.setStatus(
              product.getIsActive() != null && product.getIsActive() ? "ACTIVE" : "INACTIVE");
          response.setCreatedAt(
              product.getCreatedAt() != null ? product.getCreatedAt().toLocalDate() : null);

          if (product.getImages() != null && !product.getImages().isEmpty()) {
            Image primaryImage =
                product.getImages().stream()
                    .filter(img -> img.getIsPrimary() != null && img.getIsPrimary())
                    .findFirst()
                    .orElse(product.getImages().get(0));
            response.setImageUrl(serverUrl + "/img/" + primaryImage.getId());
          }

          int totalQuantity = 0;
          if (product.getVariants() != null) {
            totalQuantity =
                product.getVariants().stream()
                    .mapToInt(v -> v.getStockQuantity() != null ? v.getStockQuantity() : 0)
                    .sum();
          }
          response.setTotalQuantity(totalQuantity);
          return response;
        });
  }

  public ProductDto.AdminProductResponse adminGetProductById(Integer id) {
    Product product =
        productRepository
            .findByIdWithAdminBasics(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    product.setVariants(productVariantRepository.findByProductIdWithAttributes(id));
    ProductDto.AdminProductResponse response =
        modelMapper.map(product, ProductDto.AdminProductResponse.class);
    response.setBasePrice(
        product.getBasePrice() != null ? product.getBasePrice().doubleValue() : 0.0);
    response.setStatus(
        product.getIsActive() != null && product.getIsActive() ? "ACTIVE" : "INACTIVE");
    response.setCreatedAt(
        product.getCreatedAt() != null ? product.getCreatedAt().toLocalDate() : null);

    if (product.getImages() != null && !product.getImages().isEmpty()) {
      List<ProductDto.ImageDto> imgs = new ArrayList<>();
      for (var img : product.getImages()) {
        ProductDto.ImageDto imgDto = new ProductDto.ImageDto();
        imgDto.setId(img.getId());
        imgDto.setLinkImage(serverUrl + "/img/" + img.getId());
        imgDto.setVariantId(img.getVariant() != null ? img.getVariant().getId() : null);
        imgs.add(imgDto);
      }
      response.setImages(imgs);
    }

    if (product.getVariants() != null && !product.getVariants().isEmpty()) {
      List<ProductDto.VariantDto> variantDtos = new ArrayList<>();
      for (var variant : product.getVariants()) {
        variantDtos.add(mapVariantToDto(variant));
      }
      response.setVariants(variantDtos);
    }

    return response;
  }

  public ProductDto.AdminProductResponse adminCreateProduct(ProductDto.AdminCreateRequest request) {
    ProductType productType =
        productTypeRepository
            .findById(request.getProductTypeId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("ProductType", "id", request.getProductTypeId()));
    Producer producer =
        producerRepository
            .findById(request.getProducerId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Producer", "id", request.getProducerId()));

    Product product = new Product();
    product.setName(request.getName());
    product.setBasePrice(BigDecimal.valueOf(request.getPrice()));
    product.setDescription(request.getDetail());
    product.setIsActive("ACTIVE".equalsIgnoreCase(request.getStatus()));
    product.setProductType(productType);
    product.setProducer(producer);
    if (request.getIsFeatured() != null) {
      product.setIsFeatured(request.getIsFeatured());
    }

    List<ProductVariant> variants = new ArrayList<>();
    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    variant.setVariantName("Mặc định");
    variant.setSkuCode("SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
    variant.setPrice(product.getBasePrice() != null ? product.getBasePrice() : BigDecimal.ZERO);
    variant.setStockQuantity(request.getQuantity() != null ? request.getQuantity() : 0);
    variant.setIsDefault(true);
    variant.setIsActive(true);
    variants.add(variant);
    product.setVariants(variants);

    Product savedProduct = productRepository.save(product);
    return adminGetProductById(savedProduct.getId());
  }

  public ProductDto.AdminProductResponse adminUpdateProduct(
      Integer id, ProductDto.AdminUpdateRequest request) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    if (request.getName() != null) {
      product.setName(request.getName());
      product.setSlug(generateUniqueSlug(request.getName(), id));
    }
    if (request.getPrice() != null) product.setBasePrice(BigDecimal.valueOf(request.getPrice()));
    if (request.getDetail() != null) product.setDescription(request.getDetail());
    if (request.getStatus() != null)
      product.setIsActive("ACTIVE".equalsIgnoreCase(request.getStatus()));
    if (request.getIsFeatured() != null) product.setIsFeatured(request.getIsFeatured());

    if (request.getProductTypeId() != null) {
      ProductType productType =
          productTypeRepository
              .findById(request.getProductTypeId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "ProductType", "id", request.getProductTypeId()));
      product.setProductType(productType);
    }
    if (request.getProducerId() != null) {
      Producer producer =
          producerRepository
              .findById(request.getProducerId())
              .orElseThrow(
                  () -> new ResourceNotFoundException("Producer", "id", request.getProducerId()));
      product.setProducer(producer);
    }
    Product savedProduct = productRepository.save(product);
    return adminGetProductById(savedProduct.getId());
  }

  public void adminDeleteProduct(Integer id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    product.setIsActive(false);
    productRepository.save(product);
  }

  public ProductDto.AdminProductResponse adminToggleStatus(Integer id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    product.setIsActive(product.getIsActive() == null || !product.getIsActive());
    productRepository.save(product);
    return adminGetProductById(id);
  }

  public ProductDto.AdminProductResponse adminAddVariant(
      Integer id, ProductDto.AdminVariantRequest request) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    ProductVariant variant = new ProductVariant();
    variant.setProduct(product);
    String sku =
        request.getSkuCode() != null && !request.getSkuCode().isBlank()
            ? request.getSkuCode().trim()
            : "SKU-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    assertSkuAvailable(sku, null);
    variant.setSkuCode(sku);
    if (request.getVariantName() != null) variant.setVariantName(request.getVariantName());
    if (request.getPrice() != null) variant.setPrice(BigDecimal.valueOf(request.getPrice()));
    if (request.getOriginalPrice() != null)
      variant.setOriginalPrice(BigDecimal.valueOf(request.getOriginalPrice()));
    variant.setStockQuantity(request.getStockQuantity() != null ? request.getStockQuantity() : 0);
    variant.setIsDefault(Boolean.TRUE.equals(request.getIsDefault()));
    variant.setIsActive(!Boolean.FALSE.equals(request.getIsActive()));

    if (request.getAttributeValueIds() != null && !request.getAttributeValueIds().isEmpty()) {
      List<AttributeValue> attributes =
          attributeValueRepository.findAllById(request.getAttributeValueIds());
      variant.setAttributeValues(attributes);
    }

    productVariantRepository.save(variant);
    return adminGetProductById(id);
  }

  public ProductDto.AdminProductResponse adminUpdateVariant(
      Integer productId, Integer variantId, ProductDto.AdminVariantRequest request) {
    ProductVariant variant =
        productVariantRepository
            .findById(variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", variantId));
    if (request.getSkuCode() != null && !request.getSkuCode().isBlank()) {
      assertSkuAvailable(request.getSkuCode().trim(), variantId);
      variant.setSkuCode(request.getSkuCode().trim());
    }
    if (request.getVariantName() != null) variant.setVariantName(request.getVariantName());
    if (request.getPrice() != null) variant.setPrice(BigDecimal.valueOf(request.getPrice()));
    if (request.getOriginalPrice() != null)
      variant.setOriginalPrice(BigDecimal.valueOf(request.getOriginalPrice()));
    if (request.getStockQuantity() != null) variant.setStockQuantity(request.getStockQuantity());
    if (request.getIsDefault() != null) variant.setIsDefault(request.getIsDefault());
    if (request.getIsActive() != null) variant.setIsActive(request.getIsActive());

    if (request.getAttributeValueIds() != null) {
      List<AttributeValue> attributes =
          attributeValueRepository.findAllById(request.getAttributeValueIds());
      variant.setAttributeValues(attributes);
    }

    productVariantRepository.save(variant);
    return adminGetProductById(productId);
  }

  public ProductDto.AdminProductResponse adminDeleteVariant(Integer productId, Integer variantId) {
    ProductVariant variant =
        productVariantRepository
            .findById(variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", variantId));

    // Soft Delete: Vô hiệu hóa variant thay vì xóa cứng
    // Lý do: Bảo toàn lịch sử đơn hàng, bảo hành, giao dịch kho
    variant.setIsActive(false);
    productVariantRepository.save(variant);
    return adminGetProductById(productId);
  }

  public ProductDto.AdminProductResponse adminAddImage(
      Integer id, ProductDto.AdminImageRequest request) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    Image image = new Image();
    image.setProduct(product);
    if (request.getVariantId() != null) {
      ProductVariant variant =
          productVariantRepository.findById(request.getVariantId()).orElse(null);
      image.setVariant(variant);
    }
    image.setImageUrl(request.getLinkImage());
    image.setIsPrimary(Boolean.TRUE.equals(request.getIsDefault()));

    product.getImages().add(image);
    productRepository.save(product);
    return adminGetProductById(id);
  }

  public ProductDto.AdminProductResponse adminDeleteImage(Integer productId, Integer imageId) {
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

    Image imageToDelete = null;
    if (product.getImages() != null) {
      for (Image img : product.getImages()) {
        if (img.getId().equals(imageId)) {
          imageToDelete = img;
          break;
        }
      }
    }

    if (imageToDelete == null) {
      throw new ResourceNotFoundException("Image", "id", imageId);
    }

    product.getImages().remove(imageToDelete);
    imageRepository.delete(imageToDelete);

    return adminGetProductById(productId);
  }

  public ProductDto.AdminStatsResponse adminGetProductStats() {
    return new ProductDto.AdminStatsResponse(
        productRepository.count(),
        productRepository.countByIsActive(true),
        productRepository.countByIsActive(false),
        productRepository.countByIsFeatured(true),
        productVariantRepository.findLowStockVariants().size(),
        productVariantRepository.countByIsActiveTrue());
  }

  public ProductDto.ValidateSkuResponse validateSku(ProductDto.ValidateSkuRequest request) {
    String sku = request.getSkuCode().trim();
    var existing = productVariantRepository.findBySkuCode(sku);
    if (existing.isEmpty()
        || (request.getExcludeVariantId() != null
            && existing.get().getId().equals(request.getExcludeVariantId()))) {
      return new ProductDto.ValidateSkuResponse(sku, true, "SKU khả dụng", null, null);
    }
    ProductVariant v = existing.get();
    return new ProductDto.ValidateSkuResponse(
        sku,
        false,
        "SKU đã được sử dụng",
        v.getId(),
        v.getProduct() != null ? v.getProduct().getId() : null);
  }

  public ProductDto.BulkStatusResult adminBulkStatus(ProductDto.BulkStatusRequest request) {
    int success = 0;
    int fail = 0;
    List<String> errors = new ArrayList<>();
    for (Integer id : request.getIds()) {
      try {
        Product product =
            productRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        product.setIsActive(request.getIsActive());
        productRepository.save(product);
        success++;
      } catch (Exception e) {
        fail++;
        errors.add("ID " + id + ": " + e.getMessage());
      }
    }
    return new ProductDto.BulkStatusResult(success, fail, errors);
  }

  public ProductDto.AdminProductResponse adminSetFeatured(
      Integer id, ProductDto.FeaturedRequest request) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
    product.setIsFeatured(request.getIsFeatured());
    productRepository.save(product);
    return adminGetProductById(id);
  }

  public ProductDto.AdminProductResponse adminSetVariantStock(
      Integer productId, Integer variantId, ProductDto.VariantStockRequest request) {
    ProductVariant variant =
        productVariantRepository
            .findById(variantId)
            .orElseThrow(() -> new ResourceNotFoundException("Variant", "id", variantId));
    if (variant.getProduct() == null || !variant.getProduct().getId().equals(productId)) {
      throw new BadRequestException("Variant không thuộc sản phẩm #" + productId);
    }
    variant.setStockQuantity(request.getStockQuantity());
    productVariantRepository.save(variant);
    return adminGetProductById(productId);
  }

  public ProductDto.AdminProductResponse adminGetProductBySlug(String slug) {
    Product product =
        productRepository
            .findBySlug(slug)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
    return adminGetProductById(product.getId());
  }

  private void assertSkuAvailable(String sku, Integer excludeVariantId) {
    productVariantRepository
        .findBySkuCode(sku)
        .ifPresent(
            v -> {
              if (excludeVariantId == null || !v.getId().equals(excludeVariantId)) {
                throw new BadRequestException("SKU \"" + sku + "\" đã tồn tại trong hệ thống");
              }
            });
  }

  private String generateUniqueSlug(String name, Integer excludeId) {
    String base = slugify(name);
    if (base.isEmpty()) {
      base = "product";
    }
    String candidate = base;
    int suffix = 1;
    while (true) {
      var existing = productRepository.findBySlug(candidate);
      if (existing.isEmpty()
          || (excludeId != null && existing.get().getId().equals(excludeId))) {
        break;
      }
      candidate = base + "-" + suffix++;
    }
    return candidate;
  }

  private static String slugify(String name) {
    if (name == null) {
      return "";
    }
    return name
        .trim()
        .toLowerCase()
        .replaceAll("[àáạảãâầấậẩẫăằắặẳẵ]", "a")
        .replaceAll("[èéẹẻẽêềếệểễ]", "e")
        .replaceAll("[ìíịỉĩ]", "i")
        .replaceAll("[òóọỏõôồốộổỗơờớợởỡ]", "o")
        .replaceAll("[ùúụủũưừứựửữ]", "u")
        .replaceAll("[ỳýỵỷỹ]", "y")
        .replaceAll("[đ]", "d")
        .replaceAll("\\s+", "-")
        .replaceAll("[^a-z0-9-]", "")
        .replaceAll("-+", "-");
  }
}
