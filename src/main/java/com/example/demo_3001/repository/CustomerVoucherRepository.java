package com.example.demo_3001.repository;

import com.example.demo_3001.model.CustomerVoucher;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerVoucherRepository extends JpaRepository<CustomerVoucher, Long> {
    List<CustomerVoucher> findByCustomerPhoneNumberAndStatusOrderByIdDesc(String phoneNumber, String status);
    List<CustomerVoucher> findByCustomerPhoneNumberOrderByIdDesc(String phoneNumber);
    Optional<CustomerVoucher> findByVoucherCodeIgnoreCaseAndCustomerPhoneNumberAndStatus(String voucherCode, String phoneNumber, String status);
    Optional<CustomerVoucher> findByIdAndCustomerPhoneNumberAndStatus(Long id, String phoneNumber, String status);
}
