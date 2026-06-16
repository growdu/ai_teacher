package com.aiteacher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseGenerateResponse {
    private Long courseId;
    private String title;
    private CourseOutline outline;
    private String script;
    private String status;
}