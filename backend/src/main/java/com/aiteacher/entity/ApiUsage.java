package com.aiteacher.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("api_usage")
public class ApiUsage {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private String yearMonth;  // format: 2024-01
    
    private Integer callCount;
    
    // 新增：追踪AI Provider使用详情
    private String providerName;  // e.g., "openai", "claude", "qwen"
    
    private String model;        // e.g., "gpt-4", "claude-3-5-sonnet"
    
    private String apiType;       // e.g., "chat", "embedding", "tts", "video"
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
