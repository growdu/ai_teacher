package com.aiteacher.service;

import com.aiteacher.dto.HealthStatus;

import java.util.Map;

public interface HealthService {
    HealthStatus checkAll();
    HealthStatus.ComponentHealth checkDatabase();
    HealthStatus.ComponentHealth checkRedis();
    HealthStatus.ComponentHealth checkMinIO();
    Map<String, HealthStatus.ComponentHealth> checkAIProviders();
}