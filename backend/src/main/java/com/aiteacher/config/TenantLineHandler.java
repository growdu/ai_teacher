package com.aiteacher.config;

import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.handler.TenantLineHandler;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.LongValue;
import org.springframework.stereotype.Component;

/**
 * MyBatis Plus Tenant Line Handler - automatically adds tenant_id filter
 */
@Component
public class TenantLineHandler implements TenantLineHandler {
    
    @Override
    public Expression getTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId != null) {
            return new LongValue(tenantId);
        }
        // Return a value that will match all rows if no tenant is set
        // This is a safety measure - in production, you might want to throw an exception
        return new LongValue(-1L);
    }
    
    @Override
    public String getTenantIdColumn() {
        return "tenant_id";
    }
    
    @Override
    public boolean ignoreTable(String tableName) {
        // Tables that don't have tenant_id column
        return false;
    }
}