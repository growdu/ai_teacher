package com.aiteacher.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CourseGenerateRequest {

    @NotNull(message = "知识点ID不能为空")
    private Long knowledgePointId;

    private String title;

    private Integer chapterCount;
}