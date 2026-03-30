package com.example.auth.controller;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.MfaVerificationRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        try {
            return ResponseEntity.ok(authService.register(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AuthResponse.builder().message(e.getMessage()).mfaRequired(false).build());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        try {
            return ResponseEntity.ok(authService.login(request));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(AuthResponse.builder().message("Invalid credentials").mfaRequired(false).build());
        }
    }

    @PostMapping("/verify-mfa")
    public ResponseEntity<AuthResponse> verifyMfa(@RequestBody MfaVerificationRequest request) {
        try {
            return ResponseEntity.ok(authService.verifyMfa(request));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(
                    AuthResponse.builder()
                            .message(e.getMessage())
                            .mfaRequired(false)
                            .build());
        }
    }

    @PostMapping("/reset-password-mfa")
    public ResponseEntity<Map<String, String>> resetPasswordMfa(@RequestBody Map<String, String> body) {
        try {
            String email = body.get("email");
            String mfaCode = body.get("mfaCode");
            String newPassword = body.get("newPassword");
            authService.resetPasswordMfa(email, mfaCode, newPassword);
            return ResponseEntity.ok(Map.of("message", "Password reset successfully. You can now log in."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
