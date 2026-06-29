package com.aiteacher.service;

import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.mapper.TeachingMaterialMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeachingMaterialService {

    @Autowired
    private TeachingMaterialMapper teachingMaterialMapper;

    public Page<TeachingMaterial> page(Page<TeachingMaterial> page, LambdaQueryWrapper<TeachingMaterial> wrapper) {
        // Bypass PaginationInnerInterceptor - use manual LIMIT/OFFSET via full load + slice
        List<TeachingMaterial> records = teachingMaterialMapper.selectList(wrapper);
        int total = records.size();
        int fromIndex = (int) ((page.getCurrent() - 1) * page.getSize());
        int toIndex = (int) Math.min(fromIndex + page.getSize(), total);
        page.setTotal(total);
        page.setRecords(fromIndex < total ? records.subList(fromIndex, toIndex) : List.of());
        return page;
    }

    public TeachingMaterial getById(Long id) {
        return teachingMaterialMapper.selectById(id);
    }

    public java.util.List<TeachingMaterial> list(LambdaQueryWrapper<TeachingMaterial> wrapper) {
        return teachingMaterialMapper.selectList(wrapper);
    }

    public boolean updateById(TeachingMaterial material) {
        return teachingMaterialMapper.updateById(material) > 0;
    }

    public boolean deleteById(Long id) {
        return teachingMaterialMapper.deleteById(id) > 0;
    }
}