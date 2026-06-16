package com.aiteacher.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("ai_config")
public class AiConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private String provider;
    
    private String apiKeyEncrypted;
    
    private String apiSecret;
    
    private String baseUrl;
    
    private String model;
    
    private Boolean enabled;
    
    private Integer priority;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Integer deleted;
}