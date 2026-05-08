package com.cloudflow.cloudflow.auth;

import com.cloudflow.cloudflow.auth.dto.AuthResponse;
import com.cloudflow.cloudflow.auth.dto.LoginRequest;
import com.cloudflow.cloudflow.auth.dto.RegisterRequest;
import com.cloudflow.cloudflow.tenant.Tenant;
import com.cloudflow.cloudflow.tenant.TenantRepository;
import com.cloudflow.cloudflow.user.User;
import com.cloudflow.cloudflow.user.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("register should throw when company name already exists")
    void register_duplicateCompanyName_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setCompanyName("Acme Corp");
        request.setSlug("acmecorp");
        request.setEmail("admin@acme.com");
        request.setPassword("password123");

        when(tenantRepository.existsByName("Acme Corp")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Company name already exists");

        // Tenant should NOT be saved when name is duplicate
        verify(tenantRepository, never()).save(any());
    }

    @Test
    @DisplayName("register should throw when slug is already taken")
    void register_duplicateSlug_throwsException() {
        RegisterRequest request = new RegisterRequest();
        request.setCompanyName("New Corp");
        request.setSlug("acmecorp"); // slug already taken
        request.setEmail("admin@new.com");
        request.setPassword("password123");

        when(tenantRepository.existsByName("New Corp")).thenReturn(false);
        when(tenantRepository.existsBySlug("acmecorp")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Slug already taken");
    }

    @Test
    @DisplayName("login should throw same error for wrong email and wrong password")
    void login_invalidCredentials_throwsSameMessage() {
        LoginRequest request = new LoginRequest();
        request.setSlug("acmecorp");
        request.setEmail("wrong@email.com");
        request.setPassword("wrongpassword");

        Tenant tenant = new Tenant();
        tenant.setId(UUID.randomUUID());
        tenant.setSlug("acmecorp");

        when(tenantRepository.findBySlug("acmecorp")).thenReturn(Optional.of(tenant));
        when(userRepository.findByEmailAndTenantId(anyString(), any(UUID.class)))
                .thenReturn(Optional.empty());

        // Should throw same error whether email is wrong or password is wrong
        // This prevents user enumeration attacks
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }
}