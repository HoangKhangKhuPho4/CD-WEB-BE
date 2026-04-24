package com.cdweb.be.controller;

import com.cdweb.be.dto.ApiResponse;
import com.cdweb.be.dto.FacebookLoginRequest;
import com.cdweb.be.dto.FacebookUserResponse;
import com.cdweb.be.dto.GoogleLoginRequest;
import com.cdweb.be.dto.UserDto;
import com.cdweb.be.entity.Role;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.repository.RoleRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.service.FacebookAuthService;
import com.cdweb.be.service.GoogleAuthService;
import com.cdweb.be.service.PasswordResetService;
import com.cdweb.be.util.JwtTokenProvider;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.Set;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173")
@Tag(name = "Xác thực (Authentication)", description = "Các API dành cho Đăng ký, Đăng nhập và Quản lý tài khoản")
public class AuthController {

  @Autowired private AuthenticationManager authenticationManager;

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordEncoder passwordEncoder;

  @Autowired private JwtTokenProvider tokenProvider;

  @Autowired private RoleRepository roleRepository;
  @Autowired private com.cdweb.be.repository.AddressRepository addressRepository;

  @Autowired private GoogleAuthService googleAuthService;

  @Autowired private ModelMapper modelMapper;
  @Autowired private FacebookAuthService facebookAuthService;
  @Autowired private PasswordResetService passwordResetService;

  @PostMapping("/login")
  @Operation(summary = "Đăng nhập", description = "Đăng nhập bằng Email và Mật khẩu để nhận JWT Token")
  public ResponseEntity<ApiResponse<UserDto.LoginResponse>> authenticateUser(
      @Valid @RequestBody UserDto.LoginRequest loginRequest) {
    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(
                loginRequest.getUsernameOrEmail(), loginRequest.getPassword()));

    SecurityContextHolder.getContext().setAuthentication(authentication);
    String jwt = tokenProvider.generateToken(authentication);

    User user =
        userRepository
            .findByUsernameOrEmail(
                loginRequest.getUsernameOrEmail(), loginRequest.getUsernameOrEmail())
            .orElseThrow(() -> new BadRequestException("User not found"));

    // Update last login time
    user.setLastLoginAt(java.time.LocalDateTime.now());
    userRepository.save(user);

