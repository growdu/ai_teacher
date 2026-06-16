package com.aiteacher.filter;

import com.aiteacher.config.TenantContext;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Tenant Filter - extracts tenant info from JWT token and sets tenant context
 */
@Slf4j
@Component
@Order(1)
public class TenantFilter implements Filter {
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        try {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            
            // Extract tenant info from header or JWT
            String tenantIdHeader = httpRequest.getHeader("X-Tenant-Id");
            String userIdHeader = httpRequest.getHeader("X-User-Id");
            
            if (tenantIdHeader != null && !tenantIdHeader.isEmpty()) {
                TenantContext.setTenantId(Long.parseLong(tenantIdHeader));
            }
            
            if (userIdHeader != null && !userIdHeader.isEmpty()) {
                TenantContext.setUserId(Long.parseLong(userIdHeader));
            }
            
            chain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}