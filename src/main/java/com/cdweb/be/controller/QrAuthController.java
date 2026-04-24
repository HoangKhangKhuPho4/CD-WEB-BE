package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.UserDto;
import com.cdweb.be.entity.QrAuthToken;
import com.cdweb.be.entity.User;
import com.cdweb.be.repository.QrAuthTokenRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.util.JwtTokenProvider;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/qr")
@CrossOrigin(origins = "*")
public class QrAuthController {

  @Autowired private QrAuthTokenRepository qrRepo;

  @Autowired private UserRepository userRepository;

  @Autowired private JwtTokenProvider jwtTokenProvider;

  @Autowired private ModelMapper modelMapper;

  // ----- [API 1] Màn hình Máy tính xin cấp quyền in Mã QR Trắng ------
  @GetMapping("/generate")
  public ResponseEntity<ApiResponse<Map<String, String>>> generateQRToken() {
    QrAuthToken qrcode = new QrAuthToken();
    qrRepo.save(qrcode); // Database tự sinh ra UUID và trạng thái PENDING

    Map<String, String> response = new HashMap<>();
    response.put("qrToken", qrcode.getToken());
    response.put("expiresAt", qrcode.getExpiryDate().toString());

    return ResponseEntity.ok(ApiResponse.success("Success", response));
  }

  // ----- [API 2] Điện thoại Quét & Bấm "Tôi muốn Đăng nhập vô PC này" ------
  @PostMapping("/verify")
  @PreAuthorize("isAuthenticated()") // Chỉ user đã login trên điện thoại mới được xác thực QR
  public ResponseEntity<ApiResponse<String>> verifyQRToken(
      @RequestParam("token") String token, @RequestParam("userId") Integer userId) {
    Optional<QrAuthToken> qrOpt = qrRepo.findByTokenAndExpiryDateAfter(token, LocalDateTime.now());

    if (qrOpt.isEmpty()) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Mã QR này không tồn tại hoặc đã hết hạn (Quá 2 phút)!"));
    }

    QrAuthToken qrCode = qrOpt.get();
    if ("VERIFIED".equals(qrCode.getStatus())) {
      return ResponseEntity.badRequest()
          .body(ApiResponse.error("Mã QR này đã bị quét bởi ai đó rồi!"));
    }

    User user = userRepository.findById(userId).orElse(null);
    if (user == null) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Người dùng không tồn tại"));
    }

    qrCode.setUser(user);
    qrCode.setStatus("VERIFIED"); // Mở khóa
    qrRepo.save(qrCode);

    return ResponseEntity.ok(ApiResponse.success("Xác thực vào Máy Tính thành công!", null));
  }

  // ----- [API 3] Máy tính liên tục Polling hỏi trạng thái ------
  @GetMapping("/status/{token}")
  public ResponseEntity<?> checkQRStatus(@PathVariable("token") String token) {
    Optional<QrAuthToken> qrOpt = qrRepo.findByToken(token);

    if (qrOpt.isEmpty()) {
      return ResponseEntity.badRequest().body(ApiResponse.error("Token QR không hợp lệ!"));
    }

    QrAuthToken qrCode = qrOpt.get();
    Map<String, Object> response = new HashMap<>();

    response.put("status", qrCode.getStatus()); // 'PENDING' hoặc 'VERIFIED'

    // Cú Magic: Nếu trạng thái đã mở cửa -> Giao thẳng vé JWT cho PC
    if ("VERIFIED".equals(qrCode.getStatus()) && qrCode.getUser() != null) {
      String jwt = jwtTokenProvider.generateTokenFromUsername(qrCode.getUser().getUsername());
      UserDto.Response userResponse = modelMapper.map(qrCode.getUser(), UserDto.Response.class);

      response.put("accessToken", jwt);
      response.put("user", userResponse); // Thêm user cho Frontend ReactJS dùng luôn
    }

    return ResponseEntity.ok(ApiResponse.success("Lấy trạng thái thành công", response));
  }
}
