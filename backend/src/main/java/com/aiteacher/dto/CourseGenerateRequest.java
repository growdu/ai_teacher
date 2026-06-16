package com.aiteacher.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CourseGenerateRequest {
    
    @NotBlank(message = "知识点内容不能为空")
    private String knowledgePoint;
    
    private String subject;
    
    private String grade;
    
    private Integer chaptersCount;
    
    private String targetDuration;
}