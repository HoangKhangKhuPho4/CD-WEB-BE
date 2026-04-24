package com.cdweb.be.service.impl;

import com.cdweb.be.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

  @Autowired(required = false) // Allow context to load even if configuration is incomplete
  private JavaMailSender mailSender;

  @Value("${spring.mail.username:noreply@electrostore.com}")
  private String fromEmail;

  @Override
  public void sendResetPasswordEmail(String toEmail, String resetLink) {
    if (mailSender == null) {
      System.out.println("⚠️ Email Service is not configured. Reset Link: " + resetLink);
      return;
    }

    try {
      MimeMessage message = mailSender.createMimeMessage();
      MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

      helper.setFrom(fromEmail);
      helper.setTo(toEmail);
      helper.setSubject("[Electro Store] Yêu cầu khôi phục mật khẩu");

      String content =
          "<div style='font-family: Arial, sans-serif; padding: 20px;'>"
              + "<h2>Khôi phục mật khẩu</h2>"
              + "<p>Chào bạn,</p>"
              + "<p>Chúng tôi nhận được yêu cầu khôi phục mật khẩu cho tài khoản của bạn tại <b>Electro Store</b>.</p>"
              + "<p>Vui lòng nhấn vào nút bên dưới để tiến hành đặt lại mật khẩu. Liên kết này sẽ hết hạn sau 15 phút.</p>"
              + "<p><a href='"
              + resetLink
              + "' style='background-color: #4CAF50; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px; display: inline-block;'>Đặt lại mật khẩu</a></p>"
              + "<p>Nếu bạn không gửi yêu cầu này, vui lòng bỏ qua email này.</p>"
              + "<p>Trân trọng,<br>Đội ngũ Electro Store</p>"
              + "</div>";

      helper.setText(content, true);

      mailSender.send(message);
    } catch (MessagingException e) {
      throw new RuntimeException("Không thể gửi email khôi phục mật khẩu: " + e.getMessage());
    }
  }
}
