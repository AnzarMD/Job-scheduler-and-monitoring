package com.cloudflow.cloudflow.config;

import com.cloudflow.cloudflow.multitenancy.TenantFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final TenantFilter tenantFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints — no token needed
                        .requestMatchers(
                                "/api/v1/auth/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/actuator/health",
                                "/actuator/prometheus"
                        ).permitAll()
                        // Everything else requires a valid JWT
                        .anyRequest().authenticated()
                )
                // Run TenantFilter before Spring's own auth filter
                .addFilterBefore(tenantFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
//    {
//        "email": "admin@acme.com",
//            "password": "password123",
//            "slug": "acmecorp"
    //eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJjNGFhYWYyYi0xNzQ0LTQwMmItYjUwNy05ZjEwZGQ2Mzk4ZmEiLCJ0ZW5hbnRJZCI6IjZkMTE1NzU3LTgzZmQtNGQ0ZC1hNDA2LTg5NzRjNmFiMDYwNSIsImVtYWlsIjoiYWRtaW5AYWNtZS5jb20iLCJyb2xlIjoiT1dORVIiLCJpYXQiOjE3NzczMjg5MDksImV4cCI6MTc3NzQxNTMwOX0.qciG5Uc-oYKpgwITUXTMvoONAhHfS-UxQqvnyTBsoBZV3o-0McQmd335c2JR-Q5lqBOgOb8Buv_hnf67Rqw2RQ
//    }
}