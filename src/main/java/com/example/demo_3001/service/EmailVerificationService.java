package com.example.demo_3001.service;

import com.example.demo_3001.model.EmailVerificationCode;
import com.example.demo_3001.repository.EmailVerificationCodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[A-Za-z0-9+_.-]+@gmail\\.com$");

    private final EmailVerificationCodeRepository verificationCodeRepository;
    private final EmailService emailService;

    @Value("${app.auth.otp.expire-minutes:5}")
    private long otpExpireMinutes;

    public boolean isValidGmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email.trim().toLowerCase()).matches();
    }

    @Transactional
    public boolean createAndSendCode(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        String code = String.valueOf(ThreadLocalRandom.current().nextInt(100000, 1000000));

        verificationCodeRepository.deleteByEmail(normalizedEmail);

        EmailVerificationCode record = new EmailVerificationCode();
        record.setEmail(normalizedEmail);
        record.setCode(code);
        record.setCreatedAt(LocalDateTime.now());
        record.setExpiresAt(LocalDateTime.now().plusMinutes(otpExpireMinutes));
        record.setUsed(false);
        verificationCodeRepository.save(record);

        try {
            emailService.sendVerificationCode(normalizedEmail, code);
            return true;
        } catch (Exception ignored) {
            return false;
        }
    }

    @Transactional
    public boolean verifyCode(String email, String code) {
        if (email == null || code == null) {
            return false;
        }

        String normalizedEmail = email.trim().toLowerCase();
        String normalizedCode = code.trim();
        Optional<EmailVerificationCode> recordOpt = verificationCodeRepository
                .findTopByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(normalizedEmail, normalizedCode);

        if (recordOpt.isEmpty()) {
            return false;
        }

        EmailVerificationCode record = recordOpt.get();
        if (record.getExpiresAt().isBefore(LocalDateTime.now())) {
            return false;
        }

        record.setUsed(true);
        verificationCodeRepository.save(record);
        return true;
    }
}
