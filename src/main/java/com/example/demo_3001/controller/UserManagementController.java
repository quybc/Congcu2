package com.example.demo_3001.controller;

import com.example.demo_3001.model.AppUser;
import com.example.demo_3001.service.UserManagementService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserManagementController {
    private final UserManagementService userManagementService;

    @GetMapping
    public String listUsers(@RequestParam(required = false) Long roleId,
                            @RequestParam(required = false) String keyword,
                            Model model) {
        var users = userManagementService.getUsers(roleId, keyword);
        Map<Long, String> roleNameMap = users.stream()
                .collect(Collectors.toMap(AppUser::getId, userManagementService::resolveRoleName));
        model.addAttribute("users", users);
        model.addAttribute("roleNameMap", roleNameMap);
        model.addAttribute("selectedRoleId", roleId == null ? "" : roleId);
        model.addAttribute("keyword", keyword == null ? "" : keyword.trim());
        model.addAttribute("roles", userManagementService.getAllRoles());
        return "users/user-list";
    }

    @PostMapping("/{id}/role")
    public String updateRole(@PathVariable Long id,
                             @RequestParam Long roleId,
                             @RequestParam(required = false) String filterRoleId,
                             @RequestParam(required = false) String keyword,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            userManagementService.updateUserRole(id, roleId, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Cập nhật quyền thành công");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return buildRedirectUrl(filterRoleId, keyword);
    }

    @PostMapping("/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id,
                               @RequestParam(required = false) String filterRoleId,
                               @RequestParam(required = false) String keyword,
                               Authentication authentication,
                               RedirectAttributes redirectAttributes) {
        try {
            userManagementService.toggleUserStatus(id, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Cập nhật trạng thái tài khoản thành công");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return buildRedirectUrl(filterRoleId, keyword);
    }

    @PostMapping("/{id}/delete")
    public String deleteUser(@PathVariable Long id,
                             @RequestParam(required = false) String filterRoleId,
                             @RequestParam(required = false) String keyword,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        try {
            userManagementService.deleteUser(id, authentication.getName());
            redirectAttributes.addFlashAttribute("success", "Xóa tài khoản thành công");
        } catch (RuntimeException ex) {
            redirectAttributes.addFlashAttribute("error", ex.getMessage());
        }
        return buildRedirectUrl(filterRoleId, keyword);
    }

    private String buildRedirectUrl(String filterRoleId, String keyword) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromPath("/users");
        boolean hasFilterRole = filterRoleId != null && !filterRoleId.isBlank();
        boolean hasKeyword = keyword != null && !keyword.isBlank();
        if (hasFilterRole) {
            builder.queryParam("roleId", filterRoleId.trim());
        }
        if (hasKeyword) {
            builder.queryParam("keyword", keyword.trim());
        }
        return "redirect:" + builder.build().encode().toUriString();
    }
}
