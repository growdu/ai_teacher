package com.aiteacher.config;

import com.aiteacher.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter implements ApplicationRunner {

    @Autowired
    private JwtService jwtService;

    // CORS allowed origins from environment variable
    private static final List<String> ALLOWED_ORIGINS;

    static {
        String corsEnv = System.getenv("CORS_ALLOWED_ORIGINS");
        if (corsEnv != null && !corsEnv.isEmpty()) {
            ALLOWED_ORIGINS = List.of(corsEnv.split(","));
        } else {
            ALLOWED_ORIGINS = List.of("http://localhost:80", "http://localhost:3000");
        }
    }

    @Override
    public void run(ApplicationArguments args) {
        // JWT secret must come from environment variable JWT_SECRET
        String jwtSecret = System.getenv("JWT_SECRET");
        if (jwtSecret == null || jwtSecret.isEmpty()) {
            throw new IllegalStateException("JWT_SECRET environment variable is not set. Application refuses to start.");
        }
        if (jwtSecret.length() < 32) {
            throw new IllegalStateException("JWT_SECRET must be at least 32 characters long. Current length: " + jwtSecret.length());
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Always add CORS headers first — even for 403 responses
        addCorsHeaders(request, response);

        // Public paths that don't require authentication
        String path = request.getRequestURI();
        if (isPublicPath(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);

            try {
                if (jwtService.validateToken(token)) {
                    Long userId = jwtService.getUserIdFromToken(token);
                    String username = jwtService.getUsernameFromToken(token);
                    String role = jwtService.parseToken(token).get("role", String.class);

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    username,
                                    null,
                                    Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role))
                            );

                    authentication.setDetails(userId);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (Exception e) {
                logger.error("JWT validation failed", e);
            }
        }

        filterChain.doFilter(request, response);
    }

    private void addCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        boolean isAllowed = origin != null && ALLOWED_ORIGINS.contains(origin);

        response.setHeader("Access-Control-Allow-Origin", isAllowed ? origin : "");
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        response.setHeader("Access-Control-Allow-Headers",
            "Authorization, Content-Type, X-Requested-With, Origin, Accept");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "3600");
    }

    private boolean isPublicPath(String path) {
        return path.startsWith("/api/auth/") ||
               path.equals("/api/health") ||
               path.startsWith("/actuator/") ||
               path.startsWith("/swagger-ui/") ||
               path.startsWith("/v3/api-docs") ||
               path.startsWith("/api/payment/callback/");
    }
}
