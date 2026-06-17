package com.aiteacher.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.TenantLineInnerInterceptor;
import org.apache.ibatis.type.TypeHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();

        // Tenant line interceptor
        interceptor.addInnerInterceptor(new TenantLineInnerInterceptor(new CustomTenantLineHandler()));

        // Note: PaginationInnerInterceptor removed - causes issues with TenantLine
        // If pagination is needed, use manual LIMIT/OFFSET in @Select queries

        return interceptor;
    }

    // Jackson JSON type handler removed - caused parameter binding issues
    // If JSONB columns need special handling, add explicit @TableField annotations
    // or use resultType mapping instead
}