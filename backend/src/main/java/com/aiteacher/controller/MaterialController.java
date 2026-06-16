package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.dto.PptGenerateRequest;
import com.aiteacher.dto.VideoGenerateRequest;
import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.service.PptGenerationService;
import com.aiteacher.service.TeachingMaterialService;
import com.aiteacher.service.VideoGenerationService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/material")
public class MaterialController {

    @Autowired
    private PptGenerationService pptGenerationService;

    @Autowired
    private VideoGenerationService videoGenerationService;

    @Autowired
    private TeachingMaterialService teachingMaterialService;

    /**
     * Generate PPT from course
     */
    @PostMapping("/ppt/generate")
    public R<Object> generatePpt(
            @RequestBody PptGenerateRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        TeachingMaterial material = pptGenerationService.generatePpt(request.getCourseId(), request.getTemplate(), userId);
        return R.ok(Map.of(
                "materialId", material.getId(),
                "fileUrl", material.getFileUrl()
        ));
    }

    /**
     * Generate video from course
     */
    @PostMapping("/video/generate")
    public R<Map<String, Object>> generateVideo(
            @RequestBody VideoGenerateRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        Long tenantId = getTenantId(authentication);
        
        // Generate asynchronously
        Long taskId = videoGenerationService.generateVideoAsync(request, userId, tenantId);
        
        return R.ok(Map.of(
                "taskId", taskId,
                "status", "processing"
        ));
    }

    /**
     * Get task status
     */
    @GetMapping("/task/{taskId}")
    public R<Map<String, Object>> getTaskStatus(@PathVariable Long taskId) {
        return R.ok(Map.of(
                "taskId", taskId
                // Status is fetched from AsyncTaskService
        ));
    }

    /**
     * List teaching materials
     */
    @GetMapping("/list")
    public R<List<TeachingMaterial>> list(
            @RequestParam(required = false) Long courseId,
            @RequestParam(required = false) String materialType,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        LambdaQueryWrapper<TeachingMaterial> wrapper = new LambdaQueryWrapper<>();
        if (courseId != null) wrapper.eq(TeachingMaterial::getCourseId, courseId);
        if (materialType != null) wrapper.eq(TeachingMaterial::getMaterialType, materialType);
        wrapper.orderByDesc(TeachingMaterial::getCreatedAt);
        return R.ok(teachingMaterialService.list(wrapper));
    }

    /**
     * Page query teaching materials
     */
    @GetMapping("/page")
    public R<Page<TeachingMaterial>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String materialType,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        Page<TeachingMaterial> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<TeachingMaterial> wrapper = new LambdaQueryWrapper<>();
        if (materialType != null) wrapper.eq(TeachingMaterial::getMaterialType, materialType);
        wrapper.orderByDesc(TeachingMaterial::getCreatedAt);
        return R.ok(teachingMaterialService.page(page, wrapper));
    }

    /**
     * Get material by ID
     */
    @GetMapping("/{id}")
    public R<TeachingMaterial> getById(@PathVariable Long id) {
        return R.ok(teachingMaterialService.getById(id));
    }

    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() != null) {
            return (Long) authentication.getDetails();
        }
        return 1L;
    }

    private Long getTenantId(Authentication authentication) {
        // In production, get from JWT token
        return 1L;
    }
}