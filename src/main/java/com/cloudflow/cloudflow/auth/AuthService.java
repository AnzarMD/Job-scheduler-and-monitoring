package com.cloudflow.cloudflow.auth;

import com.cloudflow.cloudflow.auth.dto.AuthResponse;
import com.cloudflow.cloudflow.auth.dto.LoginRequest;
import com.cloudflow.cloudflow.auth.dto.RegisterRequest;
import com.cloudflow.cloudflow.tenant.Tenant;
import com.cloudflow.cloudflow.tenant.TenantRepository;
import com.cloudflow.cloudflow.user.User;
import com.cloudflow.cloudflow.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        // Validate uniqueness
        if (tenantRepository.existsByName(request.getCompanyName())) {
            throw new IllegalArgumentException("Company name already exists");
        }
        if (tenantRepository.existsBySlug(request.getSlug())) {
            throw new IllegalArgumentException("Slug already taken");
        }

        // Create tenant
        Tenant tenant = Tenant.builder()
                .name(request.getCompanyName())
                .slug(request.getSlug())
                .apiKey(UUID.randomUUID().toString().replace("-", ""))
                .plan("FREE")
                .maxJobs(10)
                .isActive(true)
                .build();
        tenant = tenantRepository.save(tenant);

        // Create the first user (OWNER role)
        User user = User.builder()
                .tenant(tenant)
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .role("OWNER")
                .isActive(true)
                .build();
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .tenantId(tenant.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .companyName(tenant.getName())
                .build();
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // Find tenant by slug first
        Tenant tenant = tenantRepository.findBySlug(request.getSlug())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        // Find user by email within this tenant
        User user = userRepository.findByEmailAndTenantId(request.getEmail(), tenant.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        // Verify password using BCrypt
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        if (!user.getIsActive()) {
            throw new IllegalArgumentException("Account is disabled");
        }

        String token = jwtService.generateToken(user);

        return AuthResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .userId(user.getId())
                .tenantId(tenant.getId())
                .email(user.getEmail())
                .role(user.getRole())
                .companyName(tenant.getName())
                .build();
    }
}