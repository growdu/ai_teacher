package com.aiteacher.controller;

import com.aiteacher.common.Result;
import com.aiteacher.dto.HealthStatus;
import com.aiteacher.service.HealthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class HealthController {

    private final HealthService healthService;

    /**
     * Simple liveness check - returns OK if the service is running
     */
    @GetMapping("/health")
    public Result<String> health() {
        return Result.success("AI Teacher Studio Backend is running");
    }

    /**
     * Detailed health check covering all components (DB, Redis, MinIO, AI Providers)
     */
    @GetMapping("/health/detailed")
    public Result<HealthStatus> healthDetailed() {
        HealthStatus status = healthService.checkAll();
        if ("UP".equals(status.getStatus())) {
            return Result.success("All components healthy", status);
        } else {
            return Result.success("Some components unhealthy", status);
        }
    }

    /**
     * Individual component health checks
     */
    @GetMapping("/health/database")
    public Result<HealthStatus.ComponentHealth> healthDatabase() {
        return Result.success(healthService.checkDatabase());
    }

    @GetMapping("/health/redis")
    public Result<HealthStatus.ComponentHealth> healthRedis() {
        return Result.success(healthService.checkRedis());
    }

    @GetMapping("/health/minio")
    public Result<HealthStatus.ComponentHealth> healthMinIO() {
        return Result.success(healthService.checkMinIO());
    }

    @GetMapping("/health/ai-providers")
    public Result<Map<String, HealthStatus.ComponentHealth>> healthAIProviders() {
        return Result.success(healthService.checkAIProviders());
    }
}
