package com.aiteacher.service;

import com.aiteacher.entity.ApiUsage;
import com.aiteacher.entity.Plan;
import com.aiteacher.entity.Subscription;
import com.aiteacher.exception.BusinessException;
import com.aiteacher.mapper.ApiUsageMapper;
import com.aiteacher.mapper.PlanMapper;
import com.aiteacher.mapper.SubscriptionMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
public class SubscriptionService {

    @Autowired
    private SubscriptionMapper subscriptionMapper;

    @Autowired
    private PlanMapper planMapper;

    @Autowired
    private ApiUsageMapper apiUsageMapper;

    private static final DateTimeFormatter YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM");

    /**
     * 获取租户当前订阅
     */
    public Subscription getCurrentSubscription(Long tenantId) {
        Subscription subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getTenantId, tenantId)
                .eq(Subscription::getStatus, "active")
                .orderByDesc(Subscription::getCreatedAt)
                .last("LIMIT 1")
        );
        
        if (subscription != null) {
            // 加载关联的Plan
            Plan plan = planMapper.selectById(subscription.getPlanId());
            subscription.setPlan(plan);
        }
        
        return subscription;
    }

    /**
     * 创建订阅
     */
    @Transactional
    public Subscription createSubscription(Long tenantId, Long planId) {
        // 检查套餐是否存在
        Plan plan = planMapper.selectById(planId);
        if (plan == null || !"active".equals(plan.getStatus())) {
            throw new BusinessException("无效的套餐");
        }

        // 取消现有订阅
        List<Subscription> existingSubs = subscriptionMapper.selectList(
            new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getTenantId, tenantId)
                .eq(Subscription::getStatus, "active")
        );
        for (Subscription sub : existingSubs) {
            sub.setStatus("cancelled");
            sub.setAutoRenew(false);
            sub.setUpdatedAt(LocalDateTime.now());
            subscriptionMapper.updateById(sub);
        }

        // 创建新订阅
        Subscription subscription = new Subscription();
        subscription.setTenantId(tenantId);
        subscription.setPlanId(planId);
        subscription.setStatus("active");
        subscription.setStartedAt(LocalDateTime.now());
        subscription.setAutoRenew(true);
        
        // 根据计费周期计算过期时间
        if ("yearly".equals(plan.getBillingPeriod())) {
            subscription.setExpiresAt(LocalDateTime.now().plusYears(1));
        } else {
            subscription.setExpiresAt(LocalDateTime.now().plusMonths(1));
        }
        
        subscription.setCreatedAt(LocalDateTime.now());
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionMapper.insert(subscription);
        
        subscription.setPlan(plan);
        return subscription;
    }

    /**
     * 取消订阅
     */
    @Transactional
    public Subscription cancelSubscription(Long tenantId) {
        Subscription subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getTenantId, tenantId)
                .eq(Subscription::getStatus, "active")
                .orderByDesc(Subscription::getCreatedAt)
                .last("LIMIT 1")
        );
        
        if (subscription == null) {
            throw new BusinessException("当前没有有效订阅");
        }
        
        subscription.setStatus("cancelled");
        subscription.setAutoRenew(false);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionMapper.updateById(subscription);
        
        // 加载关联的Plan
        Plan plan = planMapper.selectById(subscription.getPlanId());
        subscription.setPlan(plan);
        
        return subscription;
    }

    /**
     * 续订订阅
     */
    @Transactional
    public Subscription renewSubscription(Long tenantId) {
        Subscription subscription = subscriptionMapper.selectOne(
            new LambdaQueryWrapper<Subscription>()
                .eq(Subscription::getTenantId, tenantId)
                .eq(Subscription::getStatus, "active")
                .orderByDesc(Subscription::getCreatedAt)
                .last("LIMIT 1")
        );
        
        if (subscription == null) {
            throw new BusinessException("当前没有有效订阅");
        }
        
        Plan plan = planMapper.selectById(subscription.getPlanId());
        if (plan == null) {
            throw new BusinessException("订阅套餐不存在");
        }
        
        // 延长过期时间
        if ("yearly".equals(plan.getBillingPeriod())) {
            subscription.setExpiresAt(subscription.getExpiresAt().plusYears(1));
        } else {
            subscription.setExpiresAt(subscription.getExpiresAt().plusMonths(1));
        }
        
        subscription.setAutoRenew(true);
        subscription.setUpdatedAt(LocalDateTime.now());
        subscriptionMapper.updateById(subscription);
        
        subscription.setPlan(plan);
        return subscription;
    }

    /**
     * 校验API配额
     * @return true 如果配额充足
     * @throws BusinessException 如果配额超限 (code 402)
     */
    public void checkApiQuota(Long tenantId) {
        // 获取当前订阅
        Subscription subscription = getCurrentSubscription(tenantId);
        if (subscription == null) {
            throw new BusinessException(402, "请先订阅服务");
        }
        
        Plan plan = subscription.getPlan();
        if (plan == null) {
            throw new BusinessException(402, "订阅信息异常");
        }
        
        // 检查订阅是否过期
        if (subscription.getExpiresAt().isBefore(LocalDateTime.now())) {
            subscription.setStatus("expired");
            subscriptionMapper.updateById(subscription);
            throw new BusinessException(402, "订阅已过期，请续订");
        }
        
        // -1 表示无限配额
        if (plan.getApiQuota() == -1) {
            return;
        }
        
        // 获取当月API使用量
        String yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        ApiUsage apiUsage = apiUsageMapper.selectOne(
            new LambdaQueryWrapper<ApiUsage>()
                .eq(ApiUsage::getTenantId, tenantId)
                .eq(ApiUsage::getYearMonth, yearMonth)
        );
        
        int currentUsage = apiUsage != null ? apiUsage.getCallCount() : 0;
        
        if (currentUsage >= plan.getApiQuota()) {
            throw new BusinessException(402, "API配额已用完，请升级套餐或等待下月重置");
        }
    }

    /**
     * 记录API调用
     */
    @Transactional
    public void recordApiCall(Long tenantId) {
        String yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        
        ApiUsage apiUsage = apiUsageMapper.selectOne(
            new LambdaQueryWrapper<ApiUsage>()
                .eq(ApiUsage::getTenantId, tenantId)
                .eq(ApiUsage::getYearMonth, yearMonth)
        );
        
        if (apiUsage == null) {
            apiUsage = new ApiUsage();
            apiUsage.setTenantId(tenantId);
            apiUsage.setYearMonth(yearMonth);
            apiUsage.setCallCount(1);
            apiUsage.setUpdatedAt(LocalDateTime.now());
            apiUsageMapper.insert(apiUsage);
        } else {
            apiUsage.setCallCount(apiUsage.getCallCount() + 1);
            apiUsage.setUpdatedAt(LocalDateTime.now());
            apiUsageMapper.updateById(apiUsage);
        }
    }

    /**
     * 获取当月API使用量
     */
    public int getCurrentApiUsage(Long tenantId) {
        String yearMonth = YearMonth.now().format(YEAR_MONTH_FORMATTER);
        ApiUsage apiUsage = apiUsageMapper.selectOne(
            new LambdaQueryWrapper<ApiUsage>()
                .eq(ApiUsage::getTenantId, tenantId)
                .eq(ApiUsage::getYearMonth, yearMonth)
        );
        return apiUsage != null ? apiUsage.getCallCount() : 0;
    }
}
