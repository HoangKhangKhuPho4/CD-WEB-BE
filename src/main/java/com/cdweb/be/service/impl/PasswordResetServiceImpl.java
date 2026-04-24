package com.cdweb.be.service.impl;

import com.cdweb.be.dto.UserDto;
import com.cdweb.be.entity.PasswordResetToken;
import com.cdweb.be.entity.User;
import com.cdweb.be.exception.BadRequestException;
import com.cdweb.be.exception.ResourceNotFoundException;
import com.cdweb.be.repository.PasswordResetTokenRepository;
import com.cdweb.be.repository.UserRepository;
import com.cdweb.be.service.EmailService;
import com.cdweb.be.service.PasswordResetService;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PasswordResetServiceImpl implements PasswordResetService {

  @Autowired private UserRepository userRepository;

  @Autowired private PasswordResetTokenRepository tokenRepository;

  @Autowired private EmailService emailService;

  @Autowired private PasswordEncoder passwordEncoder;

  @Value("${app.password-reset.frontend-url}")
  private String frontendUrl;

  @Value("${app.password-reset.expiration-minutes:15}")
  private int expirationMinutes;

  @Override
  public void createPasswordResetToken(UserDto.ForgotPasswordRequest request) {
    User user =
        userRepository
            .findByEmail(request.getEmail())
            .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.getEmail()));

    // Delete any existing unused token for this user to avoid duplication
    tokenRepository.deleteByUser(user);

    // Generate token
    String token = UUID.randomUUID().toString();
    PasswordResetToken resetToken =
        PasswordResetToken.builder()
            .token(token)
            .user(user)
            .expiryDate(LocalDateTime.now().plusMinutes(expirationMinutes))
            .used(false)
            .build();

    tokenRepository.save(resetToken);

    // Send email
    String resetLink = frontendUrl + "?token=" + token;
    emailService.sendResetPasswordEmail(user.getEmail(), resetLink);
  }

  @Override
  public void resetPassword(UserDto.ResetPasswordRequest request) {
    if (!request.getNewPassword().equals(request.getConfirmPassword())) {
      throw new BadRequestException("Mật khẩu xác nhận không khớp!");
    }

    PasswordResetToken resetToken =
        tokenRepository
            .findByToken(request.getToken())
            .orElseThrow(() -> new BadRequestException("Mã khôi phục mật khẩu không hợp lệ!"));

    if (resetToken.isUsed()) {
      throw new BadRequestException("Mã khôi phục mật khẩu đã được sử dụng!");
    }

    if (resetToken.isExpired()) {
      throw new BadRequestException("Mã khôi phục mật khẩu đã hết hạn!");
    }

    User user = resetToken.getUser();
    user.setPassword(passwordEncoder.encode(request.getNewPassword()));
    userRepository.save(user);

    resetToken.setUsed(true);
    tokenRepository.save(resetToken);
  }
}
