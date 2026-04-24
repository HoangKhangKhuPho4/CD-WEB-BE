package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.UserDto;
import com.cdweb.be.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasAuthority('USER_MANAGE')")
@CrossOrigin(origins = "*")
public class AdminUserController {

  @Autowired private UserService userService;

  /**
   * GET /api/admin/users Lấy danh sách người dùng (hiển thị cho Admin), cho phép phân trang, tìm
   * kiếm và sắp xếp.
   */
  @GetMapping
  public ResponseEntity<ApiResponse<Page<UserDto.Response>>> getAllUsers(
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(required = false) String keyword,
      @RequestParam(defaultValue = "createdAt") String sortBy,
      @RequestParam(defaultValue = "desc") String sortDir) {

    Sort sort =
        sortDir.equalsIgnoreCase("asc")
            ? Sort.by(sortBy).ascending()
            : Sort.by(sortBy).descending();
    Pageable pageable = PageRequest.of(page, size, sort);

    Page<UserDto.Response> responsePage = userService.getAllUsersForAdmin(keyword, pageable);
    return ResponseEntity.ok(
        ApiResponse.success("Lấy danh sách người dùng thành công", responsePage));
  }

  /** GET /api/admin/users/{id} Lấy thông tin chi tiết của một người dùng bất kỳ */
  @GetMapping("/{id}")
  public ResponseEntity<ApiResponse<UserDto.Response>> getUserById(@PathVariable Integer id) {
    UserDto.Response response = userService.getUserById(id);
    return ResponseEntity.ok(ApiResponse.success("Lấy thông tin người dùng thành công", response));
  }

  /** POST /api/admin/users Admin tạo mới một người dùng (Nhân viên hoặc Khách hàng) */
  @PostMapping
  public ResponseEntity<ApiResponse<UserDto.Response>> createUser(
      @Valid @RequestBody UserDto.CreateRequest request) {
    UserDto.Response response = userService.createUser(request);
    return ResponseEntity.ok(ApiResponse.success("Tạo người dùng thành công", response));
  }

  /** PUT /api/admin/users/{id} Admin cập nhật thông tin người dùng */
  @PutMapping("/{id}")
  public ResponseEntity<ApiResponse<UserDto.Response>> updateUser(
      @PathVariable Integer id, @Valid @RequestBody UserDto.UpdateRequest updateRequest) {
    UserDto.Response response = userService.updateUser(id, updateRequest);
    return ResponseEntity.ok(ApiResponse.success("Cập nhật người dùng thành công", response));
  }

  /** DELETE /api/admin/users/{id} Admin xóa (soft delete) người dùng */
  @DeleteMapping("/{id}")
  public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Integer id) {
    userService.deleteUser(id);
    return ResponseEntity.ok(ApiResponse.success("Xóa người dùng thành công", null));
  }

  /**
   * PUT /api/admin/users/{id}/status Bật hoặc tắt trạng thái hoạt động (block/unblock) của người
   * dùng.
   */
  @PutMapping("/{id}/status")
  public ResponseEntity<ApiResponse<UserDto.Response>> toggleStatus(@PathVariable Integer id) {
    UserDto.Response response = userService.toggleStatus(id);
    String msg =
        response.getStatus() == 1
            ? "Đã mở khoá tài khoản thành công"
            : "Đã khoá tài khoản thành công";
    return ResponseEntity.ok(ApiResponse.success(msg, response));
  }
}
