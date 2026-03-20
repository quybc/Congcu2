package com.example.demo_3001.controller;

import com.example.demo_3001.model.Order;
import com.example.demo_3001.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final OrderRepository orderRepository;

    @GetMapping("/momo-return")
    public String momoReturn(@RequestParam Map<String, String> params, Model model) {
        String momoOrderId = params.get("orderId");
        String resultCode = params.get("resultCode");
        String message = params.get("message");
        Long internalOrderId = extractInternalOrderId(momoOrderId);

        if (internalOrderId != null) {
            Order order = orderRepository.findById(internalOrderId).orElse(null);
            if (order != null) {
                if ("0".equals(resultCode)) {
                    order.setPaymentStatus("PAID");
                    order.setPaymentMethod("MOMO");
                    orderRepository.save(order);
                    model.addAttribute("message", "Thanh toán thành công qua MoMo!");
                    model.addAttribute("order", order);
                    return "order/confirmation"; // Re-use confirmation page
                } else {
                    order.setPaymentStatus("FAILED");
                    orderRepository.save(order);
                    model.addAttribute("error", "Thanh toán thất bại: " + message);
                    return "order/payment-failed"; // Need to create this or handle in cart
                }
            }
        }
        
        model.addAttribute("error", "Không tìm thấy đơn hàng hoặc dữ liệu không hợp lệ.");
        return "redirect:/";
    }

    private Long extractInternalOrderId(String momoOrderId) {
        if (momoOrderId == null || momoOrderId.isBlank()) {
            return null;
        }
        String[] parts = momoOrderId.split("-", 2);
        try {
            return Long.parseLong(parts[0]);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // Optional: Notify URL (IPN) implementation
    // @PostMapping("/momo-notify")
    // ...
}
