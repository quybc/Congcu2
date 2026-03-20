package com.example.demo_3001.service;

import com.example.demo_3001.model.AppUser;
import com.example.demo_3001.model.RoleCode;
import com.example.demo_3001.model.UserRole;
import com.example.demo_3001.repository.AppUserRepository;
import com.example.demo_3001.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    public enum RegisterResult {
        INVALID_INPUT,
        USERNAME_EXISTS,
        EMAIL_EXISTS,
        SUCCESS
    }

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public RegisterResult registerUser(String username, String rawPassword, String email) {
        String normalizedUsername = username == null ? "" : username.trim();
        String normalizedEmail = normalizeEmail(email);
        if (normalizedUsername.isEmpty() || normalizedEmail.isEmpty() || rawPassword == null || rawPassword.isBlank()) {
            return RegisterResult.INVALID_INPUT;
        }
        if (appUserRepository.existsByUsername(normalizedUsername)) {
            return RegisterResult.USERNAME_EXISTS;
        }
        if (appUserRepository.existsByEmail(normalizedEmail)) {
            return RegisterResult.EMAIL_EXISTS;
        }

        AppUser appUser = new AppUser();
        appUser.setUsername(normalizedUsername);
        appUser.setPassword(passwordEncoder.encode(rawPassword));
        appUser.setEmail(normalizedEmail);
        appUser.setEnabled(true);
        AppUser savedUser = appUserRepository.save(appUser);

        Long roleId = appUserRepository.count() == 1 ? RoleCode.ADMIN.getId() : RoleCode.USER.getId();
        UserRole userRole = userRoleRepository.findById(savedUser.getId())
                .orElseGet(() -> {
                    UserRole newUserRole = new UserRole();
                    newUserRole.setUser(savedUser);
                    return newUserRole;
                });
        userRole.setRoleId(roleId);
        userRoleRepository.save(userRole);
        return RegisterResult.SUCCESS;
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
