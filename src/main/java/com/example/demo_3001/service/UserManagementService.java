package com.example.demo_3001.service;

import com.example.demo_3001.model.AppUser;
import com.example.demo_3001.model.RoleCode;
import com.example.demo_3001.model.UserRole;
import com.example.demo_3001.repository.AppUserRepository;
import com.example.demo_3001.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserManagementService {
    private static final Logger logger = LoggerFactory.getLogger(UserManagementService.class);
    private final AppUserRepository appUserRepository;
    private final UserRoleRepository userRoleRepository;

    public List<AppUser> getUsers(Long roleId, String keyword) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        boolean hasKeyword = !normalizedKeyword.isEmpty();

        if (roleId != null && hasKeyword) {
            return appUserRepository.findByUserRole_RoleIdAndUsernameContainingIgnoreCase(roleId, normalizedKeyword);
        }
        if (roleId != null) {
            return appUserRepository.findByUserRole_RoleId(roleId);
        }
        if (hasKeyword) {
            return appUserRepository.findByUsernameContainingIgnoreCase(normalizedKeyword);
        }
        return appUserRepository.findAll();
    }

    @Transactional
    public void updateUserRole(Long targetUserId, Long newRoleId, String actorUsername) {
        AppUser targetUser = appUserRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));
        UserRole oldRole = targetUser.getUserRole();
        if (oldRole == null) {
            oldRole = new UserRole(targetUser.getId(), RoleCode.USER.getId(), targetUser);
        }

        if (oldRole.getRoleId().equals(newRoleId)) {
            return;
        }

        if (targetUser.getUsername().equals(actorUsername) && isAdmin(oldRole.getRoleId()) && !isAdmin(newRoleId)) {
            throw new IllegalStateException("Bạn không thể tự hạ quyền admin của chính mình");
        }

        if (isAdmin(oldRole.getRoleId()) && !isAdmin(newRoleId) && userRoleRepository.countByRoleId(RoleCode.ADMIN.getId()) <= 1) {
            throw new IllegalStateException("Không thể hạ quyền admin cuối cùng trong hệ thống");
        }

        UserRole updatedRole = userRoleRepository.findById(targetUser.getId())
                .orElseGet(() -> {
                    UserRole userRole = new UserRole();
                    userRole.setUser(targetUser);
                    return userRole;
                });
        updatedRole.setRoleId(newRoleId);
        userRoleRepository.save(updatedRole);
        targetUser.setUserRole(updatedRole);
        logger.info("ROLE_CHANGED actor={} target={} oldRole={} newRole={}",
                actorUsername,
                targetUser.getUsername(),
                RoleCode.fromId(oldRole.getRoleId()).getName(),
                RoleCode.fromId(newRoleId).getName());
    }

    @Transactional
    public void toggleUserStatus(Long targetUserId, String actorUsername) {
        AppUser targetUser = appUserRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (targetUser.getUsername().equals(actorUsername) && targetUser.isEnabled()) {
            throw new IllegalStateException("Bạn không thể tự vô hiệu hóa tài khoản của chính mình");
        }

        targetUser.setEnabled(!targetUser.isEnabled());
        appUserRepository.save(targetUser);
        logger.info("USER_STATUS_CHANGED actor={} target={} enabled={}", actorUsername, targetUser.getUsername(), targetUser.isEnabled());
    }

    @Transactional
    public void deleteUser(Long targetUserId, String actorUsername) {
        AppUser targetUser = appUserRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy người dùng"));

        if (targetUser.getUsername().equals(actorUsername)) {
            throw new IllegalStateException("Bạn không thể tự xóa tài khoản của chính mình");
        }

        Long targetRoleId = targetUser.getUserRole() == null ? RoleCode.USER.getId() : targetUser.getUserRole().getRoleId();
        if (isAdmin(targetRoleId) && userRoleRepository.countByRoleId(RoleCode.ADMIN.getId()) <= 1) {
            throw new IllegalStateException("Không thể xóa tài khoản admin cuối cùng trong hệ thống");
        }

        userRoleRepository.deleteById(targetUser.getId());
        appUserRepository.delete(targetUser);
        logger.info("USER_DELETED actor={} target={} role={}", actorUsername, targetUser.getUsername(), RoleCode.fromId(targetRoleId).getName());
    }

    public List<RoleCode> getAllRoles() {
        return List.of(RoleCode.ADMIN, RoleCode.MANAGER, RoleCode.USER);
    }

    public String resolveRoleName(AppUser appUser) {
        Long roleId = appUser.getUserRole() == null ? RoleCode.USER.getId() : appUser.getUserRole().getRoleId();
        return RoleCode.fromId(roleId).getName();
    }

    private boolean isAdmin(Long roleId) {
        return RoleCode.ADMIN.getId().equals(roleId);
    }
}
