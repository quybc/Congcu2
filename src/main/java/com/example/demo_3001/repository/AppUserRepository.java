package com.example.demo_3001.repository;

import com.example.demo_3001.model.AppUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    long countByUserRole_RoleId(Long roleId);

    List<AppUser> findByUserRole_RoleId(Long roleId);

    List<AppUser> findByUsernameContainingIgnoreCase(String username);

    List<AppUser> findByUserRole_RoleIdAndUsernameContainingIgnoreCase(Long roleId, String username);

    List<AppUser> findByUserRoleIsNull();
}
