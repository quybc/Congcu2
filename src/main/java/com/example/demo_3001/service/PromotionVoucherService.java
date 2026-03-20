package com.example.demo_3001.service;

import com.example.demo_3001.model.PromotionVoucher;
import com.example.demo_3001.repository.PromotionVoucherRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class PromotionVoucherService {
    private final PromotionVoucherRepository promotionVoucherRepository;

    public List<PromotionVoucher> getAllVouchers() {
        return promotionVoucherRepository.findAll();
    }

    public List<PromotionVoucher> getActiveVouchers() {
        return promotionVoucherRepository.findByActiveTrueOrderByIdDesc();
    }

    public Optional<PromotionVoucher> getVoucherById(Long id) {
        return promotionVoucherRepository.findById(id);
    }

    public PromotionVoucher save(PromotionVoucher voucher) {
        String code = voucher.getCode() == null ? "" : voucher.getCode().trim().toUpperCase();
        voucher.setCode(code);
        if (voucher.getId() == null && promotionVoucherRepository.existsByCodeIgnoreCase(code)) {
            throw new RuntimeException("Mã voucher đã tồn tại");
        }
        if (voucher.getId() != null) {
            promotionVoucherRepository.findByCode(code)
                    .filter(existing -> !existing.getId().equals(voucher.getId()))
                    .ifPresent(existing -> {
                        throw new RuntimeException("Mã voucher đã tồn tại");
                    });
        }
        return promotionVoucherRepository.save(voucher);
    }

    public void deleteById(Long id) {
        promotionVoucherRepository.deleteById(id);
    }
}
