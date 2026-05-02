package com.cloudflow.cloudflow.config;

import com.cloudflow.cloudflow.multitenancy.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter extends OncePerRequestFilter {

    private final RedisTemplate<String, String> redisTemplate;

    @Value("${cloudflow.rate-limit.requests-per-minute:100}")
    private int requestsPerMinute;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // Only rate limit authenticated requests (tenantId is set by TenantFilter)
        // Public endpoints like /auth/login are not rate limited
        UUID tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Build a key per tenant per minute window
        // Example: "rate:tenant-uuid:1715000000" (unix minute timestamp)
        long currentMinute = System.currentTimeMillis() / 60_000;
        String rateLimitKey = "rate:" + tenantId + ":" + currentMinute;

        // INCR atomically increments the counter and returns the new value.
        // If the key doesn't exist, INCR creates it with value 0 first, then increments to 1.
        // This is atomic in Redis — no race condition even with 1000 concurrent requests.
        Long currentCount = redisTemplate.opsForValue().increment(rateLimitKey);

        // On the first request in a window, set the expiry to 65 seconds.
        // 65s (not 60s) provides a small grace window for clock drift between
        // app instances. The key auto-deletes after the window — no cleanup needed.
        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(rateLimitKey, 65, TimeUnit.SECONDS);
        }

        // Add rate limit headers so clients know their current usage
        response.addHeader("X-RateLimit-Limit", String.valueOf(requestsPerMinute));
        response.addHeader("X-RateLimit-Remaining",
                String.valueOf(Math.max(0, requestsPerMinute - (currentCount != null ? currentCount : 0))));

        // Check if limit exceeded
        if (currentCount != null && currentCount > requestsPerMinute) {
            log.warn("Rate limit exceeded for tenant [{}]: {} requests in current minute",
                    tenantId, currentCount);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                    "{\"status\":429,\"error\":\"Too Many Requests\"," +
                            "\"message\":\"Rate limit of " + requestsPerMinute + " requests/minute exceeded\"}"
            );
            return; // Stop the filter chain — don't process the request
        }

        filterChain.doFilter(request, response);
    }
}