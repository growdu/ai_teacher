package com.aiteacher.service;

import com.aiteacher.entity.AsyncTask;
import com.aiteacher.mapper.AsyncTaskMapper;
import com.aiteacher.websocket.TaskWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Async Task Service - manages long-running tasks
 */
@Slf4j
@Service
public class AsyncTaskService {

    @Autowired
    private AsyncTaskMapper asyncTaskMapper;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired(required = false)
    @Lazy
    private TaskWebSocketHandler taskWebSocketHandler;

    private static final String TASK_KEY_PREFIX = "async:task:";

    public AsyncTask createTask(String taskType, String taskName, Long userId, Long tenantId) {
        AsyncTask task = new AsyncTask();
        task.setTaskType(taskType);
        task.setTaskName(taskName);
        task.setUserId(userId);
        task.setTenantId(tenantId);
        task.setStatus("pending");
        task.setProgress(0);
        task.setCurrentStep("初始化");
        task.setCreatedAt(LocalDateTime.now());
        
        asyncTaskMapper.insert(task);
        cacheTask(task);
        notifyWebSocket(task);
        
        return task;
    }

    public void updateProgress(Long taskId, Integer progress, String currentStep) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task != null) {
            task.setProgress(progress);
            task.setCurrentStep(currentStep);
            task.setUpdatedAt(LocalDateTime.now());
            
            if (progress >= 100) {
                task.setStatus("completed");
                task.setCompletedAt(LocalDateTime.now());
            }
            
            asyncTaskMapper.updateById(task);
            cacheTask(task);
            notifyWebSocket(task);
        }
    }

    public void completeTask(Long taskId, String result) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus("completed");
            task.setProgress(100);
            task.setResult(result);
            task.setCompletedAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            
            asyncTaskMapper.updateById(task);
            cacheTask(task);
            notifyWebSocket(task);
        }
    }

    public void failTask(Long taskId, String errorMessage) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus("failed");
            task.setErrorMessage(errorMessage);
            task.setUpdatedAt(LocalDateTime.now());

            asyncTaskMapper.updateById(task);
            cacheTask(task);
            notifyWebSocket(task);
        }
    }

    public void cancelTask(Long taskId) {
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task != null) {
            task.setStatus("cancelled");
            task.setUpdatedAt(LocalDateTime.now());
            asyncTaskMapper.updateById(task);
            cacheTask(task);
            notifyWebSocket(task);
        }
    }

    public AsyncTask getTask(Long taskId) {
        String cacheKey = TASK_KEY_PREFIX + taskId;
        AsyncTask cachedTask = (AsyncTask) redisTemplate.opsForValue().get(cacheKey);
        if (cachedTask != null) {
            return cachedTask;
        }
        
        AsyncTask task = asyncTaskMapper.selectById(taskId);
        if (task != null) {
            cacheTask(task);
        }
        return task;
    }

    private void cacheTask(AsyncTask task) {
        try {
            String cacheKey = TASK_KEY_PREFIX + task.getId();
            redisTemplate.opsForValue().set(cacheKey, task, java.time.Duration.ofMinutes(30));
        } catch (Exception e) {
            log.warn("Failed to cache task: {}", e.getMessage());
        }
    }

    private void notifyWebSocket(AsyncTask task) {
        if (taskWebSocketHandler != null) {
            taskWebSocketHandler.sendTaskUpdate(
                    task.getId().toString(),
                    task.getStatus(),
                    task.getProgress(),
                    task.getCurrentStep()
            );
        }
    }

    public Map<String, Object> getTaskStatus(Long taskId) {
        AsyncTask task = getTask(taskId);
        if (task == null) {
            return Map.of("error", "Task not found");
        }

        Map<String, Object> status = new HashMap<>();
        status.put("taskId", task.getId());
        status.put("status", task.getStatus());
        status.put("progress", task.getProgress());
        status.put("currentStep", task.getCurrentStep());
        status.put("result", task.getResult());
        status.put("errorMessage", task.getErrorMessage());

        return status;
    }

    public java.util.List<AsyncTask> listByTenant(Long tenantId) {
        return asyncTaskMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<AsyncTask>()
                        .eq(AsyncTask::getTenantId, tenantId)
                        .orderByDesc(AsyncTask::getCreatedAt)
                        .last("LIMIT 100")
        );
    }
}