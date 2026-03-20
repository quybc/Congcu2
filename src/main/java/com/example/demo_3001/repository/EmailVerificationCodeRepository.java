package com.example.demo_3001.repository;

import com.example.demo_3001.model.EmailVerificationCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationCodeRepository extends JpaRepository<EmailVerificationCode, Long> {
    Optional<EmailVerificationCode> findTopByEmailAndCodeAndUsedFalseOrderByCreatedAtDesc(String email, String code);

    void deleteByEmail(String email);
}
