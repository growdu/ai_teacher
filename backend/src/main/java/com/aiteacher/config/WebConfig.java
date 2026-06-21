package com.aiteacher.config;

import com.aiteacher.interceptor.ApiQuotaInterceptor;
import com.aiteacher.interceptor.SubscriptionInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private SubscriptionInterceptor subscriptionInterceptor;

    @Autowired
    private ApiQuotaInterceptor apiQuotaInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 订阅状态拦截器
        registry.addInterceptor(subscriptionInterceptor)
                .addPathPatterns("/api/**")
                .excludePathPatterns(
                        "/api/auth/**",
                        "/api/health",
                        "/api/plan/list",
                        "/api/payment/create",
                        "/api/payment/callback/**",
                        "/api/subscription/status",
                        "/api/subscription/upgrade",
                        "/actuator/**"
                );

        // API配额拦截器（对AI API进行配额检查和记录）
        registry.addInterceptor(apiQuotaInterceptor)
                .addPathPatterns("/api/ai/**", "/api/ppt/**", "/api/video/**", "/api/material/**")
                .excludePathPatterns(
                        "/api/ai/config/**",
                        "/api/ai/providers"
                );
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // CORS is handled by SecurityConfig — no need to duplicate here
    }
}
