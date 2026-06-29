package com.aiteacher.service;

import com.aiteacher.config.PaymentConfig;
import com.aiteacher.entity.Payment;
import com.aiteacher.entity.Plan;
import com.aiteacher.exception.BusinessException;
import com.aiteacher.mapper.PaymentMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Unified Payment Service — WeChat Pay & Alipay (pure HTTP implementation)
 * Uses the existing WebClient dependency — no third-party SDK needed.
 *
 * IMPORTANT: For production use, you MUST fill in real credentials from:
 * - WeChat Pay: https://pay.weixin.qq.com (merchant dashboard)
 * - Alipay: https://open.alipay.com (developer console)
 *
 * This service works in "stub mode" when credentials are not configured —
 * it returns mock payment URLs so the flow can be tested end-to-end.
 */
@Slf4j
@Service
public class PaymentService {

    @Autowired
    private PaymentConfig paymentConfig;

    @Autowired
    private PaymentMapper paymentMapper;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private WebClient.Builder webClientBuilder;

    private WebClient webClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // WeChat Pay API base URL
    private static final String WECHAT_PAY_BASE = "https://api.mch.weixin.qq.com/v3";
    // Alipay API base URLs
    private static final String ALIPAY_SANDBOX_GATEWAY = "https://openapi-sandbox.dl.alipaydev.com/gateway.do";
    private static final String ALIPAY_PRODUCTION_GATEWAY = "https://open.alipayapi.com/gateway.do";
    // Alipay OpenAPI version
    private static final String ALIPAY_FORMAT = "JSON";
    private static final String ALIPAY_VERSION = "1.0";
    private static final String ALIPAY_SIGN_TYPE = "RSA2";

    @PostConstruct
    public void init() {
        this.webClient = webClientBuilder.build();
    }

    /**
     * 创建支付订单
     *
     * @param channel  "alipay" or "wechat"
     * @param plan     subscription plan
     * @param tenantId tenant ID
     * @param userId   user ID (unused but reserved for future use)
     * @return { channel, tradeNo, paymentUrl/codeUrl, expiresAt }
     */
    @Transactional
    public Map<String, String> createPayment(String channel, Plan plan, Long tenantId, Long userId) {
        String outTradeNo = generateTradeNo(tenantId);

        // Save pending payment record first
        Payment payment = new Payment();
        payment.setTradeNo(outTradeNo);
        payment.setTenantId(tenantId);
        payment.setPlanId(plan.getId());
        payment.setChannel(channel);
        payment.setStatus("pending");
        payment.setCreatedAt(LocalDateTime.now());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.insert(payment);

        if (!paymentConfig.isEnabled()) {
            // Stub mode — return a mock payment for testing
            return createStubPayment(channel, plan, tenantId, outTradeNo);
        }

        String subject = "AI Teacher Studio - " + plan.getName() + "订阅";
        long amountCents = plan.getPrice().multiply(BigDecimal.valueOf(100)).longValue();

        return switch (channel.toLowerCase()) {
            case "alipay" -> createAlipayPayment(outTradeNo, subject, amountCents, plan, tenantId);
            case "wechat" -> createWechatPayment(outTradeNo, subject, amountCents, plan, tenantId);
            default -> throw new BusinessException("不支持的支付渠道: " + channel);
        };
    }

    /**
     * Stub payment for testing when payment is disabled
     */
    private Map<String, String> createStubPayment(String channel, Plan plan, Long tenantId, String tradeNo) {
        Map<String, String> result = new HashMap<>();
        result.put("channel", channel);
        result.put("tradeNo", tradeNo);
        result.put("type", "stub");
        result.put("mode", "demo");
        result.put("message", "支付功能已禁用（PAYMENT_ENABLED=false），演示模式");
        result.put("expiresAt", Instant.now().plusSeconds(3600).toString());

        if ("alipay".equals(channel)) {
            result.put("paymentUrl", "https://open.alipay.com/mock/sandbox");
        } else {
            result.put("codeUrl", "weixin://wxpay/bizpayurl?pr=stub");
        }
        return result;
    }

