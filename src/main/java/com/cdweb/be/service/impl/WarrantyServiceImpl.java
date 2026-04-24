package com.cdweb.be.service.impl;

import com.cdweb.be.dto.WarrantyDto;
import com.cdweb.be.entity.ProductItem;
import com.cdweb.be.entity.ProductVariant;
import com.cdweb.be.entity.User;
import com.cdweb.be.entity.WarrantyTicket;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.ProductItemRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.repository.WarrantyTicketRepository;
import com.cdweb.be.service.WarrantyService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WarrantyServiceImpl implements WarrantyService {

  @Autowired private ProductItemRepository productItemRepository;

  @Autowired private WarrantyTicketRepository warrantyTicketRepository;

  @Autowired private UserRepository userRepository;

  @Override
  public WarrantyDto.Response updateWarrantyStatus(
      String imeiOrSerial, ProductItem.ProductItemStatus status) {
    Optional<ProductItem> itemOpt =
        productItemRepository.findByImeiOrSerialNumber(imeiOrSerial, imeiOrSerial);
    if (itemOpt.isEmpty()) {
      return WarrantyDto.Response.builder()
          .isValid(false)
          .message("Không tìm thấy thông tin. Vui lòng kiểm tra lại thiết bị.")
          .build();
    }
    ProductItem item = itemOpt.get();
    item.setStatus(status);
    productItemRepository.save(item);

    return checkWarranty(imeiOrSerial);
  }

  @Override
  public WarrantyDto.Response checkWarranty(String imeiOrSerial) {
    Optional<ProductItem> itemOpt =
        productItemRepository.findByImeiOrSerialNumber(imeiOrSerial, imeiOrSerial);

    if (itemOpt.isEmpty()) {
      return WarrantyDto.Response.builder()
          .isValid(false)
          .message("Không tìm thấy thông tin. Vui lòng kiểm tra lại thiết bị.")
          .build();
    }

    ProductItem item = itemOpt.get();
    ProductVariant variant = item.getVariant();

    String imageUrl = null;
    if (variant.getImages() != null && !variant.getImages().isEmpty()) {
      imageUrl = variant.getImages().get(0).getImageUrl();
    } else if (variant.getProduct() != null
        && variant.getProduct().getImages() != null
        && !variant.getProduct().getImages().isEmpty()) {
      imageUrl = variant.getProduct().getImages().get(0).getImageUrl();
    }

    LocalDate endDate = null;
    boolean isValid = false;
    String message = "Thiết bị chính hãng.";

    if (item.getWarrantyStartDate() != null) {
      endDate =
          item.getWarrantyStartDate()
              .plusMonths(item.getWarrantyMonths() != null ? item.getWarrantyMonths() : 12);
      if (LocalDate.now().isAfter(endDate)) {
        isValid = false;
        message = "Thiết bị đã hết hạn bảo hành.";
      } else {
        isValid = true;
        message = "Thiết bị còn trong thời hạn bảo hành.";
      }
    } else {
      if (item.getStatus() == ProductItem.ProductItemStatus.AVAILABLE) {
        isValid = true;
        message = "Thiết bị chính hãng chưa kích hoạt bảo hành.";
      } else {
        isValid = false;
        message = "Không thể xác định thời hạn bảo hành. Máy ở trạng thái: " + item.getStatus();
      }
    }

    return WarrantyDto.Response.builder()
        .productName(variant.getProduct() != null ? variant.getProduct().getName() : "")
        .variantName(variant.getVariantName())
        .imageUrl(imageUrl)
        .imei(item.getImei())
        .serialNumber(item.getSerialNumber())
        .status(item.getStatus())
        .warrantyStartDate(item.getWarrantyStartDate())
        .warrantyEndDate(endDate)
        .warrantyMonths(item.getWarrantyMonths())
        .isValid(isValid)
        .message(message)
        .build();
  }

  // =========================================================================
  // TICKET MANAGEMENT (RMA)
  // =========================================================================

  @Override
  public WarrantyDto.TicketResponse createWarrantyTicket(
      String username, WarrantyDto.TicketRequest request) {
    User user =
        userRepository
            .findByUsernameOrEmail(username, username)
            .orElseThrow(() -> new ResourceNotFoundException("User", "username", username));

    ProductItem productItem =
        productItemRepository
            .findByImeiOrSerialNumber(request.getImeiOrSerial(), request.getImeiOrSerial())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "ProductItem", "IMEI/Serial", request.getImeiOrSerial()));

    // Chuyển trạng thái máy sang IN_REPAIR ngay khi tạo phiếu
    productItem.setStatus(ProductItem.ProductItemStatus.IN_REPAIR);
    productItemRepository.save(productItem);

    WarrantyTicket ticket = new WarrantyTicket();
    ticket.setProductItem(productItem);
    ticket.setCustomerName(request.getCustomerName());
    ticket.setCustomerPhone(request.getCustomerPhone());
    ticket.setIssueDescription(request.getIssueDescription());
    ticket.setCreatedBy(user);
    ticket.setStatus(WarrantyTicket.TicketStatus.PENDING);

    // Generate Ticket Code: WR-YYYYMMDD-001
    String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
    String prefix = "WR-" + dateStr + "-";
    String maxCode = warrantyTicketRepository.findMaxTicketCodeWithPrefix(prefix);
    int seq = 1;
    if (maxCode != null && maxCode.length() > prefix.length()) {
      try {
        seq = Integer.parseInt(maxCode.substring(prefix.length())) + 1;
      } catch (NumberFormatException ignored) {
      }
    }
    ticket.setTicketCode(prefix + String.format("%03d", seq));

    WarrantyTicket savedTicket = warrantyTicketRepository.save(ticket);
    return mapToTicketResponse(savedTicket);
  }

  @Override
  public WarrantyDto.TicketResponse getTicketById(Integer id) {
    WarrantyTicket ticket =
        warrantyTicketRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("WarrantyTicket", "id", id));
    return mapToTicketResponse(ticket);
  }

  @Override
  public WarrantyDto.TicketResponse updateTicketStatus(
      Integer id, WarrantyDto.TicketUpdateAdminRequest request) {
    WarrantyTicket ticket =
        warrantyTicketRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("WarrantyTicket", "id", id));

    WarrantyTicket.TicketStatus newStatus;
    try {
      newStatus = WarrantyTicket.TicketStatus.valueOf(request.getStatus().toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new BadRequestException(
          "Trạng thái phiếu bảo hành không hợp lệ: " + request.getStatus());
    }

    ticket.setStatus(newStatus);

    if (request.getTechnicianNote() != null) {
      ticket.setTechnicianNote(request.getTechnicianNote());
    }
    if (request.getRepairCost() != null) {
      ticket.setRepairCost(request.getRepairCost());
    }

    // Nếu hoàn thành sửa chữa, cần update `resolvedAt`
    if (newStatus == WarrantyTicket.TicketStatus.COMPLETED && ticket.getResolvedAt() == null) {
      ticket.setResolvedAt(LocalDateTime.now());
    }

    // Nếu đã trả máy cho khách
    if (newStatus == WarrantyTicket.TicketStatus.RETURNED && ticket.getReturnedAt() == null) {
      ticket.setReturnedAt(LocalDateTime.now());
      // Trả máy lại cho khách thì chuyển trạng thái thiết bị lại là SOLD
      ProductItem item = ticket.getProductItem();
      item.setStatus(ProductItem.ProductItemStatus.SOLD);
      productItemRepository.save(item);
    }

    // Nếu hủy phiếu -> Trả trạng thái thiết bị lại SOLD (giả sử trước đó đã lấy máy lên sửa)
    if (newStatus == WarrantyTicket.TicketStatus.CANCELLED) {
      ProductItem item = ticket.getProductItem();
      if (item.getStatus() == ProductItem.ProductItemStatus.IN_REPAIR) {
        item.setStatus(ProductItem.ProductItemStatus.SOLD);
        productItemRepository.save(item);
      }
    }

    WarrantyTicket updatedTicket = warrantyTicketRepository.save(ticket);
    return mapToTicketResponse(updatedTicket);
  }

  @Override
  public Page<WarrantyDto.TicketResponse> getAllTickets(
      String keyword, String status, Pageable pageable) {
    WarrantyTicket.TicketStatus ticketStatus = null;
    if (status != null && !status.trim().isEmpty()) {
      try {
        ticketStatus = WarrantyTicket.TicketStatus.valueOf(status.trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        ticketStatus = null; // Reset to null to ensure all tickets are returned on invalid input
      }
    }

    Page<WarrantyTicket> tickets =
        warrantyTicketRepository.searchTickets(keyword, ticketStatus, pageable);
    return tickets.map(this::mapToTicketResponse);
  }

  private WarrantyDto.TicketResponse mapToTicketResponse(WarrantyTicket ticket) {
    ProductItem item = ticket.getProductItem();
    ProductVariant variant = item.getVariant();

    String statusDisplay =
        switch (ticket.getStatus()) {
          case PENDING -> "Chờ kiểm tra";
          case IN_PROGRESS -> "Đang sửa chữa";
          case COMPLETED -> "Đã sửa xong";
          case CANCELLED -> "Đã hủy";
          case RETURNED -> "Đã biên nhận trả khách";
        };

    return WarrantyDto.TicketResponse.builder()
        .id(ticket.getId())
        .ticketCode(ticket.getTicketCode())
        .imei(item.getImei())
        .serialNumber(item.getSerialNumber())
        .productName(variant.getProduct() != null ? variant.getProduct().getName() : "")
        .variantName(variant.getVariantName())
        .customerName(ticket.getCustomerName())
        .customerPhone(ticket.getCustomerPhone())
        .issueDescription(ticket.getIssueDescription())
        .technicianNote(ticket.getTechnicianNote())
        .status(ticket.getStatus().name())
        .statusDisplay(statusDisplay)
        .repairCost(ticket.getRepairCost())
        .receivedAt(ticket.getReceivedAt())
        .resolvedAt(ticket.getResolvedAt())
        .returnedAt(ticket.getReturnedAt())
        .createdBy(ticket.getCreatedBy() != null ? ticket.getCreatedBy().getUsername() : "")
        .build();
  }
}
