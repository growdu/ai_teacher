package com.aiteacher.config;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

/**
 * MyBatis Plus Tenant Line Handler - automatically adds tenant_id filter
 */
@Component
public class CustomTenantLineHandler implements com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler {
    
    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return new LongValue(tenantId);
        }
        return new LongValue(-1L);
    }
    
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
    
    @Override
    public boolean ignoreTable(String tableName) {
        // Users, auth tables don't have tenant_id or shouldn't be tenant-filtered
        return "users".equals(tableName) || "user".equals(tableName);
    }
}
