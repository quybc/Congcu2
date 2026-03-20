package com.example.demo_3001.controller;

import com.example.demo_3001.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthenticationManager authenticationManager;

    @GetMapping("/register")
    public String showRegisterForm() {
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String username,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           HttpServletRequest request,
                           Model model) {
        keepRegisterInput(model, username, email);
        if (!password.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp");
            return "auth/register";
        }

        AuthService.RegisterResult result = authService.registerUser(username, password, email);
        if (result == AuthService.RegisterResult.USERNAME_EXISTS) {
            model.addAttribute("error", "Tên đăng nhập đã tồn tại");
            return "auth/register";
        }
        if (result == AuthService.RegisterResult.EMAIL_EXISTS) {
            model.addAttribute("error", "Gmail đã được sử dụng");
            return "auth/register";
        }
        if (result == AuthService.RegisterResult.INVALID_INPUT) {
            model.addAttribute("error", "Thông tin đăng ký không hợp lệ");
            return "auth/register";
        }

        String normalizedUsername = username == null ? "" : username.trim();
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(normalizedUsername, password)
        );
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        request.getSession(true).setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);
        return "redirect:/";
    }

    @GetMapping("/login")
    public String showLoginForm() {
        return "auth/login";
    }

    @GetMapping("/403")
    public String accessDenied() {
        return "403";
    }

    private void keepRegisterInput(Model model, String username, String email) {
        model.addAttribute("username", username);
        model.addAttribute("email", email);
    }
}
