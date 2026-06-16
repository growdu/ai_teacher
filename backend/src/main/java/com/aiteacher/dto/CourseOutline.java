package com.aiteacher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CourseOutline {
    
    private String title;
    private String description;
    private List<Chapter> chapters;
    private Integer totalDuration;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Chapter {
        private String title;
        private Integer duration;
        private List<String> keyPoints;
        private String teachingNotes;
    }
}