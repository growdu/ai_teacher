package com.aiteacher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PptGenerateRequest {
    private Long courseId;
    private String template;
    private String style;
    
    // Slide data
    private String title;
    private String subtitle;
    private List<SlideData> slides;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SlideData {
        private String type; // title, chapter, content, image, quiz, summary, end
        private String title;
        private String content;
        private List<String> contentList;
        private List<String> keyPoints;
        private List<String> questions;
        private List<String> points;
        private String imagePath;
        private Integer duration;
    }
}