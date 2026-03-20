package com.example.demo_3001.repository;

import com.example.demo_3001.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    long countByRoleId(Long roleId);

    List<UserRole> findByRoleId(Long roleId);
}
