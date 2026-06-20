package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
import com.aiteacher.entity.Workspace;
import com.aiteacher.service.WorkspaceService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/workspace")
public class WorkspaceController {

    @Autowired
    private WorkspaceService workspaceService;

    @PostMapping
    public R<Workspace> create(@RequestBody Workspace workspace) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }
        workspace.setTenantId(tenantId);
        return R.ok(workspaceService.create(workspace));
    }

    @GetMapping("/list")
    public R<List<Workspace>> list() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }
        return R.ok(workspaceService.list(
                new LambdaQueryWrapper<Workspace>()
                        .eq(Workspace::getTenantId, tenantId)
                        .eq(Workspace::getDeleted, false)
                        .orderByDesc(Workspace::getCreatedAt)
        ));
    }

    @GetMapping("/page")
    public R<Page<Workspace>> page(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }
        Page<Workspace> page = new Page<>(pageNum, pageSize);
        return R.ok(workspaceService.page(page,
                new LambdaQueryWrapper<Workspace>()
                        .eq(Workspace::getTenantId, tenantId)
                        .eq(Workspace::getDeleted, false)
                        .orderByDesc(Workspace::getCreatedAt)
        ));
    }

    @GetMapping("/{id}")
    public R<Workspace> getById(@PathVariable Long id) {
        return R.ok(workspaceService.getById(id));
    }

    @PutMapping("/{id}")
    public R<Boolean> update(@PathVariable Long id, @RequestBody Workspace workspace) {
        workspace.setId(id);
        return R.ok(workspaceService.update(workspace));
    }

    @DeleteMapping("/{id}")
    public R<Boolean> delete(@PathVariable Long id) {
        return R.ok(workspaceService.delete(id));
    }
}
