package com.cloudflow.cloudflow.auth;

import com.cloudflow.cloudflow.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    @Value("${cloudflow.jwt.secret}")
    private String jwtSecret;

    @Value("${cloudflow.jwt.expiration-ms}")
    private long expirationMs;

    // Injected from RedisConfig — our connection to Redis
    private final RedisTemplate<String, String> redisTemplate;

    // Key prefixes — namespacing prevents key collisions in Redis
    // All JWT-related keys start with "jwt:" so they're easy to find and clean up
    private static final String JWT_VALID_PREFIX = "jwt:valid:";
    private static final String JWT_BLACKLIST_PREFIX = "jwt:blacklist:";

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(User user) {
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("tenantId", user.getTenant().getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims validateTokenAndGetClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Checks if a token is valid.
     *
     * Flow:
     * 1. Check blacklist — if present, reject immediately (logout was called)
     * 2. Check cache — if "valid" is cached, skip JWT parsing entirely
     * 3. Parse the JWT — expensive cryptographic operation
     * 4. If valid, cache the result with TTL = remaining token lifetime
     */
    public boolean isTokenValid(String token) {
        String blacklistKey = JWT_BLACKLIST_PREFIX + token;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(blacklistKey))) {
            log.info("TOKEN CHECK: blacklisted — rejecting");
            return false;
        }

        String cacheKey = JWT_VALID_PREFIX + token;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if ("valid".equals(cached)) {
            log.debug("TOKEN CHECK: valid (from cache)");
            return true;
        }

        try {
            Claims claims = validateTokenAndGetClaims(token);
            long remainingMs = claims.getExpiration().getTime() - System.currentTimeMillis();
            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(cacheKey, "valid", remainingMs, TimeUnit.MILLISECONDS);
            }
            log.debug("TOKEN CHECK: valid (freshly parsed)");
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("TOKEN CHECK: invalid — {}", e.getMessage());
            return false;
        }
    }

    /**
     * Blacklists a token on logout.
     *
     * WHY WE NEED THIS:
     * JWTs are stateless — the server doesn't store sessions.
     * If a user logs out, we can't "delete" their token.
     * If someone has a copy of that token, they can still use it until expiry.
     * Blacklisting solves this: on logout, we store the token in Redis with
     * the same TTL as the token's remaining lifetime. Any request with this
     * token gets rejected even before JWT parsing.
     */
    public void blacklistToken(String token) {
        log.info("=== BLACKLIST ATTEMPT START ===");
        log.info("Token length: {}", token != null ? token.length() : "null");
        try {
            Claims claims = validateTokenAndGetClaims(token);
            log.info("Claims extracted successfully. Subject: {}", claims.getSubject());

            long expirationTime = claims.getExpiration().getTime();
            long currentTime = System.currentTimeMillis();
            long remainingMs = expirationTime - currentTime;

            log.info("Expiration time: {}", expirationTime);
            log.info("Current time: {}", currentTime);
            log.info("Remaining ms: {}", remainingMs);

            if (remainingMs > 0) {
                String blacklistKey = JWT_BLACKLIST_PREFIX + token;
                log.info("Setting blacklist key: jwt:blacklist:[token...]");
                redisTemplate.opsForValue().set(
                        blacklistKey, "blacklisted", remainingMs, TimeUnit.MILLISECONDS
                );
                redisTemplate.delete(JWT_VALID_PREFIX + token);
                log.info("=== BLACKLIST SUCCESS ===");
            } else {
                log.warn("Token already expired (remainingMs={}), not blacklisting", remainingMs);
            }
        } catch (Exception e) {
            log.error("=== BLACKLIST FAILED === Exception type: {} Message: {}",
                    e.getClass().getName(), e.getMessage(), e);
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(validateTokenAndGetClaims(token).getSubject());
    }

    public UUID extractTenantId(String token) {
        return UUID.fromString(
                validateTokenAndGetClaims(token).get("tenantId", String.class)
        );
    }
}