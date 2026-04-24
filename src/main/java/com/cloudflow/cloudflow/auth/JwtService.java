package com.cloudflow.cloudflow.auth;

import com.cloudflow.cloudflow.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    @Value("${cloudflow.jwt.secret}")
    private String jwtSecret;

    @Value("${cloudflow.jwt.expiration-ms}")
    private long expirationMs;

    // Builds a SecretKey from the hex string in application.yml
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    // Generates a JWT token containing userId and tenantId as claims
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())               // "sub" claim = userId
                .claim("tenantId", user.getTenant().getId().toString())  // custom claim
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    // Parses and validates the token, returns all claims
    public Claims validateTokenAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(validateTokenAndGetClaims(token).getSubject());
    }

    public UUID extractTenantId(String token) {
        return UUID.fromString(
                validateTokenAndGetClaims(token).get("tenantId", String.class)
        );
    }

    public boolean isTokenValid(String token) {
        try {
            validateTokenAndGetClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}