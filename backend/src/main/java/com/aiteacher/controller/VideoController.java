package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
import com.aiteacher.dto.VideoGenerateRequest;
import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.service.VideoGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * Video Generation Controller
 * Endpoints for async video generation from course content
 */
@Slf4j
@RestController
@RequestMapping("/api/video")
public class VideoController {

    @Autowired
    private VideoGenerationService videoGenerationService;

    /**
     * Generate video asynchronously
     * Returns task ID for polling progress
     */
    @PostMapping("/generate")
    public R<Long> generateVideoAsync(@RequestBody VideoGenerateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }

        Long userId = TenantContext.getUserId();
        Long taskId = videoGenerationService.generateVideoAsync(request, userId, tenantId);
        log.info("Video generation started: taskId={}, courseId={}", taskId, request.getCourseId());
        return R.ok(taskId);
    }

    /**
     * Generate video synchronously (for small videos, blocking)
     * Returns the generated teaching material directly
     */
    @PostMapping("/generate/sync")
    public R<TeachingMaterial> generateVideoSync(@RequestBody VideoGenerateRequest request) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }

        Long userId = TenantContext.getUserId();
        TeachingMaterial material = videoGenerationService.generateVideo(request, userId, tenantId);
        log.info("Video generated sync: materialId={}, courseId={}", material.getId(), request.getCourseId());
        return R.ok(material);
    }
}
