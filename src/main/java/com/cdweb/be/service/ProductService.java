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
    return mapToResponse(product);
  }

  public ProductDto.Response getProductBySku(String sku) {
    // Note: SKU functionality not implemented in current Product entity
    // This method needs to be implemented when SKU field is added to Product
    throw new UnsupportedOperationException("SKU functionality not implemented yet");
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
    // Converting brand string to producer ID for now
    // This is a temporary solution until proper brand management is implemented
    try {
      Integer.parseInt(brand);
      return productRepository.findActiveProductsPage(pageable).map(this::mapToResponse);
    } catch (NumberFormatException e) {
      return productRepository.findActiveProductsPage(pageable).map(this::mapToResponse);
    }
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

    // Calculate average rating and review count
    // This would be implemented when Review functionality is added
    response.setAverageRating(0.0);
    response.setReviewCount(0);

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

  // --- ADMIN METHODS STUBS ---
  // TODO: These methods were referenced by AdminProductController but were missing.
  // They need proper implementation!

  public Page<ProductDto.AdminProductListResponse> adminGetAllProducts(
      String keyword,
      Boolean isActive,
      Integer productTypeId,
      Integer producerId,
      Pageable pageable) {
    Page<Product> products =
        productRepository.adminSearchProducts(
            keyword, isActive, productTypeId, producerId, pageable);
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
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
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
    if (request.getName() != null) product.setName(request.getName());
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
    if (request.getSkuCode() != null) variant.setSkuCode(request.getSkuCode());
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
    if (request.getSkuCode() != null) variant.setSkuCode(request.getSkuCode());
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

  public Map<String, Object> adminGetProductStats() {
    Map<String, Object> stats = new HashMap<>();
    stats.put("totalActive", productRepository.countByIsActive(true));
    stats.put("totalInactive", productRepository.countByIsActive(false));
    stats.put("totalProducts", productRepository.count());
    return stats;
  }
}
