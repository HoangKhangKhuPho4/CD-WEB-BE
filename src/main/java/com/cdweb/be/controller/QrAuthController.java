package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.UserDto;
import com.cdweb.be.entity.QrAuthToken;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.repository.QrAuthTokenRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.util.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor; // Dùng cho Constructor Injection
import org.modelmapper.ModelMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/qr")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor // Tự động inject các final field
public class QrAuthController {

  private final QrAuthTokenRepository qrRepo;
  private final UserRepository userRepository;
  private final JwtTokenProvider jwtTokenProvider;
  private final ModelMapper modelMapper;

  // ----- [API 1] Máy tính xin cấp mã QR ------
  @GetMapping("/generate")
  public ResponseEntity<ApiResponse<Map<String, String>>> generateQRToken() {
    QrAuthToken qrcode = new QrAuthToken();
    qrcode.setToken(java.util.UUID.randomUUID().toString());
    qrcode.setStatus("PENDING");
    qrcode.setExpiryDate(LocalDateTime.now().plusMinutes(2)); // Hết hạn sau 2 phút

    qrRepo.save(qrcode);

    Map<String, String> response = new HashMap<>();
    response.put("qrToken", qrcode.getToken());
    response.put("expiresAt", qrcode.getExpiryDate().toString());

    return ResponseEntity.ok(ApiResponse.success("Mã QR đã được tạo", response));
  }

  // ----- [API 2] Điện thoại xác nhận đăng nhập ------
  @PostMapping("/verify")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<ApiResponse<String>> verifyQRToken(@RequestParam("token") String token) {
    // 🛡️ BẢO MẬT: Lấy username từ Token của Điện thoại đang quét
    String currentUsername = SecurityContextHolder.getContext().getAuthentication().getName();

    User user = userRepository.findByUsernameOrEmail(currentUsername, currentUsername)
            .orElseThrow(() -> new BadRequestException("Người dùng không tồn tại"));

    Optional<QrAuthToken> qrOpt = qrRepo.findByTokenAndExpiryDateAfter(token, LocalDateTime.now());

    if (qrOpt.isEmpty()) {
      throw new BadRequestException("Mã QR này không tồn tại hoặc đã hết hạn!");
    }

    QrAuthToken qrCode = qrOpt.get();
    if ("VERIFIED".equals(qrCode.getStatus())) {
      throw new BadRequestException("Mã QR này đã được sử dụng!");
    }

    // Gán user (kiểu Long) vào QR token
    qrCode.setUser(user);
    qrCode.setStatus("VERIFIED");
    qrRepo.save(qrCode);

    return ResponseEntity.ok(ApiResponse.success("Xác thực thành công! Máy tính của bạn sẽ tự động đăng nhập.", null));
  }

  // ----- [API 3] Máy tính Polling hỏi trạng thái ------
  @GetMapping("/status/{token}")
  public ResponseEntity<ApiResponse<Map<String, Object>>> checkQRStatus(@PathVariable("token") String token) {
    QrAuthToken qrCode = qrRepo.findByToken(token)
            .orElseThrow(() -> new BadRequestException("Mã QR không hợp lệ!"));

    Map<String, Object> response = new HashMap<>();
    response.put("status", qrCode.getStatus());

    // Nếu điện thoại đã VERIFIED -> Cấp Access Token cho Máy tính
    if ("VERIFIED".equals(qrCode.getStatus()) && qrCode.getUser() != null) {
      String jwt = jwtTokenProvider.generateTokenFromUsername(qrCode.getUser().getUsername());

      // Map sang DTO (Lúc này UserDto.Response đã có fullName và Long id)
      UserDto.Response userResponse = modelMapper.map(qrCode.getUser(), UserDto.Response.class);

      response.put("accessToken", jwt);
      response.put("user", userResponse);
    }

    return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái thành công", response));
  }
}