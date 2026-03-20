package com.example.demo_3001.model;

import jakarta.persistence.*;
import lombok.*;
import java.util.List;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "orders")
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String customerName;
    private String customerPhone;
    private String customerAddress;
    private String customerGender;
    private double totalPrice;
    private double shippingFee;
    private double voucherDiscount;
    private String voucherCode;
    private String paymentMethod; // "COD" or "MOMO" or "VN_PAY"
    private String paymentStatus; // "PENDING", "PAID", "FAILED"

    @ManyToOne
    @JoinColumn(name = "customer_id")
    private Customer customer;

    @OneToMany(mappedBy = "order")
    private List<OrderDetail> orderDetails;
}
