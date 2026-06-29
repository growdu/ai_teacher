package com.aiteacher.interceptor;

import com.aiteacher.entity.ApiUsage;
import com.aiteacher.entity.Plan;
import com.aiteacher.entity.Subscription;
import com.aiteacher.exception.BusinessException;
import com.aiteacher.mapper.ApiUsageMapper;
import com.aiteacher.mapper.PlanMapper;
import com.aiteacher.service.JwtService;
import com.aiteacher.service.SubscriptionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

/**
 * API配额拦截器
 * 
 * 功能：
 * 1. 检查租户的API配额是否充足
 * 2. 记录AI API调用详情（provider、model、调用时间）
 * 3. 提供配额预警机制
 */
@Slf4j
@Component
public class ApiQuotaInterceptor implements HandlerInterceptor {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlanMapper planMapper;

    @Autowired
    private ApiUsageMapper apiUsageMapper;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    // 需要配额检查的AI相关API路径
    private static final String[] AI_API_PATHS = {
        "/api/ai/chat",
        "/api/ai/generate",
        "/api/ai/embed",
        "/api/ppt/generate",
        "/api/video/generate",
        "/api/material/generate"
    };

    // 配额预警阈值（百分比）
    private static final int QUOTA_WARNING_THRESHOLD = 80;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        // 只对需要配额检查的AI API进行拦截
        if (!requiresQuotaCheck(path)) {
            return true;
        }

        // GET请求通常只是查询，不消耗配额
        if ("GET".equalsIgnoreCase(method)) {
            return true;
        }

        // 获取租户ID
        Long tenantId = extractTenantId(request);
        if (tenantId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 获取当前订阅和计划
        Subscription subscription = subscriptionService.getCurrentSubscription(tenantId);
        if (subscription == null) {
            throw new BusinessException(403, "请先订阅服务");
        }

        Plan plan = planMapper.selectById(subscription.getPlanId());
        if (plan == null) {
            throw new BusinessException(403, "订阅信息异常");
        }

        // 检查订阅状态
        if (!"active".equals(subscription.getStatus())) {
            throw new BusinessException(403, "订阅状态异常，请联系客服");
        }

        // 检查订阅是否过期
        if (subscription.getExpiresAt() != null && 
            subscription.getExpiresAt().isBefore(LocalDateTime.now())) {
            subscription.setStatus("expired");
            throw new BusinessException(403, "订阅已过期，请续订后继续使用");
        }

        // -1 表示无限配额
        if (plan.getApiQuota() != null && plan.getApiQuota() != -1) {
            int currentUsage = subscriptionService.getCurrentApiUsage(tenantId);
            int quota = plan.getApiQuota();

            // 配额不足
            if (currentUsage >= quota) {
                log.warn("Tenant {} API quota exceeded: {}/{}", tenantId, currentUsage, quota);
                throw new BusinessException(402,
                    "本月API配额已用完（" + currentUsage + "/" + quota + "），请升级套餐或等待下月重置");
            }

            // 配额预警（超过80%）
            int usagePercent = (currentUsage * 100) / quota;
            if (usagePercent >= QUOTA_WARNING_THRESHOLD) {
                log.info("Tenant {} API quota warning: {}% used ({}/{})",
                    tenantId, usagePercent, currentUsage, quota);
                // 将预警信息放入request属性，供Controller处理
                request.setAttribute("quotaWarning",
                    "配额使用已达" + usagePercent + "%（" + currentUsage + "/" + quota + "），建议升级套餐");
            }
        }

        // 记录租户和计划信息到request，供后续记录调用时使用
        request.setAttribute("tenantId", tenantId);
        request.setAttribute("currentPlan", plan);

        return true;
    }

    /**
     * 请求完成后记录API调用
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                 Object handler, Exception ex) {
        String path = request.getRequestURI();

        // 只对需要配额检查的AI API进行记录
        if (!requiresQuotaCheck(path)) {
            return;
        }

        // 只记录成功或部分成功的请求（2xx状态码）
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            return;
        }

        Long tenantId = (Long) request.getAttribute("tenantId");
        if (tenantId == null) {
            return;
        }

        // 获取provider信息（如果有）
        String providerName = request.getHeader("X-AI-Provider");
        String model = request.getHeader("X-AI-Model");
        String apiType = inferApiType(path);

        try {
            recordApiCall(tenantId, providerName, model, apiType);
            log.debug("Recorded API call for tenant {}, provider: {}, model: {}, type: {}",
                tenantId, providerName, model, apiType);
        } catch (Exception e) {
            log.warn("Failed to record API call: {}", e.getMessage());
        }
    }

    /**
     * 检查路径是否需要配额检查
     */
    private boolean requiresQuotaCheck(String path) {
        for (String apiPath : AI_API_PATHS) {
            if (path.startsWith(apiPath)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 记录API调用
     */
    private void recordApiCall(Long tenantId, String providerName, String model, String apiType) {
        String yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);

        ApiUsage apiUsage = apiUsageMapper.selectOne(
            new LambdaQueryWrapper<ApiUsage>()
                .eq(ApiUsage::getTenantId, tenantId)
                .eq(ApiUsage::getYearMonth, yearMonth)
                .eq(providerName != null, ApiUsage::getProviderName, providerName)
                .eq(model != null, ApiUsage::getModel, model)
        );

        if (apiUsage == null) {
            // 如果没有精确匹配，查找或创建汇总记录
            apiUsage = apiUsageMapper.selectOne(
                new LambdaQueryWrapper<ApiUsage>()
                    .eq(ApiUsage::getTenantId, tenantId)
                    .eq(ApiUsage::getYearMonth, yearMonth)
                    .isNull(ApiUsage::getProviderName)
                    .isNull(ApiUsage::getModel)
            );
            
            if (apiUsage == null) {
                apiUsage = new ApiUsage();
                apiUsage.setTenantId(tenantId);
                apiUsage.setYearMonth(yearMonth);
                apiUsage.setCallCount(1);
                apiUsage.setProviderName(providerName);
                apiUsage.setModel(model);
                apiUsage.setApiType(apiType);
                apiUsage.setUpdatedAt(LocalDateTime.now());
                apiUsage.setCreatedAt(LocalDateTime.now());
                apiUsageMapper.insert(apiUsage);
            } else {
                apiUsage.setCallCount(apiUsage.getCallCount() + 1);
                apiUsage.setUpdatedAt(LocalDateTime.now());
                apiUsageMapper.updateById(apiUsage);
            }
        } else {
            apiUsage.setCallCount(apiUsage.getCallCount() + 1);
            apiUsage.setUpdatedAt(LocalDateTime.now());
            apiUsageMapper.updateById(apiUsage);
        }
    }

    /**
     * 根据路径推断API类型
     */
    private String inferApiType(String path) {
        if (path.contains("/chat")) {
            return "chat";
        } else if (path.contains("/generate")) {
            return "generate";
        } else if (path.contains("/embed")) {
            return "embedding";
        } else if (path.contains("/ppt")) {
            return "ppt";
        } else if (path.contains("/video")) {
            return "video";
        } else if (path.contains("/material")) {
            return "material";
        }
        return "other";
    }

    /**
     * 从请求中提取租户ID
     */
    private Long extractTenantId(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                if (jwtService.validateToken(token)) {
                    return jwtService.getTenantIdFromToken(token);
                }
            }
        } catch (Exception e) {
            log.debug("Failed to extract tenantId: {}", e.getMessage());
        }
        return null;
    }
}
