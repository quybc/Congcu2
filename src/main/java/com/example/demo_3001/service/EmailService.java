package com.example.demo_3001.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.auth.otp.expire-minutes:2}")
    private long otpExpireMinutes;

    public void sendVerificationCode(String toEmail, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("Mã xác thực đăng ký tài khoản");
        if (!fromEmail.isBlank()) {
            message.setFrom(fromEmail);
        }
        message.setText("Mã xác thực của bạn là: " + code + "\nMã có hiệu lực trong " + otpExpireMinutes + " phút.");
        mailSender.send(message);
    }
}
