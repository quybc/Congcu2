package com.example.demo_3001.controller;

import com.example.demo_3001.model.AppUser;
import com.example.demo_3001.model.Customer;
import com.example.demo_3001.model.Order;
import com.example.demo_3001.model.PromotionVoucher;
import com.example.demo_3001.repository.AppUserRepository;
import com.example.demo_3001.repository.CustomerRepository;
import com.example.demo_3001.repository.CustomerVoucherRepository;
import com.example.demo_3001.repository.OrderRepository;
import com.example.demo_3001.service.CartService;
import com.example.demo_3001.service.EmailVerificationService;
import com.example.demo_3001.service.PromotionVoucherService;
import com.example.demo_3001.service.VoucherService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;
import java.util.List;
import java.util.Locale;

@Controller
@RequiredArgsConstructor
@RequestMapping("/rewards")
public class RewardController {
    private static final String VERIFIED_FLAG = "rewardsRedeemVerified";
    private static final String PENDING_PHONE = "rewardsPendingPhone";
    private static final String PENDING_PROMOTION_ID = "rewardsPendingPromotionId";

    private final AppUserRepository appUserRepository;
    private final EmailVerificationService emailVerificationService;
    private final PromotionVoucherService promotionVoucherService;
    private final VoucherService voucherService;
    private final CustomerRepository customerRepository;
    private final OrderRepository orderRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final CartService cartService;

    @GetMapping
    public String rewardPage(@RequestParam(value = "voucherPhone", required = false) String voucherPhone,
                             Principal principal,
                             HttpSession session,
                             Model model) {
        AppUser appUser = appUserRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String accountEmail = normalize(appUser.getEmail());
        String normalizedPhone = voucherPhone == null ? "" : voucherPhone.trim();

        boolean verified = Boolean.TRUE.equals(session.getAttribute(VERIFIED_FLAG));
        boolean hasPendingOtp = session.getAttribute(PENDING_PHONE) != null && session.getAttribute(PENDING_PROMOTION_ID) != null;

        model.addAttribute("accountEmail", accountEmail);
        model.addAttribute("gmailVerified", verified);
        model.addAttribute("pendingOtp", hasPendingOtp);
        model.addAttribute("voucherPhone", normalizedPhone);
        model.addAttribute("appliedVoucherCode", cartService.getAppliedVoucherCode());
        model.addAttribute("voucherDiscount", cartService.getAppliedVoucherDiscount());

        model.addAttribute("promotionVouchers", promotionVoucherService.getActiveVouchers());
        model.addAttribute("customerVouchers", verified ? voucherService.getAvailableVouchersByPhone(normalizedPhone) : List.of());
        model.addAttribute("allVoucherHistory", customerVoucherRepository.findByCustomerPhoneNumberOrderByIdDesc(normalizedPhone));

        if (!normalizedPhone.isBlank()) {
            Customer customer = customerRepository.findByPhoneNumber(normalizedPhone).orElse(null);
            model.addAttribute("customer", customer);
            model.addAttribute("customerPoints", customer == null ? 0 : customer.getPoints());
            List<Order> orders = orderRepository.findByCustomerPhoneOrderByIdDesc(normalizedPhone);
            model.addAttribute("orders", orders);
            model.addAttribute("totalOrders", orders.size());
            double totalSpent = orders.stream().mapToDouble(Order::getTotalPrice).sum();
            model.addAttribute("totalSpent", totalSpent);
        } else {
            model.addAttribute("customer", null);
            model.addAttribute("customerPoints", 0);
            model.addAttribute("orders", List.of());
            model.addAttribute("totalOrders", 0);
            model.addAttribute("totalSpent", 0);
        }

        return "rewards/rewards";
    }

