package com.aiteacher.filter;

import com.aiteacher.config.TenantContext;
import com.aiteacher.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Tenant Filter - extracts tenant info from JWT token and sets tenant context
 * Tenant ID must come from JWT claims, NOT from HTTP headers (prevents header injection)
 */
@Slf4j
@Component
@Order(1)
public class TenantFilter implements Filter {

    @Autowired
    private JwtService jwtService;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;

            // Extract tenant info from JWT token (NOT from headers)
            String authHeader = httpRequest.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                try {
                    Long tenantId = jwtService.getTenantIdFromToken(token);
                    Long userId = jwtService.getUserIdFromToken(token);
                    if (tenantId != null) {
                        TenantContext.setTenantId(tenantId);
                    }
                    if (userId != null) {
                        TenantContext.setUserId(userId);
                    }
                } catch (Exception e) {
                    // Token invalid or expired, context remains empty
                    log.debug("Could not extract tenant info from JWT: {}", e.getMessage());
                }
            }

            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}