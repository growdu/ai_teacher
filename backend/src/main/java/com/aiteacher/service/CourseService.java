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
        return courseMapper.selectList(wrapper);
    }

    public Page<Course> page(Page<Course> page, LambdaQueryWrapper<Course> wrapper) {
        return courseMapper.selectPage(page, wrapper);
    }

    public Course getById(Long id) {
        return courseMapper.selectById(id);
    }

    public boolean updateById(Course course) {
        return courseMapper.updateById(course) > 0;
    }

    public boolean deleteById(Long id) {
        return courseMapper.deleteById(id) > 0;
    }
}