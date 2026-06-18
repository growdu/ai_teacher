package com.aiteacher.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("plan")
public class Plan {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String name;
    
    private String description;
    
    private BigDecimal price;
    
    private String billingPeriod;  // monthly, yearly
    
    private String features;  // JSON array as string
    
    private Integer apiQuota;  // -1 means unlimited
    
    private Integer storageQuota;  // GB
    
    private String status;  // active, inactive
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
