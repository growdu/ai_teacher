package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
import com.aiteacher.config.PaymentConfig;
import com.aiteacher.entity.Plan;
import com.aiteacher.entity.Subscription;
import com.aiteacher.mapper.PlanMapper;
import com.aiteacher.service.PaymentService;
import com.aiteacher.service.SubscriptionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Payment Controller - handles payment creation and callbacks
 * Supports WeChat Pay and Alipay
 */
@Slf4j
@RestController
@RequestMapping("/api/payment")
public class PaymentController {

    @Autowired
    private PaymentService paymentService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PlanMapper planMapper;

    @Autowired
    private PaymentConfig paymentConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 获取支持的支付渠道
     */
    @GetMapping("/channels")
    public R<Map<String, Boolean>> getAvailableChannels() {
        Map<String, Boolean> channels = new HashMap<>();
        channels.put("alipay", true);
        channels.put("wechat", true);
        return R.ok(channels);
    }

    /**
     * 创建支付订单
     * Returns: { channel, tradeNo, paymentUrl/codeUrl, expiresAt }
     */
    @PostMapping("/create")
    public R<Map<String, String>> createPayment(@RequestBody Map<String, Object> request) {

        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();
        String channel = (String) request.get("channel");
        Long planId = Long.valueOf(request.get("planId").toString());

        if (channel == null || planId == null) {
            return R.fail("参数不完整");
        }

        // 获取套餐信息
        Plan plan = planMapper.selectById(planId);
        if (plan == null || !"active".equals(plan.getStatus())) {
            return R.fail("无效的套餐");
        }

        // 如果价格为零，直接创建订阅（免费试用）
        if (plan.getPrice() != null && plan.getPrice().compareTo(java.math.BigDecimal.ZERO) == 0) {
            Subscription subscription = subscriptionService.createSubscription(tenantId, planId);
            Map<String, String> freeResult = new HashMap<>();
            freeResult.put("type", "free");
            freeResult.put("subscriptionId", String.valueOf(subscription.getId()));
            return R.ok(freeResult);
        }

        // 创建付费支付
        Map<String, String> paymentResult = paymentService.createPayment(channel, plan, tenantId, userId);
        paymentResult.put("expiresAt", java.time.Instant.now().plusSeconds(3600).toString());

        return R.ok(paymentResult);
    }

    /**
     * 支付宝异步回调
     */
    @PostMapping("/callback/alipay")
    public String alipayCallback(@RequestParam Map<String, String> params) {
        try {
            log.info("Alipay callback received: {}", params);
            // Verify signature before processing
            if (!paymentService.verifyAlipayCallback(params)) {
                log.error("Alipay callback signature verification FAILED");
                return "failure";
            }
            String outTradeNo = params.get("out_trade_no");
            String tradeStatus = params.get("trade_status");
            if (outTradeNo != null && ("TRADE_SUCCESS".equals(tradeStatus) || "TRADE_FINISHED".equals(tradeStatus))) {
                paymentService.activateSubscriptionFromTradeNo(outTradeNo);
            }
            return "success";
        } catch (Exception e) {
            log.error("Alipay callback error: {}", e.getMessage());
            return "failure";
        }
    }

    /**
     * 微信支付异步回调
     */
    @PostMapping("/callback/wechat")
    public String wechatCallback(
            @RequestHeader(value = "Wechatpay-Signature", required = false) String signature,
            @RequestHeader(value = "Wechatpay-Timestamp", required = false) String timestamp,
            @RequestHeader(value = "Wechatpay-Nonce", required = false) String nonce,
            @RequestBody String body) {
        try {
            log.info("WeChat Pay callback received, body: {}", body);
            // Verify signature before processing
            if (paymentConfig.isVerifyWechatSign() && signature != null && timestamp != null && nonce != null) {
                if (!paymentService.verifyWechatCallback(body, signature, timestamp, nonce)) {
                    log.error("WeChat callback signature verification FAILED");
                    return "failure";
                }
            }

            // Parse tradeNo from callback body
            JsonNode node = objectMapper.readTree(body);
            String outTradeNo = node.has("out_trade_no") ? node.get("out_trade_no").asText() : null;
            String tradeState = node.has("trade_state") ? node.get("trade_state").asText() : null;

            if (outTradeNo != null && "SUCCESS".equals(tradeState)) {
                paymentService.activateSubscriptionFromTradeNo(outTradeNo);
            }
            return "success";
        } catch (Exception e) {
            log.error("WeChat callback error: {}", e.getMessage());
            return "failure";
        }
    }
}
