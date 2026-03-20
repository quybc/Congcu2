package com.example.demo_3001.model;

import jakarta.persistence.*;
import lombok.*;

@Setter
@Getter
@RequiredArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "customers")
public class Customer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String fullName;
    
    @Column(unique = true)
    private String phoneNumber;
    
    private String address;
    
    private String gender; // "Anh" or "Chi"
    
    private int points = 0; // Accumulated loyalty points
}
