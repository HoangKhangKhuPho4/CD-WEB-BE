package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.AttributeDto;
import com.cdweb.be.dto.AttributeValueDto;
import com.cdweb.be.service.AttributeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminAttributeController {

  @Autowired private AttributeService attributeService;

  // ==========================================
  // ATTRIBUTES
  // ==========================================

  @GetMapping("/attributes")
  public ResponseEntity<ApiResponse<List<AttributeDto.Response>>> getAllAttributes() {
    List<AttributeDto.Response> attributes = attributeService.getAllAttributes();
    return ResponseEntity.ok(ApiResponse.success("Attributes retrieved successfully", attributes));
  }

  @GetMapping("/attributes/{id}")
  public ResponseEntity<ApiResponse<AttributeDto.Response>> getAttributeById(
      @PathVariable Integer id) {
    AttributeDto.Response attribute = attributeService.getAttributeById(id);
    return ResponseEntity.ok(ApiResponse.success("Attribute retrieved successfully", attribute));
  }

  @PostMapping("/attributes")
  public ResponseEntity<ApiResponse<AttributeDto.Response>> createAttribute(
      @Valid @RequestBody AttributeDto.CreateRequest request) {
    AttributeDto.Response attribute = attributeService.createAttribute(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Attribute created successfully", attribute));
  }

  @PutMapping("/attributes/{id}")
  public ResponseEntity<ApiResponse<AttributeDto.Response>> updateAttribute(
      @PathVariable Integer id, @Valid @RequestBody AttributeDto.UpdateRequest request) {
    AttributeDto.Response attribute = attributeService.updateAttribute(id, request);
    return ResponseEntity.ok(ApiResponse.success("Attribute updated successfully", attribute));
  }

  @DeleteMapping("/attributes/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteAttribute(@PathVariable Integer id) {
    attributeService.deleteAttribute(id);
    return ResponseEntity.ok(ApiResponse.success("Attribute deleted successfully", null));
  }

  // ==========================================
  // ATTRIBUTE VALUES
  // ==========================================

  @GetMapping("/attribute-values")
  public ResponseEntity<ApiResponse<List<AttributeValueDto.Response>>> getAllAttributeValues() {
    List<AttributeValueDto.Response> attributeValues = attributeService.getAllAttributeValues();
    return ResponseEntity.ok(
        ApiResponse.success("Attribute values retrieved successfully", attributeValues));
  }

  @GetMapping("/attributes/{attributeId}/values")
  public ResponseEntity<ApiResponse<List<AttributeValueDto.Response>>>
      getAttributeValuesByAttributeId(@PathVariable Integer attributeId) {
    List<AttributeValueDto.Response> attributeValues =
        attributeService.getAttributeValuesByAttributeId(attributeId);
    return ResponseEntity.ok(
        ApiResponse.success("Attribute values retrieved successfully", attributeValues));
  }

  @PostMapping("/attribute-values")
  public ResponseEntity<ApiResponse<AttributeValueDto.Response>> createAttributeValue(
      @Valid @RequestBody AttributeValueDto.CreateRequest request) {
    AttributeValueDto.Response attributeValue = attributeService.createAttributeValue(request);
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("Attribute value created successfully", attributeValue));
  }

  @PutMapping("/attribute-values/{id}")
  public ResponseEntity<ApiResponse<AttributeValueDto.Response>> updateAttributeValue(
      @PathVariable Integer id, @Valid @RequestBody AttributeValueDto.CreateRequest request) {
    AttributeValueDto.Response attributeValue = attributeService.updateAttributeValue(id, request);
    return ResponseEntity.ok(
        ApiResponse.success("Attribute value updated successfully", attributeValue));
  }

  @DeleteMapping("/attribute-values/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteAttributeValue(@PathVariable Integer id) {
    attributeService.deleteAttributeValue(id);
    return ResponseEntity.ok(ApiResponse.success("Attribute value deleted successfully", null));
  }
}
