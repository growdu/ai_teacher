package com.aiteacher.entity;

import com.aiteacher.common.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("teaching_material")
public class TeachingMaterial extends BaseEntity {

    private Long tenantId;
    private Long workspaceId;
    private Long courseId;
    private String materialType;
    private String title;
    private String fileUrl;
    private String thumbnailUrl;
    private Integer duration;
    private Long fileSize;
    private String status;
}