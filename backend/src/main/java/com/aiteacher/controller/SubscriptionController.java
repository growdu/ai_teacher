package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
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
    public R<Map<String, Object>> getCurrentSubscription() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(401, "未授权");
        }
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
    public R<Subscription> createSubscription(@RequestBody Map<String, Long> request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(401, "未授权");
        }
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
    public R<Subscription> cancelSubscription() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(401, "未授权");
        }
        Subscription subscription = subscriptionService.cancelSubscription(tenantId);
        return R.ok("订阅已取消", subscription);
    }

    /**
     * 续订订阅
     */
    @PutMapping("/renew")
    public R<Subscription> renewSubscription() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new BusinessException(401, "未授权");
        }
        Subscription subscription = subscriptionService.renewSubscription(tenantId);
        return R.ok("续订成功", subscription);
    }
}
