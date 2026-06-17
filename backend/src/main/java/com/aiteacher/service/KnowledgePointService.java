package com.aiteacher.service;

import com.aiteacher.entity.KnowledgePoint;
import com.aiteacher.mapper.KnowledgePointMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgePointService {

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    public List<KnowledgePoint> list(LambdaQueryWrapper<KnowledgePoint> wrapper) {
        return knowledgePointMapper.selectList(wrapper);
    }

    public Page<KnowledgePoint> page(Page<KnowledgePoint> page, LambdaQueryWrapper<KnowledgePoint> wrapper) {
        // Bypass PaginationInnerInterceptor - use manual LIMIT/OFFSET via full load + slice
        List<KnowledgePoint> records = knowledgePointMapper.selectList(wrapper);
        int total = records.size();
        int fromIndex = (int) ((page.getCurrent() - 1) * page.getSize());
        int toIndex = (int) Math.min(fromIndex + page.getSize(), total);
        page.setTotal(total);
        page.setRecords(fromIndex < total ? records.subList(fromIndex, toIndex) : List.of());
        return page;
    }

    public KnowledgePoint getById(Long id) {
        return knowledgePointMapper.selectById(id);
    }

    public boolean save(KnowledgePoint knowledgePoint) {
        return knowledgePointMapper.insert(knowledgePoint) > 0;
    }

    public boolean updateById(KnowledgePoint knowledgePoint) {
        return knowledgePointMapper.updateById(knowledgePoint) > 0;
    }
}
