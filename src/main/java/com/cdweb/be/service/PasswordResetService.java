package com.cdweb.be.service;

import com.cdweb.be.dto.UserDto;

public interface PasswordResetService {
  void createPasswordResetToken(UserDto.ForgotPasswordRequest request);

  void resetPassword(UserDto.ResetPasswordRequest request);
}
