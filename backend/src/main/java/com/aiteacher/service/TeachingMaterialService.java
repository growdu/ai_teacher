package com.aiteacher.service;

import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.mapper.TeachingMaterialMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeachingMaterialService {

    @Autowired
    private TeachingMaterialMapper teachingMaterialMapper;

    public Page<TeachingMaterial> page(Page<TeachingMaterial> page, LambdaQueryWrapper<TeachingMaterial> wrapper) {
        return teachingMaterialMapper.selectPage(page, wrapper);
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
}