package com.aiteacher.entity;

import com.aiteacher.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("ai_config")
public class AiConfig extends BaseEntity {

    private Long tenantId;
    private String provider;
    private String apiKeyEncrypted;
    private String apiSecret;
    private String baseUrl;
    private String model;
    private Boolean enabled;
    private Integer priority;
}