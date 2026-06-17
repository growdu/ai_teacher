package com.aiteacher.service;

import com.aiteacher.entity.Course;
import com.aiteacher.mapper.CourseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseService {

    @Autowired
    private CourseMapper courseMapper;

    public List<Course> list(LambdaQueryWrapper<Course> wrapper) {
        // Use custom query to bypass TenantLineInnerInterceptor issue
        return courseMapper.selectAllForList();
    }

    public Page<Course> page(Page<Course> page, LambdaQueryWrapper<Course> wrapper) {
        // Use custom query - MyBatis pagination wrapper is bypassed
        List<Course> records = courseMapper.selectAllForList();
        page.setRecords(records);
        return page;
    }

    public Course getById(Long id) {
        return courseMapper.selectByIdCustom(id);
    }

    public boolean updateById(Course course) {
        return courseMapper.updateById(course) > 0;
    }

    public boolean deleteById(Long id) {
        Course course = new Course();
        course.setId(id);
        course.setDeleted(true);
        return courseMapper.updateById(course) > 0;
    }
}