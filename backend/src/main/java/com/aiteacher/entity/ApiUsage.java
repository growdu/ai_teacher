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
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
