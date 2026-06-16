package com.aiteacher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoGenerateRequest {
    private Long courseId;
    private Long pptId;
    private String avatarId;
    private String voiceId;
    private Boolean hasAvatar;
    private Boolean hasPptBackground;
    private String script;
}