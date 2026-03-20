package com.example.demo_3001.repository;

import com.example.demo_3001.model.PromotionVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PromotionVoucherRepository extends JpaRepository<PromotionVoucher, Long> {
    Optional<PromotionVoucher> findByCode(String code);
    boolean existsByCodeIgnoreCase(String code);
    List<PromotionVoucher> findByActiveTrueOrderByIdDesc();
}
