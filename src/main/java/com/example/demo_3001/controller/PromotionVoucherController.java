package com.example.demo_3001.controller;

import com.example.demo_3001.model.PromotionVoucher;
import com.example.demo_3001.service.PromotionVoucherService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
@RequestMapping("/promotions")
public class PromotionVoucherController {
    private final PromotionVoucherService promotionVoucherService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("vouchers", promotionVoucherService.getAllVouchers());
        return "promotions/promotion-list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("voucher", new PromotionVoucher());
        return "promotions/promotion-form";
    }

    @PostMapping("/save")
    public String save(@Valid PromotionVoucher voucher, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "promotions/promotion-form";
        }
        try {
            promotionVoucherService.save(voucher);
        } catch (RuntimeException ex) {
            result.rejectValue("code", "duplicate.code", ex.getMessage());
            return "promotions/promotion-form";
        }
        redirectAttributes.addFlashAttribute("successMessage", "Đã lưu voucher khuyến mãi thành công");
        return "redirect:/promotions";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        PromotionVoucher voucher = promotionVoucherService.getVoucherById(id)
                .orElseThrow(() -> new IllegalArgumentException("Invalid voucher Id:" + id));
        model.addAttribute("voucher", voucher);
        return "promotions/promotion-form";
    }

    @GetMapping("/delete/{id}")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        promotionVoucherService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Đã xóa voucher khuyến mãi");
        return "redirect:/promotions";
    }
}
