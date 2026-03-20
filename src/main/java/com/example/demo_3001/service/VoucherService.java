package com.example.demo_3001.service;

import com.example.demo_3001.model.Customer;
import com.example.demo_3001.model.CustomerVoucher;
import com.example.demo_3001.model.PromotionVoucher;
import com.example.demo_3001.repository.CustomerRepository;
import com.example.demo_3001.repository.CustomerVoucherRepository;
import com.example.demo_3001.repository.PromotionVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class VoucherService {
    private static final String STATUS_AVAILABLE = "AVAILABLE";
    private static final String STATUS_USED = "USED";

    private final CustomerRepository customerRepository;
    private final PromotionVoucherRepository promotionVoucherRepository;
    private final CustomerVoucherRepository customerVoucherRepository;
    private final CartService cartService;

    public List<CustomerVoucher> getAvailableVouchersByPhone(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            return List.of();
        }
        Long appliedVoucherId = cartService.getAppliedVoucherId();
        return customerVoucherRepository.findByCustomerPhoneNumberAndStatusOrderByIdDesc(phoneNumber.trim(), STATUS_AVAILABLE)
                .stream()
                .filter(v -> appliedVoucherId == null || !v.getId().equals(appliedVoucherId))
                .toList();
    }

    public CustomerVoucher redeemByPoints(String phoneNumber, Long promotionVoucherId) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new RuntimeException("Vui lòng nhập số điện thoại");
        }

        String normalizedPhone = phoneNumber.trim();

        Customer customer = customerRepository.findByPhoneNumber(normalizedPhone)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng theo số điện thoại"));

        PromotionVoucher promotionVoucher = promotionVoucherRepository.findById(promotionVoucherId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy voucher khuyến mãi"));

        if (!promotionVoucher.isActive()) {
            throw new RuntimeException("Voucher khuyến mãi đang tạm khóa");
        }
        if (promotionVoucher.getQuantity() <= 0) {
            throw new RuntimeException("Voucher đã hết số lượng");
        }
        if (customer.getPoints() < promotionVoucher.getPointsRequired()) {
            throw new RuntimeException("Điểm hiện tại không đủ để đổi voucher này");
        }

        customer.setPoints(customer.getPoints() - promotionVoucher.getPointsRequired());
        promotionVoucher.setQuantity(promotionVoucher.getQuantity() - 1);

        customerRepository.save(customer);
        promotionVoucherRepository.save(promotionVoucher);

        CustomerVoucher customerVoucher = new CustomerVoucher();
        customerVoucher.setCustomer(customer);
        customerVoucher.setPromotionVoucher(promotionVoucher);
        customerVoucher.setStatus(STATUS_AVAILABLE);
        customerVoucher.setExchangedPoints(promotionVoucher.getPointsRequired());
        customerVoucher.setDiscountAmount(promotionVoucher.getDiscountAmount());
        customerVoucher.setVoucherCode(generateVoucherCode(promotionVoucher.getCode()));
        CustomerVoucher savedVoucher = customerVoucherRepository.save(customerVoucher);
        cartService.setVoucherSelectionPhone(normalizedPhone);
        return savedVoucher;
    }

    public CustomerVoucher applyVoucherToCart(String phoneNumber, String voucherCode) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new RuntimeException("Vui lòng nhập số điện thoại");
        }
        if (voucherCode == null || voucherCode.isBlank()) {
            throw new RuntimeException("Vui lòng nhập mã voucher");
        }

        String normalizedPhone = phoneNumber.trim();

        CustomerVoucher customerVoucher = customerVoucherRepository
                .findByVoucherCodeIgnoreCaseAndCustomerPhoneNumberAndStatus(voucherCode.trim(), normalizedPhone, STATUS_AVAILABLE)
                .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ hoặc đã sử dụng"));

        cartService.applyVoucher(customerVoucher.getId(), customerVoucher.getVoucherCode(), customerVoucher.getDiscountAmount());
        cartService.setVoucherSelectionPhone(normalizedPhone);
        return customerVoucher;
    }

    public CustomerVoucher applyVoucherToCart(String phoneNumber, Long customerVoucherId) {
        if (phoneNumber == null || phoneNumber.isBlank()) {
            throw new RuntimeException("Vui lòng nhập số điện thoại");
        }
        if (customerVoucherId == null) {
            throw new RuntimeException("Vui lòng chọn voucher");
        }

        String normalizedPhone = phoneNumber.trim();

        CustomerVoucher customerVoucher = customerVoucherRepository
                .findByIdAndCustomerPhoneNumberAndStatus(customerVoucherId, normalizedPhone, STATUS_AVAILABLE)
                .orElseThrow(() -> new RuntimeException("Voucher không hợp lệ hoặc đã sử dụng"));

        cartService.applyVoucher(customerVoucher.getId(), customerVoucher.getVoucherCode(), customerVoucher.getDiscountAmount());
        cartService.setVoucherSelectionPhone(normalizedPhone);
        return customerVoucher;
    }

    public void clearVoucherInCart() {
        cartService.clearAppliedVoucher();
    }

    public void consumeAppliedVoucher() {
        Long customerVoucherId = cartService.getAppliedVoucherId();
        if (customerVoucherId == null) {
            return;
        }

        Optional<CustomerVoucher> customerVoucherOpt = customerVoucherRepository.findById(customerVoucherId);
        if (customerVoucherOpt.isPresent()) {
            CustomerVoucher customerVoucher = customerVoucherOpt.get();
            if (STATUS_AVAILABLE.equals(customerVoucher.getStatus())) {
                customerVoucher.setStatus(STATUS_USED);
                customerVoucherRepository.save(customerVoucher);
            }
        }
        cartService.clearAppliedVoucher();
    }

    private String generateVoucherCode(String promotionCode) {
        return promotionCode + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}
