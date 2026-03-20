package com.example.demo_3001.controller;

import com.example.demo_3001.service.CartService;
import com.example.demo_3001.service.PromotionVoucherService;
import com.example.demo_3001.service.VoucherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/cart")
public class CartController {

    private final CartService cartService;
    private final PromotionVoucherService promotionVoucherService;
    private final VoucherService voucherService;

    @Autowired
    public CartController(CartService cartService, PromotionVoucherService promotionVoucherService, VoucherService voucherService) {
        this.cartService = cartService;
        this.promotionVoucherService = promotionVoucherService;
        this.voucherService = voucherService;
    }

    @GetMapping
    public String viewCart(@RequestParam(value = "voucherPhone", required = false) String voucherPhone, Model model) {
        String normalizedPhone = voucherPhone == null ? "" : voucherPhone.trim();
        if (normalizedPhone.isBlank()) {
            normalizedPhone = cartService.getVoucherSelectionPhone();
        } else {
            cartService.setVoucherSelectionPhone(normalizedPhone);
        }
        model.addAttribute("cartItems", cartService.getCartItems());
        model.addAttribute("totalAmount", cartService.getTotalAmount());
        model.addAttribute("shippingFee", cartService.getShippingFee());
        model.addAttribute("rewardPoints", cartService.getRewardPoints());
        model.addAttribute("finalTotal", cartService.getFinalTotal());
        model.addAttribute("voucherDiscount", cartService.getAppliedVoucherDiscount());
        model.addAttribute("appliedVoucherCode", cartService.getAppliedVoucherCode());
        model.addAttribute("promotionVouchers", promotionVoucherService.getActiveVouchers());
        model.addAttribute("voucherPhone", normalizedPhone);
        model.addAttribute("customerVouchers", voucherService.getAvailableVouchersByPhone(normalizedPhone));
        return "cart/cart";
    }

    @GetMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId) {
        cartService.addToCart(productId, 1);
        return "redirect:/cart";
    }

    @GetMapping("/clear")
    public String clearCart() {
        cartService.clearCart();
        cartService.clearAppliedVoucher();
        return "redirect:/cart";
    }

    @GetMapping("/remove/{productId}")
    public String removeFromCart(@PathVariable Long productId) {
        cartService.removeFromCart(productId);
        return "redirect:/cart";
    }

    @GetMapping("/update/{productId}")
    public String updateQuantity(@PathVariable Long productId, @RequestParam("quantity") int quantity) {
        cartService.updateQuantity(productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/voucher/redeem")
    public String redeemVoucherByPoints(@RequestParam String voucherPhone,
                                        @RequestParam Long promotionVoucherId,
                                        RedirectAttributes redirectAttributes) {
        try {
            voucherService.redeemByPoints(voucherPhone, promotionVoucherId);
            redirectAttributes.addFlashAttribute("voucherSuccess", "Đổi điểm thành voucher thành công, vui lòng tích chọn voucher để áp dụng cho đơn hàng");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("voucherError", ex.getMessage());
        }
        return "redirect:/cart?voucherPhone=" + voucherPhone;
    }

    @PostMapping("/voucher/apply")
    public String applyVoucher(@RequestParam String voucherPhone,
                               @RequestParam Long customerVoucherId,
                               RedirectAttributes redirectAttributes) {
        try {
            voucherService.applyVoucherToCart(voucherPhone, customerVoucherId);
            redirectAttributes.addFlashAttribute("voucherSuccess", "Áp dụng voucher thành công");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("voucherError", ex.getMessage());
        }
        return "redirect:/cart?voucherPhone=" + voucherPhone.trim();
    }

    @GetMapping("/voucher/apply")
    public String applyVoucherGet(@RequestParam String voucherPhone,
                                  @RequestParam Long customerVoucherId,
                                  RedirectAttributes redirectAttributes) {
        try {
            voucherService.applyVoucherToCart(voucherPhone, customerVoucherId);
            redirectAttributes.addFlashAttribute("voucherSuccess", "Áp dụng voucher thành công");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("voucherError", ex.getMessage());
        }
        return "redirect:/cart?voucherPhone=" + voucherPhone.trim();
    }

    @PostMapping("/voucher/clear")
    public String clearVoucher(@RequestParam(value = "voucherPhone", required = false) String voucherPhone,
                               RedirectAttributes redirectAttributes) {
        voucherService.clearVoucherInCart();
        redirectAttributes.addFlashAttribute("voucherSuccess", "Đã hủy áp dụng voucher");
        return "redirect:/cart?voucherPhone=" + (voucherPhone == null ? "" : voucherPhone);
    }

    @GetMapping("/voucher/clear")
    public String clearVoucherGet(@RequestParam(value = "voucherPhone", required = false) String voucherPhone,
                                  RedirectAttributes redirectAttributes) {
        voucherService.clearVoucherInCart();
        redirectAttributes.addFlashAttribute("voucherSuccess", "Đã hủy áp dụng voucher");
        return "redirect:/cart?voucherPhone=" + (voucherPhone == null ? "" : voucherPhone);
    }
}
