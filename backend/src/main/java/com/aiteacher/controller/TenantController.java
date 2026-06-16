package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.entity.Tenant;
import com.aiteacher.service.TenantService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    @Autowired
    private TenantService tenantService;

    @PostMapping
    public R<Tenant> create(@RequestBody Tenant tenant) {
        return R.ok(tenantService.create(tenant));
    }

    @GetMapping("/list")
    public R<List<Tenant>> list() {
        return R.ok(tenantService.list(
                new LambdaQueryWrapper<Tenant>()
                        .eq(Tenant::getDeleted, false)
                        .orderByDesc(Tenant::getCreatedAt)
        ));
    }

    @GetMapping("/page")
    public R<Page<Tenant>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Page<Tenant> page = new Page<>(pageNum, pageSize);
        return R.ok(tenantService.page(page,
                new LambdaQueryWrapper<Tenant>()
                        .eq(Tenant::getDeleted, false)
                        .orderByDesc(Tenant::getCreatedAt)
        ));
    }

    @GetMapping("/{id}")
    public R<Tenant> getById(@PathVariable Long id) {
        return R.ok(tenantService.getById(id));
    }

    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Tenant tenant) {
        tenant.setId(id);
        return R.ok(tenantService.update(tenant));
    }
}