    private Map<String, String> createAlipayPayment(String outTradeNo, String subject,
            long amountCents, Plan plan, Long tenantId) {
        try {
            // Alipay uses yuan (not cents), and expects decimal string
            double amount = amountCents / 100.0;

            Map<String, Object> requestBody = new LinkedHashMap<>();
            requestBody.put("out_trade_no", outTradeNo);
            requestBody.put("total_amount", String.format("%.2f", amount));
            requestBody.put("subject", subject);
            requestBody.put("product_code", "FAST_INSTANT_TRADE_PAY");
            requestBody.put("timeout_express", "1h");
            requestBody.put("notify_url", paymentConfig.getAlipayNotifyUrl());

            ObjectNode bizContent = objectMapper.valueToTree(requestBody);

            Map<String, String> response = callAlipayAPI("alipay.trade.page.pay", bizContent.toString());

            Map<String, String> result = new HashMap<>();
            result.put("channel", "alipay");
            result.put("tradeNo", outTradeNo);
            // alipay.trade.page.pay returns a form HTML string — extract the paymentUrl from response
            result.put("paymentUrl", response.getOrDefault("form", response.getOrDefault("trade_no", "")));
            result.put("expiresAt", Instant.now().plusSeconds(3600).toString());
            return result;

        } catch (Exception e) {
            log.error("Alipay payment creation failed: {}", e.getMessage());
            throw new BusinessException("支付创建失败: " + e.getMessage());
        }
    }

    private Map<String, String> createWechatPayment(String outTradeNo, String subject,
            long amountCents, Plan plan, Long tenantId) {
        try {
            // Build WeChat Pay native payment request
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("out_trade_no", outTradeNo);
            requestBody.put("description", subject);
            requestBody.put("notify_url", paymentConfig.getWechatNotifyUrl());
            requestBody.put("time_expire", Instant.now().plusSeconds(3600).toString());

            ObjectNode amountNode = objectMapper.createObjectNode();
            amountNode.put("total", (int) amountCents);
            amountNode.put("currency", "CNY");
            requestBody.set("amount", amountNode);

            String response = callWechatPayAPI("POST", "/v3/pay/transactions/native", requestBody.toString());
            JsonNode respNode = objectMapper.readTree(response);

            Map<String, String> result = new HashMap<>();
            result.put("channel", "wechat");
            result.put("tradeNo", outTradeNo);
            result.put("codeUrl", respNode.has("code_url") ? respNode.get("code_url").asText() : "");
            result.put("expiresAt", Instant.now().plusSeconds(3600).toString());
            return result;

        } catch (Exception e) {
            log.error("WeChat payment creation failed: {}", e.getMessage());
            throw new BusinessException("支付创建失败: " + e.getMessage());
        }
    }

    /**
     * 从支付回调激活订阅
     * tradeNo 格式: AITS-{tenantId}-yyyyMMddHHmmss-{uuid}
     */
    @Transactional
    public void activateSubscriptionFromTradeNo(String tradeNo) {
        if (tradeNo == null || !tradeNo.startsWith("AITS-")) {
            log.warn("Invalid tradeNo format: {}", tradeNo);
            return;
        }

        // Parse tenantId from tradeNo: AITS-{tenantId}-...
        String[] parts = tradeNo.split("-");
        if (parts.length < 2) {
            log.warn("Cannot parse tenantId from tradeNo: {}", tradeNo);
            return;
        }

        Long tenantId;
        try {
            tenantId = Long.valueOf(parts[1]);
        } catch (NumberFormatException e) {
            log.warn("Cannot parse tenantId from tradeNo: {}", tradeNo);
            return;
        }

        // Find the payment record
        Payment payment = paymentMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Payment>()
                        .eq(Payment::getTradeNo, tradeNo)
                        .last("LIMIT 1")
        );

        if (payment == null) {
            log.warn("Payment record not found for tradeNo: {}", tradeNo);
            return;
        }

        if ("paid".equals(payment.getStatus())) {
            log.info("Payment already activated: {}", tradeNo);
            return;
        }

        // Mark payment as paid
        payment.setStatus("paid");
        payment.setPaidAt(Instant.now().toString());
        payment.setUpdatedAt(LocalDateTime.now());
        paymentMapper.updateById(payment);

