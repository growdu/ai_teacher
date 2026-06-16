package com.aiteacher.entity;

import com.aiteacher.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("course")
public class Course extends BaseEntity {

    private Long tenantId;
    private Long workspaceId;
    private Long knowledgePointId;
    private String title;
    private String outline;
    private String script;
    private String status;
    private Long creatorId;
}