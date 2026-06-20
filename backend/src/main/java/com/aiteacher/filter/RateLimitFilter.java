package com.aiteacher.filter;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate Limiting Filter — protects API from abuse
 * Uses in-memory token bucket per IP address.
 * For distributed deployment, switch to Bucket4j-Redis.
 */
@Component
public class RateLimitFilter implements Filter {

    // Per-IP buckets: 100 requests per minute per IP
    private static final Map<String, Bucket> ipBuckets = new ConcurrentHashMap<>();

    // Per-user buckets (after auth): 200 requests per minute per user
    private static final Map<Long, Bucket> userBuckets = new ConcurrentHashMap<>();

    private static final int IP_BUCKET_CAPACITY = 100;
    private static final int IP_REFILL_TOKENS = 100;
    private static final Duration IP_REFILL_DURATION = Duration.ofMinutes(1);

    private static final int USER_BUCKET_CAPACITY = 200;
    private static final int USER_REFILL_TOKENS = 200;
    private static final Duration USER_REFILL_DURATION = Duration.ofMinutes(1);

    private Bucket createIpBucket() {
        Bandwidth limit = Bandwidth.classic(IP_BUCKET_CAPACITY,
                Refill.greedy(IP_REFILL_TOKENS, IP_REFILL_DURATION));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket createUserBucket() {
        Bandwidth limit = Bandwidth.classic(USER_BUCKET_CAPACITY,
                Refill.greedy(USER_REFILL_TOKENS, USER_REFILL_DURATION));
        return Bucket.builder().addLimit(limit).build();
    }

    private Bucket getIpBucket(String clientIp) {
        return ipBuckets.computeIfAbsent(clientIp, k -> createIpBucket());
    }

    private Bucket getUserBucket(Long userId) {
        return userBuckets.computeIfAbsent(userId, k -> createUserBucket());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIp(httpRequest);
        String path = httpRequest.getRequestURI();

        // Skip rate limiting for health/actuator endpoints
        if (path.startsWith("/actuator") || path.startsWith("/api/health")) {
            chain.doFilter(request, response);
            return;
        }

        // Check user-tier bucket first (if authenticated)
        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            // For authenticated users, check user bucket
            // Note: extracting real userId from JWT would require JwtService injection
            // Using IP bucket for now; after JWT parsing is wired, use userId
            Bucket bucket = getIpBucket(clientIp);
            if (!bucket.tryConsume(1)) {
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                return;
            }
        } else {
            // Anonymous user — IP bucket
            Bucket bucket = getIpBucket(clientIp);
            if (!bucket.tryConsume(1)) {
                httpResponse.setStatus(429);
                httpResponse.setContentType("application/json");
                httpResponse.getWriter().write("{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}");
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }
}
