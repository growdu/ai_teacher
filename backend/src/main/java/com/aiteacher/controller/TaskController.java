package com.aiteacher.controller;

import com.aiteacher.common.R;
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
}