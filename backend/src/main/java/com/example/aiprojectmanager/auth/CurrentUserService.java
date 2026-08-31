package com.example.aiprojectmanager.auth;

import com.example.aiprojectmanager.common.NotFoundException;
import com.example.aiprojectmanager.user.domain.User;
import com.example.aiprojectmanager.user.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class CurrentUserService {
    private final UserRepository users;

    public CurrentUserService(UserRepository users) {
        this.users = users;
    }

    public Long id(Authentication a) {
        if (a == null) return null;
        return users.findByEmail(a.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    public Long getCurrentUserId() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Not authenticated");
        }
        return users.findByEmail(a.getName())
                .map(u -> u.getId())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }

    public User getCurrentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw new org.springframework.security.authentication.BadCredentialsException("Not authenticated");
        }
        return users.findByEmail(a.getName())
                .orElseThrow(() -> new NotFoundException("Authenticated user not found"));
    }
}