    UserDto.Response userResponse = modelMapper.map(user, UserDto.Response.class);
    // Lấy địa chỉ mặc định nếu có
    addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId()).stream()
        .findFirst()
        .ifPresent(addr -> userResponse.setAddress(addr.getAddressDetail()));

    UserDto.LoginResponse loginResponse = new UserDto.LoginResponse(jwt, "Bearer", userResponse);
    return ResponseEntity.ok(ApiResponse.success("Login successful", loginResponse));
  }

  @PostMapping("/register")
  @Operation(summary = "Đăng ký tài khoản", description = "Tạo tài khoản mới cho khách hàng")
  @io.swagger.v3.oas.annotations.responses.ApiResponse(
      responseCode = "201",
      description = "Đăng ký thành công",
      content =
          @Content(
              mediaType = "application/json",
              schema = @Schema(implementation = ApiResponse.class)))
  public ResponseEntity<ApiResponse<UserDto.Response>> registerUser(
      @Valid @RequestBody UserDto.CreateRequest signUpRequest) {
    if (userRepository.existsByUsername(signUpRequest.getUsername())) {
      throw new BadRequestException("Username is already taken!");
    }

    if (userRepository.existsByEmail(signUpRequest.getEmail())) {
      throw new BadRequestException("Email Address already in use!");
    }

    // Creating user's account
    User user = new User();
    user.setUsername(signUpRequest.getUsername());
    user.setEmail(signUpRequest.getEmail());
    user.setPassword(passwordEncoder.encode(signUpRequest.getPassword()));
    user.setName(signUpRequest.getName());
    user.setPhone(signUpRequest.getPhone());
    user.setBirth(signUpRequest.getBirth());
    user.setGender(signUpRequest.getGender());
    user.setIsActive(true);

    // Gán role: Nếu có truyền roleId thì lấy theo ID, nếu không mặc định là CUSTOMER (4)
    Integer roleId = (signUpRequest.getRoleId() != null) ? signUpRequest.getRoleId() : 4;
    Role userRole =
        roleRepository
            .findById(roleId)
            .orElseThrow(() -> new BadRequestException("Role ID " + roleId + " not found"));
    user.setRoles(Collections.singleton(userRole));

    User result = userRepository.save(user);

    // Lưu địa chỉ vào bảng user_addresses nếu có
    if (signUpRequest.getAddress() != null && !signUpRequest.getAddress().isBlank()) {
      com.cdweb.be.entity.UserAddress userAddress = new com.cdweb.be.entity.UserAddress();
      userAddress.setUser(result);
      userAddress.setReceiverName(result.getName());
      userAddress.setPhone(result.getPhone());
      userAddress.setAddressDetail(signUpRequest.getAddress());
      userAddress.setIsDefault(true);
      userAddress.setLabel("Nhà riêng");
      addressRepository.save(userAddress);
    }

    UserDto.Response userResponse = modelMapper.map(result, UserDto.Response.class);
    userResponse.setAddress(signUpRequest.getAddress());

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(ApiResponse.success("User registered successfully", userResponse));
  }

  @PostMapping("/check-username")
  public ResponseEntity<ApiResponse<Boolean>> checkUsernameAvailability(
      @RequestParam String username) {
    Boolean isAvailable = !userRepository.existsByUsername(username);
    return ResponseEntity.ok(ApiResponse.success("Username availability checked", isAvailable));
  }

  @PostMapping("/check-email")
  public ResponseEntity<ApiResponse<Boolean>> checkEmailAvailability(@RequestParam String email) {
    Boolean isAvailable = !userRepository.existsByEmail(email);
    return ResponseEntity.ok(ApiResponse.success("Email availability checked", isAvailable));
  }

  @PostMapping("/google")
  public ResponseEntity<ApiResponse<UserDto.LoginResponse>> googleLogin(
      @Valid @RequestBody GoogleLoginRequest request) {
    try {
      GoogleIdToken.Payload payload = googleAuthService.verifyToken(request.getIdToken());

      // 🛡️ Kiểm tra 5% Bảo mật cuối cùng: Đảm bảo Email đã được xác thực ở Google
      if (!payload.getEmailVerified()) {
        throw new BadRequestException("Google account email is not verified!");
      }

      String email = payload.getEmail();
      String name = (String) payload.get("name");
      String googleId = payload.getSubject();

      User user = userRepository.findByEmail(email).orElse(null);
      if (user == null) {
        user = new User();
        user.setUsername(
            email.split("@")[0] + "_" + java.util.UUID.randomUUID().toString().substring(0, 4));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setName(name);
        user.setOauthProvider("GOOGLE");
        user.setOauthUid(googleId);

        Role userRole =
            roleRepository
                .findByName("CUSTOMER")
                .orElseThrow(() -> new BadRequestException("Default CUSTOMER role not found"));
        user.setRoles(Collections.singleton(userRole));
        user.setIsActive(true);
        user = userRepository.save(user);
      } else {
        if (user.getOauthProvider() == null) {
          user.setOauthProvider("GOOGLE");
          user.setOauthUid(googleId);
          userRepository.save(user);
        }
      }

      // Generate JWT using generateTokenFromUsername
      String jwt = tokenProvider.generateTokenFromUsername(user.getUsername());

      user.setLastLoginAt(java.time.LocalDateTime.now());
      userRepository.save(user);

      UserDto.Response userResponse = modelMapper.map(user, UserDto.Response.class);
      UserDto.LoginResponse loginResponse = new UserDto.LoginResponse(jwt, "Bearer", userResponse);

      return ResponseEntity.ok(ApiResponse.success("Google Login successful", loginResponse));
    } catch (BadRequestException e) {
      throw e;
    } catch (Exception e) {
      throw new BadRequestException("Failed to login with Google: " + e.getMessage());
    }
  }

  @PostMapping("/facebook")
  public ResponseEntity<ApiResponse<UserDto.LoginResponse>> facebookLogin(
      @Valid @RequestBody FacebookLoginRequest request) {
    try {
      FacebookUserResponse payload = facebookAuthService.verifyToken(request.getAccessToken());

      String email = payload.getEmail();
      String name = payload.getName();
      String fbId = payload.getId();

      // 🛡️ Xử lý Trường hợp Facebook không trả về Email
      if (email == null || email.isEmpty()) {
        email = fbId + "@facebook.com";
      }

      User user = userRepository.findByEmail(email).orElse(null);
      if (user == null) {
        user = new User();
        user.setUsername(fbId + "_" + java.util.UUID.randomUUID().toString().substring(0, 4));
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(java.util.UUID.randomUUID().toString()));
        user.setName(name != null ? name : "Facebook User");
        user.setOauthProvider("FACEBOOK");
        user.setOauthUid(fbId);

        Role userRole =
            roleRepository
                .findByName("CUSTOMER")
                .orElseThrow(() -> new BadRequestException("Default CUSTOMER role not found"));
        Set<Role> roles = Collections.singleton(userRole);
        user.setRoles(roles);
        user.setIsActive(true);
        user = userRepository.save(user);
      } else {
        if (user.getOauthProvider() == null) {
          user.setOauthProvider("FACEBOOK");
          user.setOauthUid(fbId);
          userRepository.save(user);
        }
      }

      String jwt = tokenProvider.generateTokenFromUsername(user.getUsername());
      user.setLastLoginAt(java.time.LocalDateTime.now());
      userRepository.save(user);

      UserDto.Response userResponse = modelMapper.map(user, UserDto.Response.class);
      UserDto.LoginResponse loginResponse = new UserDto.LoginResponse(jwt, "Bearer", userResponse);

      return ResponseEntity.ok(ApiResponse.success("Facebook Login successful", loginResponse));
    } catch (BadRequestException e) {
      throw e;
    } catch (Exception e) {
      throw new BadRequestException("Failed to login with Facebook: " + e.getMessage());
    }
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(
      @Valid @RequestBody UserDto.ForgotPasswordRequest request) {
    passwordResetService.createPasswordResetToken(request);
    return ResponseEntity.ok(
        ApiResponse.success(
            "Email khôi phục mật khẩu đã được gửi. Vui lòng kiểm tra hộp thư.", null));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @Valid @RequestBody UserDto.ResetPasswordRequest request) {
    passwordResetService.resetPassword(request);
    return ResponseEntity.ok(
        ApiResponse.success("Mật khẩu của bạn đã được cập nhật thành công.", null));
  }
}
