package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.entity.Resource;
import com.aiteacher.service.ResourceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/resource")
public class ResourceController {

    @Autowired
    private ResourceService resourceService;

    @PostMapping("/upload")
    public R<Resource> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) Long workspaceId,
            Authentication authentication) {
        Long tenantId = getTenantId(authentication);
        return R.ok(resourceService.upload(file, workspaceId, tenantId));
    }

    @GetMapping("/list")
    public R<List<Resource>> list(
            @RequestParam(required = false) Long workspaceId,
            @RequestParam(required = false) String resourceType,
            Authentication authentication) {
        Long tenantId = getTenantId(authentication);
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resource::getTenantId, tenantId);
        if (workspaceId != null) wrapper.eq(Resource::getWorkspaceId, workspaceId);
        if (resourceType != null) wrapper.eq(Resource::getResourceType, resourceType);
        wrapper.orderByDesc(Resource::getCreatedAt);
        return R.ok(resourceService.list(wrapper));
    }

    @GetMapping("/page")
    public R<Page<Resource>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String resourceType,
            Authentication authentication) {
        Long tenantId = getTenantId(authentication);
        Page<Resource> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<Resource> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Resource::getTenantId, tenantId);
        if (resourceType != null) wrapper.eq(Resource::getResourceType, resourceType);
        wrapper.orderByDesc(Resource::getCreatedAt);
        return R.ok(resourceService.page(page, wrapper));
    }

    @GetMapping("/{id}")
    public R<Resource> getById(@PathVariable Long id) {
        return R.ok(resourceService.getById(id));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(resourceService.delete(id));
    }

    private Long getTenantId(Authentication authentication) {
        return 1L;
    }
}