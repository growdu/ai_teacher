package com.aiteacher.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("payment")
public class Payment {
    @TableId(type = IdType.AUTO)
    private Long id;

    private String tradeNo;          // AITS-{tenantId}-yyyyMMddHHmmss-{uuid}

    private Long tenantId;

    private Long planId;

    private String channel;           // alipay / wechat

    private String status;            // pending / paid / failed / expired

    private String paidAt;            // ISO timestamp when paid

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;

    // 关联的Plan对象(非数据库字段)
    @TableField(exist = false)
    private Plan plan;
}
