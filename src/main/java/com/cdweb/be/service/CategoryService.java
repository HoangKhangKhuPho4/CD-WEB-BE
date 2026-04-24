package com.cdweb.be.service;

import com.cdweb.be.dto.CategoryDto;
import com.cdweb.be.dto.ProductDto;
import com.cdweb.be.entity.Product;
import com.cdweb.be.entity.ProductType;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.ProductRepository;
import com.cdweb.be.repository.ProductTypeRepository;
import com.cdweb.be.repository.ProductVariantRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@Slf4j
public class CategoryService {

  @Autowired private ProductTypeRepository productTypeRepository;

  @Autowired private ProductVariantRepository productVariantRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private ModelMapper modelMapper;

  @org.springframework.beans.factory.annotation.Value("${app.server.url:http://localhost:8080}")
  private String serverUrl;

  public Page<CategoryDto.Response> getAllCategories(Pageable pageable) {
    return productTypeRepository.findAllActive(pageable).map(this::mapToResponse);
  }

  public List<CategoryDto.Response> getAllCategoriesAsList() {
    return productTypeRepository.findAllActive().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public List<CategoryDto.Response> getRootCategories() {
    return productTypeRepository.findRootCategories().stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public CategoryDto.Response getCategoryById(Long id) {
    ProductType productType =
        productTypeRepository
            .findById(id.intValue())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));
    return mapToResponse(productType);
  }

  public CategoryDto.Response getCategoryBySlug(String code) {
    ProductType productType =
        productTypeRepository
            .findByCode(code)
            .orElseThrow(() -> new ResourceNotFoundException("Category", "code", code));
    return mapToResponse(productType);
  }

  public List<CategoryDto.CategoryWithProductsResponse> getHomeCategories() {
    List<ProductType> rootCategories = productTypeRepository.findRootCategories();

    return rootCategories.stream()
        .map(
            cat -> {
              CategoryDto.CategoryWithProductsResponse response =
                  new CategoryDto.CategoryWithProductsResponse();
              response.setId(cat.getId());
              response.setName(cat.getName());
              response.setCode(cat.getCode());
              response.setDescription(cat.getDescription());
              response.setIconUrl(cat.getIconUrl());

              // Get top 8 products for this category based on newest
              Pageable pageable = PageRequest.of(0, 8, Sort.by(Sort.Direction.DESC, "createdAt"));
              Page<Product> productsPage =
                  productRepository.findByProductTypeIdAndIsActiveTrue(cat.getId(), pageable);

              response.setProducts(
                  productsPage.getContent().stream()
                      .map(this::mapProductToResponse)
                      .collect(Collectors.toList()));

              return response;
            })
        .collect(Collectors.toList());
  }

  public CategoryDto.Response createCategory(CategoryDto.CreateRequest createRequest) {
    log.info("Creating new category: {}", createRequest.getName());
    String code = createRequest.getCode();
    if (code == null || code.trim().isEmpty()) {
      code = generateSlug(createRequest.getName());
    }

    if (productTypeRepository.existsByCode(code)) {
      throw new BadRequestException("Mã danh mục đã tồn tại: " + code);
    }

    ProductType productType = new ProductType();
    productType.setName(createRequest.getName());
    productType.setCode(code);
    productType.setDescription(createRequest.getDescription());
    productType.setIconUrl(createRequest.getIconUrl());
    productType.setIsActive(true);
    productType.setDisplayOrder(
        createRequest.getDisplayOrder() != null ? createRequest.getDisplayOrder() : 0);

    if (createRequest.getParentId() != null) {
      ProductType parent =
          productTypeRepository
              .findById(createRequest.getParentId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException("Category", "id", createRequest.getParentId()));
      productType.setParent(parent);
    }

    ProductType saved = productTypeRepository.save(productType);
    return mapToResponse(saved);
  }

