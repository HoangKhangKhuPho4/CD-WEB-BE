package com.cdweb.be.service;

import com.cdweb.be.dto.WarrantyDto;
import com.cdweb.be.entity.ProductItem;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarrantyService {
  WarrantyDto.Response checkWarranty(String imeiOrSerial);

  WarrantyDto.Response updateWarrantyStatus(
      String imeiOrSerial, ProductItem.ProductItemStatus status);

  // Ticket Management
  WarrantyDto.TicketResponse createWarrantyTicket(
      String username, WarrantyDto.TicketRequest request);

  WarrantyDto.TicketResponse getTicketById(Integer id);

  WarrantyDto.TicketResponse updateTicketStatus(
      Integer id, WarrantyDto.TicketUpdateAdminRequest request);

  Page<WarrantyDto.TicketResponse> getAllTickets(String keyword, String status, Pageable pageable);
}
