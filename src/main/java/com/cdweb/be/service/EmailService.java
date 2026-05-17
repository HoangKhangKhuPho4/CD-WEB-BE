package com.cdweb.be.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired private JavaMailSender mailSender;

    @Async
    public void sendOrderStatusEmail(String to, String customerName, String orderId, String status) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Cập nhật đơn hàng #" + orderId + " – " + statusLabel(status));
            helper.setText(buildOrderStatusHtml(customerName, orderId, status), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send order status email: " + e.getMessage());
        }
    }

    @Async
    public void sendPasswordResetEmail(String to, String name, String resetLink) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject("Khôi phục mật khẩu – CD Web");
            helper.setText(buildPasswordResetHtml(name, resetLink), true);
            mailSender.send(message);
        } catch (MessagingException e) {
            System.err.println("Failed to send password reset email: " + e.getMessage());
        }
    }

    private String statusLabel(String status) {
        return switch (status) {
            case "confirmed"  -> "Đã xác nhận";
            case "shipping"   -> "Đang giao hàng";
            case "delivered"  -> "Giao hàng thành công";
            case "cancelled"  -> "Đã hủy";
            default           -> "Đang xử lý";
        };
    }

    private String buildOrderStatusHtml(String name, String orderId, String status) {
        String color = "cancelled".equals(status) ? "#e74c3c" : "#27ae60";
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="color: #3C50E0;">CD Web – Cập nhật đơn hàng</h2>
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Đơn hàng <strong>#%s</strong> của bạn đã được cập nhật:</p>
              <div style="background: #f5f5f5; border-left: 4px solid %s; padding: 16px; margin: 16px 0; border-radius: 4px;">
                <strong style="color: %s; font-size: 18px;">%s</strong>
              </div>
              <p>Bạn có thể theo dõi đơn hàng trong mục <strong>Lịch sử đơn hàng</strong> trên website.</p>
              <hr style="margin: 24px 0; border: none; border-top: 1px solid #eee;" />
              <p style="color: #888; font-size: 12px;">Email này được gửi tự động, vui lòng không trả lời.</p>
            </div>
            """.formatted(name, orderId, color, color, statusLabel(status));
    }

    private String buildPasswordResetHtml(String name, String resetLink) {
        return """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: 0 auto; padding: 24px;">
              <h2 style="color: #3C50E0;">CD Web – Khôi phục mật khẩu</h2>
              <p>Xin chào <strong>%s</strong>,</p>
              <p>Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản của bạn.</p>
              <p>Nhấn vào nút bên dưới để đặt lại mật khẩu (có hiệu lực trong <strong>60 phút</strong>):</p>
              <a href="%s" style="display: inline-block; background: #3C50E0; color: white; padding: 12px 24px;
                 border-radius: 6px; text-decoration: none; margin: 16px 0; font-weight: bold;">
                Đặt lại mật khẩu
              </a>
              <p>Nếu bạn không yêu cầu điều này, hãy bỏ qua email này.</p>
              <hr style="margin: 24px 0; border: none; border-top: 1px solid #eee;" />
              <p style="color: #888; font-size: 12px;">Email này được gửi tự động, vui lòng không trả lời.</p>
            </div>
            """.formatted(name, resetLink);
    }
}