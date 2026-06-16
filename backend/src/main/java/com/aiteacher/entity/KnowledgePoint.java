package com.aiteacher.entity;

import com.aiteacher.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_point")
public class KnowledgePoint extends BaseEntity {

    private Long tenantId;
    private Long workspaceId;
    private String subject;
    private String grade;
    private String content;
    private String tags;
}