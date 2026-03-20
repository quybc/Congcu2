package com.example.demo_3001.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "categories")
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Tên là bắt buộc")
    private String name;

    private String parentCategory; // Danh mục cha (Điện thoại, Laptop, Phụ kiện, ...)

    private String image; // Hình ảnh đại diện của danh mục

    @Transient
    private String displayName;

    public String getDisplayName() {
        return displayName != null ? displayName : name;
    }
}
