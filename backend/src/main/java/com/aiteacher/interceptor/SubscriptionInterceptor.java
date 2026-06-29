package com.aiteacher.interceptor;

import com.aiteacher.entity.Plan;
import com.aiteacher.entity.Subscription;
import com.aiteacher.exception.BusinessException;
import com.aiteacher.mapper.PlanMapper;
import com.aiteacher.service.JwtService;
import com.aiteacher.service.SubscriptionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 订阅状态校验拦截器
 *
 * 策略：免费用户可以浏览+试用基础功能，高级创作（视频/AI批量生成）需要订阅
 * 流程：了解(免费) → 注册(免费) → 登录(免费) → 试用基础创作(免费) → 付费(解锁高级)
 */
@Slf4j
@Component
public class SubscriptionInterceptor implements HandlerInterceptor {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PlanMapper planMapper;

    // 任何人都可以访问（无需登录订阅）
    private static final String[] PUBLIC_PATHS = {
        "/api/auth/register",
        "/api/auth/login",
        "/api/auth/refresh",
        "/api/health",
        "/api/plans",
        "/api/plans/",
        "/api/payment/create",
        "/api/payment/callback",
        "/api/payment/channels",
        "/api/subscription/status",
        "/api/subscription/upgrade"
    };

    // 需要登录才能访问（免费用户可用）
    private static final String[] FREE_PATHS = {
        "/api/course/list",         // 浏览课程列表
        "/api/course/detail",       // 课程详情
        "/api/knowledge/list",      // 浏览知识库
        "/api/knowledge/detail",    // 知识库详情
        "/api/quiz/list",           // 浏览测验
        "/api/quiz/detail"          // 测验详情
    };

    // 必须付费订阅才能访问
    private static final String[] PREMIUM_PATHS = {
        "/api/course/create",       // 创建课程
        "/api/course/update",       // 更新课程
        "/api/course/delete",       // 删除课程
        "/api/course/publish",      // 发布课程
        "/api/knowledge/create",    // 创建知识库
        "/api/knowledge/update",
        "/api/knowledge/delete",
        "/api/material/upload",     // 上传素材
        "/api/quiz/create",
        "/api/quiz/update",
        "/api/quiz/delete",
        "/api/ai/chat",             // AI 对话
        "/api/ai/generate",         // AI 生成内容
        "/api/ppt/generate",        // PPT 生成
        "/api/video/generate"       // 视频生成
    };

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        boolean isWriteMethod = !"GET".equalsIgnoreCase(method);

        // 公开路径直接放行
        for (String p : PUBLIC_PATHS) {
            if (path.startsWith(p)) return true;
        }

        // 精确匹配免费路径
        for (String p : FREE_PATHS) {
            if (path.equals(p) || path.startsWith(p + "/")) {
                // 免费用户只需登录，不需要订阅
                checkLoginOnly(request);
                return true;
            }
        }

        // 检查是否需要付费订阅
        boolean requiresPremium = false;
        for (String p : PREMIUM_PATHS) {
            if (path.startsWith(p)) {
                requiresPremium = true;
                break;
            }
        }

        if (!requiresPremium) {
            // 其他路径（未分类）默认需要登录
            checkLoginOnly(request);
            return true;
        }

        // ========== 付费订阅校验 ==========

        // 获取登录用户
        Long tenantId = extractTenantId(request);
        if (tenantId == null) {
            throw new BusinessException(401, "请先登录");
        }

        // 检查订阅状态
        Subscription subscription = subscriptionService.getCurrentSubscription(tenantId);
        if (subscription == null) {
            // 无订阅记录 = 免费用户，只能浏览
            throw new BusinessException(403, "此功能需要订阅后才能使用，请先选择订阅计划");
        }

        if (!"active".equals(subscription.getStatus())) {
            String msg = "expired".equals(subscription.getStatus()) || "cancelled".equals(subscription.getStatus())
                ? "订阅已过期，请续费后继续使用"
                : "订阅状态异常，请联系客服";
            throw new BusinessException(403, msg);
        }

        // 配额校验
        int currentUsage = subscriptionService.getCurrentApiUsage(tenantId);
        Plan plan = planMapper.selectById(subscription.getPlanId());
        if (plan != null && plan.getApiQuota() != null && plan.getApiQuota() != -1
                && currentUsage >= plan.getApiQuota()) {
            throw new BusinessException(403,
                "本月配额已用完（" + currentUsage + "/" + plan.getApiQuota() + "），请升级套餐或等待下月重置"
            );
        }

        // 记录一次 API 调用
        try {
            subscriptionService.recordApiCall(tenantId);
        } catch (Exception e) {
            log.warn("Failed to record API call: {}", e.getMessage());
        }

        request.setAttribute("currentSubscription", subscription);
        request.setAttribute("currentPlan", plan);
        return true;
    }

    private void checkLoginOnly(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException(401, "请先登录");
        }
        // 验证 token 是否有效
        String token = authHeader.substring(7);
        try {
            jwtService.validateToken(token);
        } catch (Exception e) {
            throw new BusinessException(401, "登录已过期，请重新登录");
        }
    }

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
