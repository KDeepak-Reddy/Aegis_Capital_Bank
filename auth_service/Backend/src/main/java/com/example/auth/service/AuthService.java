package com.example.auth.service;

import com.example.auth.dto.AuthResponse;
import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.MfaVerificationRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.model.Role;
import com.example.auth.model.User;
import com.example.auth.repository.RoleRepository;
import com.example.auth.repository.UserRepository;
import com.example.auth.security.CustomUserDetailsService;
import com.example.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final MfaService mfaService;
    private final CustomUserDetailsService userDetailsService;

    // In-memory store for password reset tokens (token -> expiry epoch seconds)
    private final Map<String, long[]> resetTokens = new ConcurrentHashMap<>();
    private final Map<String, String>  resetTokenEmail = new ConcurrentHashMap<>();

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already in use");
        }
        if (userRepository.existsByPanNo(request.getPanNo())) {
            throw new RuntimeException("PAN already in use");
        }

        Role userRole = roleRepository.findByName("USER")
                .orElseGet(() -> roleRepository.save(new Role(null, "USER")));

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        User user = new User();
        user.setName(request.getName());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setDob(request.getDob());
        user.setMobileNo(request.getMobileNo());
        user.setPanNo(request.getPanNo());
        user.setMfaSecret(mfaService.generateSecretKey());
        user.setMfaEnabled(true);
        user.setRoles(roles);

        userRepository.save(user);

        return AuthResponse.builder()
                .message("User registered successfully. Scan the QR code with your authenticator app.")
                .secretImageUri(mfaService.generateQrCodeImageUri(user.getMfaSecret(), user.getEmail()))
                .mfaRequired(false)
                .build();
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.isMfaEnabled()) {
            return AuthResponse.builder()
                    .message("MFA required. Please verify using your authenticator app secret.")
                    .mfaRequired(true)
                    .build();
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .message("Login successful")
                .mfaRequired(false)
                .build();
    }

    public AuthResponse verifyMfa(MfaVerificationRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String code = request.getCode().trim();

        System.out.println("SECRET: " + user.getMfaSecret());
        System.out.println("CODE FROM USER: " + code);

        boolean valid = mfaService.verifyCode(user.getMfaSecret(), code);

        System.out.println("VALID RESULT: " + valid);

        if (!valid) {
            throw new RuntimeException("Invalid MFA code");
        }

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());
        String jwtToken = jwtService.generateToken(userDetails);

        return AuthResponse.builder()
                .token(jwtToken)
                .message("MFA verification successful")
                .mfaRequired(false)
                .build();
    }

    /**
     * MFA-secured Password Reset: validates the user's TOTP code and updates their password.
     */
    public void resetPasswordMfa(String email, String mfaCode, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("No account found with that email"));

        if (!user.isMfaEnabled()) {
            throw new RuntimeException("MFA is not enabled for this account");
        }

        boolean valid = mfaService.verifyCode(user.getMfaSecret(), mfaCode.trim());
        if (!valid) {
            throw new RuntimeException("Invalid Authenticator code");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
