package com.aiteacher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizGenerateRequest {
    private Long courseId;
    private String subject;
    private String grade;
    private String difficulty; // easy, medium, hard
    private Integer count; // number of questions
    private String type; // choice, blank, essay, mixed
}