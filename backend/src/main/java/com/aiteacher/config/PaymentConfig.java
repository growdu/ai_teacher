package com.aiteacher.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Payment Configuration - WeChat Pay & Alipay
 * All sensitive values come from environment variables.
 */
@Data
@Component
@ConfigurationProperties(prefix = "payment")
public class PaymentConfig {

    // ========== Alipay ==========
    private String alipayAppId;
    private String alipayPrivateKey;
    private String alipayPublicKey;
    private String alipayNotifyUrl;
    private String alipayReturnUrl;

    // ========== WeChat Pay ==========
    private String wechatMchId;
    private String wechatSerialNo;
    private String wechatPrivateKey;
    private String wechatApiV3Key;
    private String wechatNotifyUrl;
    private String wechatAppId;

    // ========== Common ==========
    private boolean enabled = false;
    private String environment = "production"; // "production" or "sandbox"
}
