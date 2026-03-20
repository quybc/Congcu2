package com.example.demo_3001.service;

import com.example.demo_3001.model.CartItem;
import com.example.demo_3001.model.Product;
import com.example.demo_3001.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@SessionScope
public class CartService {
    private List<CartItem> cartItems = new ArrayList<>();
    private final ProductRepository productRepository;
    private Long appliedVoucherId;
    private String appliedVoucherCode;
    private double appliedVoucherDiscount;
    private String voucherSelectionPhone;

    public CartService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public void addToCart(Long productId, int quantity) {
        Optional<Product> productOpt = productRepository.findById(productId);
        if (productOpt.isPresent()) {
            Product product = productOpt.get();
            Optional<CartItem> existingItem = cartItems.stream()
                    .filter(item -> item.getProduct().getId().equals(productId))
                    .findFirst();

            if (existingItem.isPresent()) {
                existingItem.get().setQuantity(existingItem.get().getQuantity() + quantity);
            } else {
                cartItems.add(new CartItem(product, quantity));
            }
        }
    }

    public void removeFromCart(Long productId) {
        cartItems.removeIf(item -> item.getProduct().getId().equals(productId));
    }

    public void updateQuantity(Long productId, int quantity) {
        Optional<CartItem> existingItem = cartItems.stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
        
        if (existingItem.isPresent()) {
            if (quantity > 0) {
                existingItem.get().setQuantity(quantity);
            } else {
                removeFromCart(productId);
            }
        }
    }

    public void clearCart() {
        cartItems.clear();
    }

    public List<CartItem> getCartItems() {
        return cartItems;
    }

    public double getTotalAmount() {
        return cartItems.stream()
                .mapToDouble(item -> {
                    double price = item.getProduct().getPrice();
                    double discount = item.getProduct().getDiscount();
                    int promoQty = item.getProduct().getPromotionQuantity();
                    int quantity = item.getQuantity();

                    double discountedPrice = price * (1 - discount / 100.0);
                    
                    if (quantity <= promoQty) {
                        return discountedPrice * quantity;
                    } else {
                        // Số lượng trong mức khuyến mãi tính giá giảm
                        // Số lượng vượt mức tính giá gốc
                        return (discountedPrice * promoQty) + (price * (quantity - promoQty));
                    }
                })
                .sum();
    }

    public int getCartCount() {
        return cartItems.stream().mapToInt(CartItem::getQuantity).sum();
    }

    public double getShippingFee() {
        double total = getTotalAmount();
        int distinctItems = cartItems.size(); // Số lượng loại sản phẩm khác nhau
        
        // Miễn phí ship nếu tổng tiền > 1 triệu HOẶC mua từ 2 loại sản phẩm trở lên
        if (total > 1000000 || distinctItems >= 2) {
            return 0.0;
        } else {
            return 30000.0;
        }
    }

    public int getRewardPoints() {
        double total = getTotalAmount();
        return (int) (total / 15000) * 2;
    }

    public double getFinalTotal() {
        double finalTotal = getTotalAmount() + getShippingFee() - appliedVoucherDiscount;
        return Math.max(finalTotal, 0);
    }

    public void applyVoucher(Long voucherId, String voucherCode, double discountAmount) {
        this.appliedVoucherId = voucherId;
        this.appliedVoucherCode = voucherCode;
        this.appliedVoucherDiscount = discountAmount;
    }

    public void clearAppliedVoucher() {
        this.appliedVoucherId = null;
        this.appliedVoucherCode = null;
        this.appliedVoucherDiscount = 0;
    }

    public void setVoucherSelectionPhone(String phoneNumber) {
        this.voucherSelectionPhone = phoneNumber == null ? "" : phoneNumber.trim();
    }

    public String getVoucherSelectionPhone() {
        return voucherSelectionPhone == null ? "" : voucherSelectionPhone;
    }

    public Long getAppliedVoucherId() {
        return appliedVoucherId;
    }

    public String getAppliedVoucherCode() {
        return appliedVoucherCode;
    }

    public double getAppliedVoucherDiscount() {
        return appliedVoucherDiscount;
    }
}
