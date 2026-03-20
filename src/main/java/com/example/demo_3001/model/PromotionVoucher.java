package com.example.demo_3001.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "promotion_vouchers")
public class PromotionVoucher {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Mã voucher là bắt buộc")
    private String code;

    @NotBlank(message = "Tên khuyến mãi là bắt buộc")
    private String name;

    @Min(value = 1, message = "Điểm quy đổi phải lớn hơn 0")
    private int pointsRequired;

    @Min(value = 1000, message = "Giảm giá tối thiểu 1.000đ")
    private double discountAmount;

    @Min(value = 0, message = "Số lượng không hợp lệ")
    private int quantity;

    private boolean active = true;

    private String description;
}
