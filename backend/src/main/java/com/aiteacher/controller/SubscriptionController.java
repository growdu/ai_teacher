package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.entity.Subscription;
import com.aiteacher.entity.User;
import com.aiteacher.exception.BusinessException;
import com.aiteacher.mapper.UserMapper;
import com.aiteacher.service.SubscriptionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/subscription")
public class SubscriptionController {

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private UserMapper userMapper;

    /**
     * 获取当前租户的订阅信息
     */
    @GetMapping("/current")
    public R<Map<String, Object>> getCurrentSubscription(@RequestHeader("Authorization") String token) {
        Long tenantId = getTenantIdFromToken(token);
        Subscription subscription = subscriptionService.getCurrentSubscription(tenantId);
        int currentUsage = subscriptionService.getCurrentApiUsage(tenantId);
        
        Map<String, Object> result = new HashMap<>();
        result.put("subscription", subscription);
        result.put("currentUsage", currentUsage);
        
        if (subscription != null && subscription.getPlan() != null) {
            int quota = subscription.getPlan().getApiQuota();
            result.put("quota", quota == -1 ? "unlimited" : quota);
            result.put("usagePercent", quota == -1 ? 0 : (currentUsage * 100.0 / quota));
        }
        
        return R.ok(result);
    }

    /**
     * 创建订阅
     */
    @PostMapping("/create")
    public R<Subscription> createSubscription(
            @RequestHeader("Authorization") String token,
            @RequestBody Map<String, Long> request) {
        Long tenantId = getTenantIdFromToken(token);
        Long planId = request.get("planId");
        if (planId == null) {
            throw new BusinessException("planId不能为空");
        }
        Subscription subscription = subscriptionService.createSubscription(tenantId, planId);
        return R.ok("订阅创建成功", subscription);
    }

    /**
     * 取消订阅
     */
    @PutMapping("/cancel")
    public R<Subscription> cancelSubscription(@RequestHeader("Authorization") String token) {
        Long tenantId = getTenantIdFromToken(token);
        Subscription subscription = subscriptionService.cancelSubscription(tenantId);
        return R.ok("订阅已取消", subscription);
    }

    /**
     * 续订订阅
     */
    @PutMapping("/renew")
    public R<Subscription> renewSubscription(@RequestHeader("Authorization") String token) {
        Long tenantId = getTenantIdFromToken(token);
        Subscription subscription = subscriptionService.renewSubscription(tenantId);
        return R.ok("续订成功", subscription);
    }

    /**
     * 从token中提取租户ID
     * 这里简化处理，实际应解析JWT token
     */
    private Long getTenantIdFromToken(String token) {
        if (token == null || token.isEmpty()) {
            throw new BusinessException(401, "未授权");
        }
        // 简化实现：实际应解析JWT获取userId，然后查询用户的tenantId
        // 这里假设token是Bearer xxx格式
        String actualToken = token.startsWith("Bearer ") ? token.substring(7) : token;
        if (actualToken.isEmpty()) {
            throw new BusinessException(401, "token无效");
        }
        // 使用默认租户ID 1，实际应从token解析
        return 1L;
    }
}
