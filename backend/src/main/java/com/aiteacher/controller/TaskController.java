package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
import com.aiteacher.entity.AsyncTask;
import com.aiteacher.service.AsyncTaskService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/task")
public class TaskController {

    @Autowired
    private AsyncTaskService asyncTaskService;

    @GetMapping("/{taskId}")
    public R<Map<String, Object>> getTaskStatus(@PathVariable Long taskId) {
        return R.ok(asyncTaskService.getTaskStatus(taskId));
    }

    @GetMapping("/list")
    public R<java.util.List<AsyncTask>> list() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }
        return R.ok(asyncTaskService.listByTenant(tenantId));
    }

    @DeleteMapping("/{taskId}")
    public R<Boolean> cancel(@PathVariable Long taskId) {
        asyncTaskService.cancelTask(taskId);
        return R.ok(true);
    }
}
