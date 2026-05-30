package com.cdweb.be.controller.admin;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.UserDto;
import com.cdweb.be.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AdminCustomerController {

  private final UserService userService;

  @GetMapping
  @PreAuthorize("hasAnyAuthority('CUSTOMER_VIEW', 'USER_MANAGE', 'ROLE_ADMIN')")
  public ResponseEntity<ApiResponse<Page<UserDto.Response>>> listCustomers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "15") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {

    Sort sort =
        sortDir.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);
    Page<UserDto.Response> result = userService.getCustomersForStaff(keyword, pageable);
    return ResponseEntity.ok(ApiResponse.success("Lấy danh sách khách hàng thành công", result));
  }
}
