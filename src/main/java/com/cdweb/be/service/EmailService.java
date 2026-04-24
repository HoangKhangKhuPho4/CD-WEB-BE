package com.cdweb.be.service;

public interface EmailService {
  void sendResetPasswordEmail(String toEmail, String resetLink);
}
