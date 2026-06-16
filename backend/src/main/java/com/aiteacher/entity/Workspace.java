package com.aiteacher.entity;

import com.aiteacher.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("workspace")
public class Workspace extends BaseEntity {

    private Long tenantId;
    private String name;
    private String settings;
}