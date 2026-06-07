package com.cdweb.be.service;

import com.cdweb.be.dto.WarrantyDto;
import com.cdweb.be.entity.ProductItem;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarrantyService {
  WarrantyDto.Response checkWarranty(String imeiOrSerial);

  WarrantyDto.LookupResponse lookupByCode(String imeiOrSerial);

  WarrantyDto.Response updateWarrantyStatus(
      String imeiOrSerial, ProductItem.ProductItemStatus status);

  WarrantyDto.StatsResponse getTicketStats();

  WarrantyDto.ValidateTicketResponse validateTicket(WarrantyDto.TicketRequest request);

  byte[] exportTicketsCsv(String keyword, String status, LocalDate fromDate, LocalDate toDate);

  WarrantyDto.TicketResponse createWarrantyTicket(
      String username, WarrantyDto.TicketRequest request);

  WarrantyDto.TicketResponse createPublicWarrantyTicket(WarrantyDto.TicketRequest request);

  WarrantyDto.TicketResponse getTicketById(Integer id);

  WarrantyDto.TicketResponse getTicketByCode(String ticketCode);

  WarrantyDto.TicketResponse updateTicketStatus(
      Integer id, WarrantyDto.TicketUpdateAdminRequest request);

  Page<WarrantyDto.TicketResponse> getAllTickets(
      String keyword, String status, LocalDate fromDate, LocalDate toDate, Pageable pageable);
}
