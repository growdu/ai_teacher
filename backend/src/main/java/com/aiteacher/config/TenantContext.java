package com.aiteacher.config;

/**
 * Tenant Context - stores current tenant information in thread local
 */
public class TenantContext {
    
    private static final ThreadLocal<Long> CURRENT_TENANT = new ThreadLocal<>();
    private static final ThreadLocal<Long> CURRENT_USER = new ThreadLocal<>();
    
    public static void setTenantId(Long tenantId) {
        CURRENT_TENANT.set(tenantId);
    }
    
    public static Long getTenantId() {
        return CURRENT_TENANT.get();
    }
    
    public static void setUserId(Long userId) {
        CURRENT_USER.set(userId);
    }
    
    public static Long getUserId() {
        return CURRENT_USER.get();
    }
    
    public static void clear() {
        CURRENT_TENANT.remove();
        CURRENT_USER.remove();
    }
}