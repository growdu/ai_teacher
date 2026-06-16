package com.aiteacher.entity;

import com.aiteacher.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("tenant")
public class Tenant extends BaseEntity {

    private String name;
    private String code;
    private String plan;
    private String quota;
}