package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
import com.aiteacher.dto.CourseGenerateRequest;
import com.aiteacher.dto.CourseGenerateResponse;
import com.aiteacher.entity.Course;
import com.aiteacher.service.CourseGenerateService;
import com.aiteacher.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course")
public class CourseController {

    @Autowired
    private CourseGenerateService courseGenerateService;

    @Autowired
    private CourseMapper courseMapper;

    /**
     * Generate a new course from knowledge point
     */
    @PostMapping("/generate")
    public R<CourseGenerateResponse> generateCourse(
            @Valid @RequestBody CourseGenerateRequest request,
            Authentication authentication) {
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }
        CourseGenerateResponse response = courseGenerateService.generateCourse(request, userId);
        return R.ok(response);
    }

    /**
     * List all courses
     */
    @GetMapping("/list")
    public R<List<Course>> list(Authentication authentication) {
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }
        List<Course> courses = courseGenerateService.list(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getCreatorId, userId)
                        .orderByDesc(Course::getCreatedAt)
        );
        return R.ok(courses);
    }

    /**
     * Get course by ID
     */
    @GetMapping("/{id}")
    public R<Course> getById(@PathVariable Long id) {
        return R.ok(courseGenerateService.getById(id));
    }

    /**
     * Page query courses
     */
    @GetMapping("/page")
    public R<Page<Course>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            Authentication authentication) {
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }
        Page<Course> page = new Page<>(pageNum, pageSize);
        Page<Course> result = courseGenerateService.page(page,
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getCreatorId, userId)
                        .orderByDesc(Course::getCreatedAt)
        );
        return R.ok(result);
    }

    /**
     * Update course
     */
    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Course course) {
        course.setId(id);
        return R.ok(courseGenerateService.updateById(course));
    }

    /**
     * Delete course
     */
    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        Course course = new Course();
        course.setId(id);
        course.setDeleted(true);
        return R.ok(courseGenerateService.updateById(course));
    }
}