  public CategoryDto.Response updateCategory(Long id, CategoryDto.UpdateRequest updateRequest) {
    log.info("Updating category ID: {}", id);
    ProductType productType =
        productTypeRepository
            .findById(id.intValue())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

    if (updateRequest.getName() != null) {
      productType.setName(updateRequest.getName());
    }
    if (updateRequest.getCode() != null && !updateRequest.getCode().trim().isEmpty()) {
      if (!updateRequest.getCode().equals(productType.getCode())
          && productTypeRepository.existsByCode(updateRequest.getCode())) {
        throw new BadRequestException("Mã danh mục đã tồn tại: " + updateRequest.getCode());
      }
      productType.setCode(updateRequest.getCode());
    }
    if (updateRequest.getDescription() != null) {
      productType.setDescription(updateRequest.getDescription());
    }
    if (updateRequest.getIconUrl() != null) {
      productType.setIconUrl(updateRequest.getIconUrl());
    }
    if (updateRequest.getDisplayOrder() != null) {
      productType.setDisplayOrder(updateRequest.getDisplayOrder());
    }
    if (updateRequest.getParentId() != null) {
      if (updateRequest.getParentId().equals(id.intValue())) {
        throw new BadRequestException("Danh mục không thể là cha của chính nó.");
      }

      ProductType parent =
          productTypeRepository
              .findById(updateRequest.getParentId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException("Category", "id", updateRequest.getParentId()));

      if (isChildOf(parent, id.intValue())) {
        throw new BadRequestException(
            "Lỗi cấu trúc cha-con: Danh mục được chọn là con của danh mục hiện tại.");
      }

      productType.setParent(parent);
    }

    ProductType updated = productTypeRepository.save(productType);
    return mapToResponse(updated);
  }

  private boolean isChildOf(ProductType parent, Integer targetId) {
    if (parent == null) return false;
    if (parent.getId().equals(targetId)) return true;
    return isChildOf(parent.getParent(), targetId);
  }

  public void deleteCategory(Long id) {
    log.warn("Deactivating category ID: {}", id);
    ProductType productType =
        productTypeRepository
            .findById(id.intValue())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

    Long productCount = productTypeRepository.countProductsByProductTypeId(id.intValue());
    if (productCount > 0) {
      throw new BadRequestException(
          "Không thể xoá danh mục đang chứa " + productCount + " sản phẩm đang kinh doanh.");
    }

    deactivateRecursively(productType);
  }

  public CategoryDto.Response toggleStatus(Long id) {
    ProductType productType =
        productTypeRepository
            .findById(id.intValue())
            .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

    if (Boolean.TRUE.equals(productType.getIsActive())) {
      deleteCategory(id);
    } else {
      log.info("Activating category ID: {}", id);
      productType.setIsActive(true);
      productTypeRepository.save(productType);
    }

    return mapToResponse(productType);
  }

  private void deactivateRecursively(ProductType productType) {
    productType.setIsActive(false);
    productTypeRepository.save(productType);

    List<ProductType> children = productTypeRepository.findByParentId(productType.getId());
    for (ProductType child : children) {
      deactivateRecursively(child);
    }
  }

  private String generateSlug(String input) {
    if (input == null || input.isEmpty()) return "";
    String slug =
        input
            .toLowerCase()
            .replaceAll("đ", "d")
            .replaceAll("[àáảãạâầấẩẫậăằắẳẵặ]", "a")
            .replaceAll("[èéẻẽẹêềếểễệ]", "e")
            .replaceAll("[ìíỉĩị]", "i")
            .replaceAll("[òóỏõọôồốổỗộơờớởỡợ]", "o")
            .replaceAll("[ùúủũụưừứửữự]", "u")
            .replaceAll("[ỳýỷỹỵ]", "y")
            .replaceAll("[^a-z0-9\\s]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");
    return slug;
  }

