package com.aiteacher.service;

import com.aiteacher.entity.Tenant;
import com.aiteacher.mapper.TenantMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class TenantService {

    @Autowired
    private TenantMapper tenantMapper;

    public Tenant create(Tenant tenant) {
        tenant.setCreatedAt(LocalDateTime.now());
        tenant.setDeleted(false);
        tenantMapper.insert(tenant);
        return tenant;
    }

    public Tenant getById(Long id) {
        return tenantMapper.selectById(id);
    }

    public Page<Tenant> page(Page<Tenant> page, LambdaQueryWrapper<Tenant> wrapper) {
        // Bypass PaginationInnerInterceptor - use manual LIMIT/OFFSET via full load + slice
        List<Tenant> records = tenantMapper.selectList(wrapper);
        int total = records.size();
        int fromIndex = (int) ((page.getCurrent() - 1) * page.getSize());
        int toIndex = (int) Math.min(fromIndex + page.getSize(), total);
        page.setTotal(total);
        page.setRecords(fromIndex < total ? records.subList(fromIndex, toIndex) : List.of());
        return page;
    }

    public List<Tenant> list(LambdaQueryWrapper<Tenant> wrapper) {
        return tenantMapper.selectList(wrapper);
    }

    public boolean update(Tenant tenant) {
        tenant.setUpdatedAt(LocalDateTime.now());
        return tenantMapper.updateById(tenant) > 0;
    }
}