package com.example.auth.service;

import com.example.auth.dto.UserProfileDto;
import com.example.auth.model.User;
import com.example.auth.repository.UserRepository;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

// Service
@Service
public class UserService {

    private final UserRepository repo;

    public UserService(UserRepository repo) {
        this.repo = repo;
    }

    public UserProfileDto getProfileByEmail(String email) {
        User user = repo.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        return new UserProfileDto(
                safe(user.getName()),
                safe(user.getEmail()),
                user.getDob() != null ? user.getDob().toString() : "-",
                safe(user.getMobileNo()),
                safe(user.getPanNo())
        );
    }

    private String safe(String v) { return (v == null || v.isBlank()) ? "-" : v; }
}
