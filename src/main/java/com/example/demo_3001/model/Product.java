package com.example.demo_3001.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private double price;
    private String description;
    private String image;
    private int discount; // Percentage discount (e.g., 10 for 10%)

    private String productType = "NONE"; // NONE, KHUYEN_MAI, QUA_TANG
    
    private int quantity; // Tổng số lượng trong kho
    private int promotionQuantity; // Số lượng khuyến mãi còn lại
    private int totalPromotionQuantity = 10; // Tổng số lượng khuyến mãi ban đầu (mặc định 10)

    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;
}
