package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.entity.KnowledgePoint;
import com.aiteacher.service.KnowledgePointService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/knowledge-point")
public class KnowledgePointController {

    @Autowired
    private KnowledgePointService knowledgePointService;

    @GetMapping("/list")
    public R<List<KnowledgePoint>> list(
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String subject,
            @RequestParam(required = false) String grade) {
        LambdaQueryWrapper<KnowledgePoint> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) wrapper.eq(KnowledgePoint::getTenantId, tenantId);
        if (subject != null) wrapper.eq(KnowledgePoint::getSubject, subject);
        if (grade != null) wrapper.eq(KnowledgePoint::getGrade, grade);
        wrapper.orderByDesc(KnowledgePoint::getCreatedAt);
        return R.ok(knowledgePointService.list(wrapper));
    }

    @GetMapping("/page")
    public R<Page<KnowledgePoint>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long tenantId) {
        Page<KnowledgePoint> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<KnowledgePoint> wrapper = new LambdaQueryWrapper<>();
        if (tenantId != null) wrapper.eq(KnowledgePoint::getTenantId, tenantId);
        wrapper.orderByDesc(KnowledgePoint::getCreatedAt);
        return R.ok(knowledgePointService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public R<KnowledgePoint> getById(@PathVariable Long id) {
        return R.ok(knowledgePointService.getById(id));
    }

    @PostMapping
    public R<Boolean> create(@RequestBody KnowledgePoint knowledgePoint) {
        return R.ok(knowledgePointService.save(knowledgePoint));
    }

    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody KnowledgePoint knowledgePoint) {
        knowledgePoint.setId(id);
        return R.ok(knowledgePointService.updateById(knowledgePoint));
    }
}