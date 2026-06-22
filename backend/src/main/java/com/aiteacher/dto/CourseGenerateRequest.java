package com.aiteacher.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseGenerateRequest {

    @NotNull(message = "知识点ID不能为空")
    private Long knowledgePointId;

    private String title;

    private Integer chapterCount;
}