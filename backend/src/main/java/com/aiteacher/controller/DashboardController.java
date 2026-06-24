package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
import com.aiteacher.entity.AsyncTask;
import com.aiteacher.entity.Course;
import com.aiteacher.entity.KnowledgePoint;
import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.mapper.AsyncTaskMapper;
import com.aiteacher.mapper.CourseMapper;
import com.aiteacher.mapper.KnowledgePointMapper;
import com.aiteacher.mapper.TeachingMaterialMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    @Autowired
    private TeachingMaterialMapper teachingMaterialMapper;

    @Autowired
    private AsyncTaskMapper asyncTaskMapper;

    /**
     * Get dashboard statistics for the current tenant/user
     */
    @GetMapping("/stats")
    public R<Map<String, Object>> getStats() {
        Long tenantId = TenantContext.getTenantId();
        Long userId = TenantContext.getUserId();

        if (tenantId == null || userId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }

        Map<String, Object> stats = new HashMap<>();

        // Course count & generation rate (scoped by user = creator)
        List<Course> allCourses = courseMapper.selectAllForList();
        long totalCourses = allCourses.size();
        long generatedCourses = allCourses.stream()
                .filter(c -> "generated".equals(c.getStatus()))
                .count();

        stats.put("courseCount", totalCourses);
        stats.put("generatedCourseCount", generatedCourses);
        stats.put("generationRate", totalCourses > 0
                ? Math.round((generatedCourses * 100.0) / totalCourses) : 0);

        // Knowledge point count (scoped by tenant)
        long knowledgeCount = knowledgePointMapper.selectCount(
                new LambdaQueryWrapper<KnowledgePoint>()
                        .eq(KnowledgePoint::getTenantId, tenantId)
                        .eq(KnowledgePoint::getDeleted, false)
        );
        stats.put("knowledgeCount", knowledgeCount);

        // Teaching material count (scoped by tenant)
        long materialCount = teachingMaterialMapper.selectCount(
                new LambdaQueryWrapper<TeachingMaterial>()
                        .eq(TeachingMaterial::getTenantId, tenantId)
        );
        stats.put("materialCount", materialCount);

        // PPT and video counts
        long pptCount = teachingMaterialMapper.selectCount(
                new LambdaQueryWrapper<TeachingMaterial>()
                        .eq(TeachingMaterial::getTenantId, tenantId)
                        .eq(TeachingMaterial::getMaterialType, "ppt")
        );
        long videoCount = teachingMaterialMapper.selectCount(
                new LambdaQueryWrapper<TeachingMaterial>()
                        .eq(TeachingMaterial::getTenantId, tenantId)
                        .eq(TeachingMaterial::getMaterialType, "video")
        );
        stats.put("pptCount", pptCount);
        stats.put("videoCount", videoCount);

        // Active task count (running + pending, scoped by tenant)
        long activeTaskCount = asyncTaskMapper.selectCount(
                new LambdaQueryWrapper<AsyncTask>()
                        .eq(AsyncTask::getTenantId, tenantId)
                        .in(AsyncTask::getStatus, "pending", "running")
        );
        stats.put("activeTaskCount", activeTaskCount);

        return R.ok(stats);
    }
}
