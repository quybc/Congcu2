package com.example.demo_3001.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "user_roles")
public class UserRole {
    @Id
    @Column(name = "user_id")
    private Long userId;

    @Column(name = "role_id", nullable = false)
    private Long roleId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "user_id")
    private AppUser user;
}
