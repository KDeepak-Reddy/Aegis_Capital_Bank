package com.example.auth.controller;

import com.example.auth.dto.UserProfileDto;
import com.example.auth.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping("/api")
public class AppController {

    private final UserService userService;

    public AppController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<String> getDashboardInfo(Principal principal) {
        String email = (principal != null) ? principal.getName() : "User";
        String username = email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        return ResponseEntity.ok("Welcome back, " + username + "!");
    }

    @GetMapping("/profile")
    public ResponseEntity<UserProfileDto> getProfile(Principal principal) {
        if (principal == null || principal.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        String email = principal.getName();
        return ResponseEntity.ok(userService.getProfileByEmail(email));
    }
}