    @PostMapping("/redeem/request")
    public String requestRedeem(@RequestParam String voucherPhone,
                                @RequestParam Long promotionVoucherId,
                                Principal principal,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        AppUser appUser = appUserRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        String accountEmail = normalize(appUser.getEmail());

        if (voucherPhone == null || voucherPhone.trim().isBlank()) {
            redirectAttributes.addFlashAttribute("rewardError", "Vui lòng nhập số điện thoại tích điểm");
            return "redirect:/rewards";
        }
        Customer customer = customerRepository.findByPhoneNumber(voucherPhone.trim())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng theo số điện thoại"));
        PromotionVoucher promotionVoucher = promotionVoucherService.getVoucherById(promotionVoucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher khuyến mãi"));
        if (customer.getPoints() < promotionVoucher.getPointsRequired()) {
            redirectAttributes.addFlashAttribute("rewardError", "Điểm tích lũy không đủ, không thể đổi voucher này");
            return "redirect:/rewards?voucherPhone=" + voucherPhone.trim();
        }
        if (!emailVerificationService.isValidGmail(accountEmail)) {
            redirectAttributes.addFlashAttribute("rewardError", "Chỉ hỗ trợ Gmail hợp lệ");
            return "redirect:/rewards";
        }

        boolean sent = emailVerificationService.createAndSendCode(accountEmail);
        if (!sent) {
            redirectAttributes.addFlashAttribute("rewardError", "Không gửi được mã OTP Gmail, vui lòng thử lại");
            return "redirect:/rewards";
        }

        session.setAttribute(PENDING_PHONE, voucherPhone.trim());
        session.setAttribute(PENDING_PROMOTION_ID, promotionVoucherId);
        session.setAttribute(VERIFIED_FLAG, false);
        redirectAttributes.addFlashAttribute("rewardSuccess", "Đã gửi mã OTP xác thực qua Gmail, nhập OTP để hoàn tất đổi điểm");
        return "redirect:/rewards?voucherPhone=" + voucherPhone.trim();
    }

    @PostMapping("/redeem/confirm")
    public String confirmRedeem(@RequestParam String otp,
                                Principal principal,
                                HttpSession session,
                                RedirectAttributes redirectAttributes) {
        AppUser appUser = appUserRepository.findByUsername(principal.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String accountEmail = normalize(appUser.getEmail());
        Object pendingPhoneObj = session.getAttribute(PENDING_PHONE);
        Object pendingPromotionObj = session.getAttribute(PENDING_PROMOTION_ID);
        if (pendingPhoneObj == null || pendingPromotionObj == null) {
            redirectAttributes.addFlashAttribute("rewardError", "Không có yêu cầu đổi điểm chờ xác thực");
            return "redirect:/rewards";
        }

        boolean verified = emailVerificationService.verifyCode(accountEmail, otp);
        if (!verified) {
            redirectAttributes.addFlashAttribute("rewardError", "OTP không đúng hoặc đã hết hạn, không thể đổi điểm");
            return "redirect:/rewards?voucherPhone=" + String.valueOf(pendingPhoneObj);
        }

        String voucherPhone = String.valueOf(pendingPhoneObj);
        Long promotionVoucherId = (Long) pendingPromotionObj;
        try {
            voucherService.redeemByPoints(voucherPhone, promotionVoucherId);
            int remainingPoints = customerRepository.findByPhoneNumber(voucherPhone)
                    .map(Customer::getPoints)
                    .orElse(0);
            session.setAttribute(VERIFIED_FLAG, true);
            session.removeAttribute(PENDING_PHONE);
            session.removeAttribute(PENDING_PROMOTION_ID);
            redirectAttributes.addFlashAttribute("rewardSuccess", "Xác thực Gmail thành công, đổi điểm thành voucher thành công. Điểm còn lại: " + remainingPoints);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("rewardError", ex.getMessage());
        }
        return "redirect:/rewards?voucherPhone=" + voucherPhone.trim();
    }

    @PostMapping("/apply")
    public String apply(@RequestParam String voucherPhone,
                        @RequestParam String voucherCode,
                        Principal principal,
                        HttpSession session,
                        RedirectAttributes redirectAttributes) {
        if (!Boolean.TRUE.equals(session.getAttribute(VERIFIED_FLAG))) {
            redirectAttributes.addFlashAttribute("rewardError", "Vui lòng đổi điểm và xác thực Gmail thành công trước khi áp dụng voucher");
            return "redirect:/rewards";
        }
        try {
            voucherService.applyVoucherToCart(voucherPhone, voucherCode);
            redirectAttributes.addFlashAttribute("rewardSuccess", "Áp dụng voucher thành công");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("rewardError", ex.getMessage());
        }
        return "redirect:/rewards?voucherPhone=" + voucherPhone.trim();
    }

    @PostMapping("/clear")
    public String clear(@RequestParam(value = "voucherPhone", required = false) String voucherPhone,
                        RedirectAttributes redirectAttributes) {
        voucherService.clearVoucherInCart();
        redirectAttributes.addFlashAttribute("rewardSuccess", "Đã hủy voucher đang áp dụng");
        return "redirect:/rewards?voucherPhone=" + (voucherPhone == null ? "" : voucherPhone.trim());
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
