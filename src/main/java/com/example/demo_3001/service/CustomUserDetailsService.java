package com.example.demo_3001.service;

import com.example.demo_3001.model.AppUser;
import com.example.demo_3001.model.RoleCode;
import com.example.demo_3001.repository.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {
    private final AppUserRepository appUserRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        AppUser appUser = appUserRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Không tìm thấy tài khoản"));
        if (appUser.getUserRole() == null) {
            throw new UsernameNotFoundException("Tài khoản chưa được gán role");
        }
        RoleCode roleCode = RoleCode.fromId(appUser.getUserRole().getRoleId());

        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + roleCode.getName())))
                .disabled(!appUser.isEnabled())
                .build();
    }
}