        // Activate the subscription
        try {
            subscriptionService.createSubscription(tenantId, payment.getPlanId());
            log.info("Subscription activated for tenant {} via trade {}", tenantId, tradeNo);
        } catch (Exception e) {
            log.error("Failed to activate subscription for tenant {}: {}", tenantId, e.getMessage());
        }
    }

    /**
     * 查询微信支付订单状态
     */
    public String queryWechatOrder(String outTradeNo) {
        try {
            String response = callWechatPayAPI("GET",
                    "/v3/pay/transactions/out-trade-no/" + outTradeNo + "?mchid=" + paymentConfig.getWechatMchId(),
                    null);
            JsonNode node = objectMapper.readTree(response);
            return node.has("trade_state") ? node.get("trade_state").asText() : "UNKNOWN";
        } catch (Exception e) {
            log.error("WeChat query failed: {}", e.getMessage());
            return "ERROR";
        }
    }

    // ========== HTTP Call Helpers ==========

    private Map<String, String> callAlipayAPI(String method, String bizContent) {
        if (paymentConfig.getAlipayAppId() == null || paymentConfig.getAlipayAppId().isEmpty()) {
            throw new BusinessException("支付宝未配置（ALIPAY_APP_ID 为空）");
        }

        try {
            long timestamp = System.currentTimeMillis() / 1000;
            String nonceStr = UUID.randomUUID().toString().replace("-", "");

            // Choose gateway: sandbox or production
            String gateway = (paymentConfig.isSandboxEnabled()
                    || "sandbox".equalsIgnoreCase(paymentConfig.getEnvironment()))
                    ? ALIPAY_SANDBOX_GATEWAY
                    : ALIPAY_PRODUCTION_GATEWAY;

            // Alipay RSA2 sign: sign_string = biz_content (just the JSON body)
            String signature = signAlipay(bizContent, paymentConfig.getAlipayPrivateKey());

            // Build Authorization header: Alipay auth is "app_id:signature"
            String authHeader = paymentConfig.getAlipayAppId() + ":" + signature;

            // Build query string params for GET-style param passing (Alipay OpenAPI pattern)
            StringBuilder queryBuilder = new StringBuilder();
            queryBuilder.append("app_id=").append(paymentConfig.getAlipayAppId());
            queryBuilder.append("&method=").append(method);
            queryBuilder.append("&timestamp=").append(timestamp);
            queryBuilder.append("&biz_content=").append(java.net.URLEncoder.encode(bizContent, StandardCharsets.UTF_8));
            queryBuilder.append("&sign_type=").append(ALIPAY_SIGN_TYPE);
            queryBuilder.append("&version=").append(ALIPAY_VERSION);
            queryBuilder.append("&format=").append(ALIPAY_FORMAT);
            queryBuilder.append("&notify_url=").append(paymentConfig.getAlipayNotifyUrl() != null
                    ? java.net.URLEncoder.encode(paymentConfig.getAlipayNotifyUrl(), StandardCharsets.UTF_8) : "");
            queryBuilder.append("&charset=").append("UTF-8");
            queryBuilder.append("&sign=").append(java.net.URLEncoder.encode(signature, StandardCharsets.UTF_8));
            queryBuilder.append("&sdk_version=").append("1.0");

            // For alipay.trade.page.pay, we need to POST with these params as query string
            String response = webClient.post()
                    .uri(gateway + "?" + queryBuilder.toString())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .header("Authorization", authHeader)
                    .header("X-Ca-Timestamp", String.valueOf(timestamp))
                    .header("X-Ca-Nonce", nonceStr)
                    .bodyValue("")  // empty body, params in query string
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            Map<String, String> result = new HashMap<>();
            result.put("response", response != null ? response : "");
            if (response != null) {
                // alipay.trade.page.pay returns { "alipay_trade_page_pay_response": { "trade_no": "...", "form": "..." } }
                JsonNode node = objectMapper.readTree(response);
                JsonNode respNode = node.has(method + "_response")
                        ? node.get(method + "_response")
                        : node;
                if (respNode.has("trade_no")) result.put("trade_no", respNode.get("trade_no").asText());
                if (respNode.has("form")) result.put("form", respNode.get("form").asText());
                if (respNode.has("msg")) result.put("msg", respNode.get("msg").asText());
                if (respNode.has("code") && !"10000".equals(respNode.get("code").asText())) {
                    log.warn("Alipay API error: {} - {}", respNode.get("code"), respNode.get("msg"));
                }
            }
            return result;

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Alipay API call failed: {}", e.getMessage());
            throw new BusinessException("支付宝接口调用失败: " + e.getMessage());
        }
    }

    private String callWechatPayAPI(String method, String path, String requestBody) {
        if (paymentConfig.getWechatMchId() == null || paymentConfig.getWechatMchId().isEmpty()) {
            throw new BusinessException("微信支付未配置（WECHAT_MCH_ID 为空）");
        }

        try {
            long timestamp = Instant.now().getEpochSecond();
            String nonceStr = UUID.randomUUID().toString().replace("-", "");

            // Build signature
            String signStr = timestamp + "\n" + nonceStr + "\n" + (requestBody != null ? requestBody : "") + "\n";
            String signature = signWechatPay(signStr);

            String token = "WECHATPAY2-SHA256-RSA2048 mchid=\"" + paymentConfig.getWechatMchId()
                    + "\",nonce_str=\"" + nonceStr + "\",timestamp=\"" + timestamp
                    + "\",signature=\"" + signature + "\",serial_no=\""
                    + paymentConfig.getWechatSerialNo() + "\"";

            WebClient.RequestBodySpec reqSpec = webClient.method(
                    "GET".equals(method) ? HttpMethod.GET : HttpMethod.POST)
                    .uri(WECHAT_PAY_BASE + path)
                    .header("Authorization", token)
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Wechatpay-Timestamp", String.valueOf(timestamp))
                    .header("Wechatpay-Nonce", nonceStr);

            String response;
            if (requestBody != null && !"GET".equals(method)) {
                response = reqSpec.bodyValue(requestBody).retrieve().bodyToMono(String.class).block();
            } else {
                response = reqSpec.retrieve().bodyToMono(String.class).block();
            }

            return response != null ? response : "{}";

        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("WeChat Pay API call failed: {}", e.getMessage());
            throw new BusinessException("微信支付接口调用失败: " + e.getMessage());
        }
    }

    // ========== Signature Helpers ==========

    private String signAlipay(String data, String privateKey) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey key = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(privateKey)));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(key);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            log.warn("Alipay signing failed: {}", e.getMessage());
            return "";
        }
    }

    private String signWechatPay(String message) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PrivateKey key = kf.generatePrivate(new java.security.spec.PKCS8EncodedKeySpec(
                    Base64.getDecoder().decode(paymentConfig.getWechatPrivateKey())));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(key);
            sig.update(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(sig.sign());
        } catch (Exception e) {
            log.warn("WeChat Pay signing failed: {}", e.getMessage());
            return "";
        }
    }

    private String generateTradeNo(Long tenantId) {
        return "AITS-" + tenantId + "-" + DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
                .format(java.time.LocalDateTime.now()) + "-" +
                UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    /**
     * 验证微信支付回调签名
     */
    public boolean verifyWechatCallback(String body, String signature,
            String timestamp, String nonce) {
        try {
            String message = timestamp + "\n" + nonce + "\n" + body + "\n";
            String expectedSig = signWechatPay(message);
            return expectedSig.equals(signature);
        } catch (Exception e) {
            log.error("WeChat signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 验证支付宝异步回调签名
     * @param params 所有回调参数（包含 sign 字段）
     * @return true 如果验签通过
     */
    public boolean verifyAlipayCallback(Map<String, String> params) {
        if (!paymentConfig.isVerifyAlipaySign()) {
            log.warn("Alipay sign verification is disabled, skipping");
            return true;
        }
        if (paymentConfig.getAlipayPublicKey() == null || paymentConfig.getAlipayPublicKey().isEmpty()) {
            log.warn("Alipay public key not configured, skipping verification");
            return true;
        }
        try {
            String sign = params.get("sign");
            String signType = params.get("sign_type");
            if (sign == null || sign.isEmpty()) {
                log.warn("Alipay callback missing sign");
                return false;
            }
            // Build sign string: all params except sign and sign_type, sorted by key
            StringBuilder sb = new StringBuilder();
            params.entrySet().stream()
                .filter(e -> !"sign".equals(e.getKey()) && !"sign_type".equals(e.getKey()))
                .sorted(Map.Entry.comparingByKey())
                .forEach(e -> {
                    if (sb.length() > 0) sb.append("&");
                    sb.append(e.getKey()).append("=").append(e.getValue());
                });
            String signData = sb.toString();
            return verifyRSA2(signData, sign, paymentConfig.getAlipayPublicKey());
        } catch (Exception e) {
            log.error("Alipay sign verification error: {}", e.getMessage());
            return false;
        }
    }

    private boolean verifyRSA2(String data, String signature, String publicKey) {
        try {
            KeyFactory kf = KeyFactory.getInstance("RSA");
            PublicKey key = kf.generatePublic(new java.security.spec.X509EncodedKeySpec(
                    Base64.getDecoder().decode(publicKey)));
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initVerify(key);
            sig.update(data.getBytes(StandardCharsets.UTF_8));
            return sig.verify(Base64.getDecoder().decode(signature));
        } catch (Exception e) {
            log.error("RSA2 verification failed: {}", e.getMessage());
            return false;
        }
    }
}
