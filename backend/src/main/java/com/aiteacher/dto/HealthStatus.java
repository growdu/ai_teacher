package com.aiteacher.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HealthStatus implements Serializable {
    
    private String status; // "UP", "DOWN"
    private LocalDateTime timestamp;
    private ComponentHealth database;
    private ComponentHealth redis;
    private ComponentHealth minio;
    private Map<String, ComponentHealth> aiProviders;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ComponentHealth implements Serializable {
        private String status; // "UP", "DOWN"
        private String message;
        private Long responseTimeMs;
    }
}