  public List<CategoryDto.Response> getSubCategories(Long parentId) {
    return productTypeRepository.findByParentId(parentId.intValue()).stream()
        .map(this::mapToResponse)
        .collect(Collectors.toList());
  }

  public Page<CategoryDto.Response> searchCategories(String keyword, Pageable pageable) {
    return productTypeRepository.searchByName(keyword, pageable).map(this::mapToResponse);
  }

  public List<ProductDto.VariantDto> getVariantsByCategory(Long categoryId) {
    productTypeRepository
        .findById(categoryId.intValue())
        .orElseThrow(() -> new ResourceNotFoundException("Category", "id", categoryId));

    return productVariantRepository.findByCategoryId(categoryId.intValue()).stream()
        .map(this::mapVariantToDto)
        .collect(Collectors.toList());
  }

  private ProductDto.VariantDto mapVariantToDto(ProductVariant variant) {
    ProductDto.VariantDto dto = new ProductDto.VariantDto();
    dto.setId(variant.getId());
    if (variant.getProduct() != null) {
      dto.setProductId(variant.getProduct().getId());
      dto.setProductName(variant.getProduct().getName());
    }
    dto.setSkuCode(variant.getSkuCode());
    dto.setVariantName(variant.getVariantName());
    dto.setPrice(variant.getPrice() != null ? variant.getPrice().doubleValue() : 0.0);
    dto.setStockQuantity(variant.getStockQuantity());
    dto.setIsActive(variant.getIsActive());

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
      dto.setAttributeValues(attrValues);
    }

    return dto;
  }

  private ProductDto.Response mapProductToResponse(Product product) {
    ProductDto.Response response = modelMapper.map(product, ProductDto.Response.class);
    response.setPrice(product.getBasePrice() != null ? product.getBasePrice().doubleValue() : 0.0);
    response.setStatus(product.getIsActive() != null && product.getIsActive() ? "1" : "0");

    if (product.getImages() != null && !product.getImages().isEmpty()) {
      response.setImages(
          product.getImages().stream()
              .map(
                  img -> {
                    ProductDto.ImageDto imgDto = new ProductDto.ImageDto();
                    imgDto.setId(img.getId());
                    imgDto.setLinkImage(serverUrl + "/img/" + img.getId());
                    imgDto.setVariantId(img.getVariant() != null ? img.getVariant().getId() : null);
                    return imgDto;
                  })
              .collect(Collectors.toList()));
    }

    return response;
  }

  private CategoryDto.Response mapToResponse(ProductType productType) {
    CategoryDto.Response response = new CategoryDto.Response();
    response.setId(productType.getId());
    response.setName(productType.getName());
    response.setCode(productType.getCode());
    response.setDescription(productType.getDescription());
    response.setIconUrl(productType.getIconUrl());
    response.setIsActive(productType.getIsActive());
    response.setDisplayOrder(productType.getDisplayOrder());

    if (productType.getParent() != null) {
      CategoryDto.Response parentResponse = new CategoryDto.Response();
      parentResponse.setId(productType.getParent().getId());
      parentResponse.setName(productType.getParent().getName());
      parentResponse.setCode(productType.getParent().getCode());
      response.setParentCategory(parentResponse);
    }

    if (productType.getChildren() != null && !productType.getChildren().isEmpty()) {
      List<CategoryDto.Response> children =
          productType.getChildren().stream()
              .filter(c -> Boolean.TRUE.equals(c.getIsActive()))
              .map(
                  child -> {
                    CategoryDto.Response childResponse = new CategoryDto.Response();
                    childResponse.setId(child.getId());
                    childResponse.setName(child.getName());
                    childResponse.setCode(child.getCode());
                    return childResponse;
                  })
              .collect(Collectors.toList());
      response.setSubCategories(children);
    } else {
      response.setSubCategories(new ArrayList<>());
    }

    response.setProductCount(
        productTypeRepository.countProductsByProductTypeId(productType.getId()));

    return response;
  }
}
