package com.aiteacher.service.impl;

import com.aiteacher.dto.HealthStatus;
import com.aiteacher.provider.AIProviderRegistry;
import com.aiteacher.provider.llm.LLMProvider;
import com.aiteacher.service.HealthService;
import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class HealthServiceImpl implements HealthService {

    private final DataSource dataSource;
    private final RedisConnectionFactory redisConnectionFactory;
    private final MinioClient minioClient;
    private final AIProviderRegistry aiProviderRegistry;

    @Override
    public HealthStatus checkAll() {
        HealthStatus status = new HealthStatus();
        status.setTimestamp(LocalDateTime.now());

        Map<String, HealthStatus.ComponentHealth> aiProviders = new LinkedHashMap<>();
        boolean allHealthy = true;

        HealthStatus.ComponentHealth dbHealth = checkDatabase();
        HealthStatus.ComponentHealth redisHealth = checkRedis();
        HealthStatus.ComponentHealth minioHealth = checkMinIO();

        for (LLMProvider provider : aiProviderRegistry.getLLMProviders()) {
            String name = provider.getProviderName();
            aiProviders.put(name, checkSingleAIProvider(provider));
        }

        if ("DOWN".equals(dbHealth.getStatus()) ||
            "DOWN".equals(redisHealth.getStatus()) ||
            "DOWN".equals(minioHealth.getStatus())) {
            allHealthy = false;
        }

        for (HealthStatus.ComponentHealth providerHealth : aiProviders.values()) {
            if ("DOWN".equals(providerHealth.getStatus())) {
                allHealthy = false;
                break;
            }
        }

        status.setStatus(allHealthy ? "UP" : "DOWN");
        status.setDatabase(dbHealth);
        status.setRedis(redisHealth);
        status.setMinio(minioHealth);
        status.setAiProviders(aiProviders);

        return status;
    }

    @Override
    public HealthStatus.ComponentHealth checkDatabase() {
        long start = System.currentTimeMillis();
        try (Connection conn = dataSource.getConnection()) {
            boolean valid = conn.isValid(5);
            long responseTime = System.currentTimeMillis() - start;
            if (valid) {
                return HealthStatus.ComponentHealth.builder()
                        .status("UP")
                        .message("Database connection successful")
                        .responseTimeMs(responseTime)
                        .build();
            } else {
                return HealthStatus.ComponentHealth.builder()
                        .status("DOWN")
                        .message("Database connection invalid")
                        .responseTimeMs(responseTime)
                        .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            log.error("Database health check failed", e);
            return HealthStatus.ComponentHealth.builder()
                    .status("DOWN")
                    .message("Database connection failed: " + e.getMessage())
                    .responseTimeMs(responseTime)
                    .build();
        }
    }

    @Override
    public HealthStatus.ComponentHealth checkRedis() {
        long start = System.currentTimeMillis();
        try {
            Object result = redisConnectionFactory.getConnection().ping();
            long responseTime = System.currentTimeMillis() - start;
            if ("PONG".equals(result)) {
                return HealthStatus.ComponentHealth.builder()
                        .status("UP")
                        .message("Redis connection successful")
                        .responseTimeMs(responseTime)
                        .build();
            } else {
                return HealthStatus.ComponentHealth.builder()
                        .status("DOWN")
                        .message("Redis unexpected response: " + result)
                        .responseTimeMs(responseTime)
                        .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            log.error("Redis health check failed", e);
            return HealthStatus.ComponentHealth.builder()
                    .status("DOWN")
                    .message("Redis connection failed: " + e.getMessage())
                    .responseTimeMs(responseTime)
                    .build();
        }
    }

    @Override
    public HealthStatus.ComponentHealth checkMinIO() {
        long start = System.currentTimeMillis();
        try {
            String bucketName = "ai-teacher";
            boolean exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucketName).build()
            );
            long responseTime = System.currentTimeMillis() - start;
            if (exists) {
                return HealthStatus.ComponentHealth.builder()
                        .status("UP")
                        .message("MinIO bucket '" + bucketName + "' exists")
                        .responseTimeMs(responseTime)
                        .build();
            } else {
                return HealthStatus.ComponentHealth.builder()
                        .status("DOWN")
                        .message("MinIO bucket '" + bucketName + "' not found")
                        .responseTimeMs(responseTime)
                        .build();
            }
        } catch (Exception e) {
            long responseTime = System.currentTimeMillis() - start;
            log.error("MinIO health check failed", e);
            return HealthStatus.ComponentHealth.builder()
                    .status("DOWN")
                    .message("MinIO connection failed: " + e.getMessage())
                    .responseTimeMs(responseTime)
                    .build();
        }
    }

    @Override
    public Map<String, HealthStatus.ComponentHealth> checkAIProviders() {
        Map<String, HealthStatus.ComponentHealth> results = new LinkedHashMap<>();
        List<LLMProvider> providers = aiProviderRegistry.getLLMProviders();
        if (providers.isEmpty()) {
            results.put("no_provider", HealthStatus.ComponentHealth.builder()
                    .status("DOWN")
                    .message("No AI provider registered")
                    .responseTimeMs(0L)
                    .build());
            return results;
        }
        for (LLMProvider provider : providers) {
            String name = provider.getProviderName();
            results.put(name, checkSingleAIProvider(provider));
        }
        return results;
    }

    private HealthStatus.ComponentHealth checkSingleAIProvider(LLMProvider provider) {
        long start = System.currentTimeMillis();
        try {
            return HealthStatus.ComponentHealth.builder()
                    .status("UP")
                    .message(provider.getProviderType().getDisplayName() + " is enabled")
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .build();
        } catch (Exception e) {
            log.error("AI Provider health check failed for {}", provider.getProviderName(), e);
            return HealthStatus.ComponentHealth.builder()
                    .status("DOWN")
                    .message("AI provider check failed: " + e.getMessage())
                    .responseTimeMs(System.currentTimeMillis() - start)
                    .build();
        }
    }
}
