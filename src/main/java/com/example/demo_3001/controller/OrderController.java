package com.example.demo_3001.controller;

import com.example.demo_3001.dto.MomoResponse;
import com.example.demo_3001.model.Order;
import com.example.demo_3001.service.MomoService;
import com.example.demo_3001.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import org.springframework.web.bind.annotation.PathVariable;

@Controller
@RequestMapping("/order")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;
    private final MomoService momoService;

    @GetMapping("/manage")
    public String listOrders(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "order/order-list";
    }

    @GetMapping("/manage/detail/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        model.addAttribute("order", orderService.getOrderById(id));
        return "order/order-detail";
    }

    @PostMapping("/checkout")
    public String checkout(@RequestParam String customerName,
                           @RequestParam String customerPhone,
                           @RequestParam String customerAddress,
                           @RequestParam(defaultValue = "Anh") String customerGender,
                           @RequestParam(defaultValue = "COD") String paymentMethod,
                           Model model,
                           RedirectAttributes redirectAttributes) {
        Order order;
        try {
            order = orderService.createOrder(customerName, customerPhone, customerAddress, customerGender, paymentMethod);
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("errorCheckout", ex.getMessage());
            return "redirect:/cart";
        }
        
        if ("MOMO".equalsIgnoreCase(paymentMethod)) {
            MomoResponse response = momoService.createPayment(order);
            boolean success = response != null
                    && response.getPayUrl() != null
                    && !response.getPayUrl().isBlank()
                    && (response.getResultCode() == null || response.getResultCode() == 0)
                    && response.getErrorCode() == 0;
            if (success) {
                return "redirect:" + response.getPayUrl();
            }
            String momoMessage = response == null
                    ? "Không nhận được phản hồi từ MoMo."
                    : (response.getLocalMessage() != null ? response.getLocalMessage() : response.getMessage());
            model.addAttribute("error", "Lỗi tạo giao dịch MoMo: " + momoMessage);
            model.addAttribute("order", order);
            return "order/payment-failed";
        }
        
        model.addAttribute("order", order);
        return "order/confirmation";
    }
}
