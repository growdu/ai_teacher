package com.aiteacher.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("async_task")
public class AsyncTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private Long userId;
    
    private String taskType;
    
    private String taskName;
    
    private String status;
    
    private Integer progress;
    
    private String currentStep;
    
    private String result;
    
    private String errorMessage;
    
    private String context;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private LocalDateTime completedAt;
}
