package com.aiteacher.service;

import com.aiteacher.entity.Workspace;
import com.aiteacher.mapper.WorkspaceMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class WorkspaceService {

    @Autowired
    private WorkspaceMapper workspaceMapper;

    public Workspace create(Workspace workspace) {
        workspace.setCreatedAt(LocalDateTime.now());
        workspace.setDeleted(false);
        workspaceMapper.insert(workspace);
        return workspace;
    }

    public Workspace getById(Long id) {
        return workspaceMapper.selectById(id);
    }

    public List<Workspace> list(LambdaQueryWrapper<Workspace> wrapper) {
        return workspaceMapper.selectList(wrapper);
    }

    public Page<Workspace> page(Page<Workspace> page, LambdaQueryWrapper<Workspace> wrapper) {
        // Bypass PaginationInnerInterceptor - use manual LIMIT/OFFSET via full load + slice
        List<Workspace> records = workspaceMapper.selectList(wrapper);
        int total = records.size();
        int fromIndex = (int) ((page.getCurrent() - 1) * page.getSize());
        int toIndex = (int) Math.min(fromIndex + page.getSize(), total);
        page.setTotal(total);
        page.setRecords(fromIndex < total ? records.subList(fromIndex, toIndex) : List.of());
        return page;
    }

    public boolean update(Workspace workspace) {
        workspace.setUpdatedAt(LocalDateTime.now());
        return workspaceMapper.updateById(workspace) > 0;
    }

    public boolean delete(Long id) {
        Workspace workspace = new Workspace();
        workspace.setId(id);
        workspace.setDeleted(true);
        return workspaceMapper.updateById(workspace) > 0;
    }
}