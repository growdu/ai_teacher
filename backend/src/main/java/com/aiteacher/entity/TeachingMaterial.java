package com.aiteacher.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("teaching_material")
public class TeachingMaterial {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private Long tenantId;
    
    private Long workspaceId;
    
    private Long courseId;
    
    private Long knowledgePointId;
    
    private String materialType;
    
    private String title;
    
    private String fileUrl;
    
    private String thumbnailUrl;
    
    private Integer duration;
    
    private Long fileSize;
    
    private String status;
    
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
    
    private Boolean deleted;
}