package com.aiteacher.provider.video;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * MiniMax Video Generation Provider
 * API: https://api.minimax.chat/v1/video_generation
 * 
 * Flow:
 * 1. Submit generation request → get task_id
 * 2. Poll /task/info?task_id=... until status = "success"
 * 3. Download video from the returned URL
 */
@Slf4j
public class MiniMaxVideoProvider {

    private final String apiKey;
    private final String baseUrl;
    private final String model;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public MiniMaxVideoProvider(String apiKey) {
        this.apiKey = apiKey;
        this.baseUrl = "https://api.minimax.chat/v1";
        this.model = "MiniMax-Video-01";
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Generate video from text prompt
     * @param prompt Text description of the video to generate
     * @param duration Video duration in seconds (5-10 supported)
     * @param outputDir Directory to save the downloaded video
     * @return Path to the downloaded video file
     */
    public String generateVideo(String prompt, int duration, String outputDir) throws Exception {
        // Step 1: Submit generation request
        String taskId = submitGenerationRequest(prompt, duration);

        // Step 2: Poll until done
        String videoUrl = pollForResult(taskId);

        // Step 3: Download video
        return downloadVideo(videoUrl, outputDir);
    }

    private String submitGenerationRequest(String prompt, int duration) throws Exception {
        String url = baseUrl + "/video_generation";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("prompt", prompt);
        requestBody.put("duration", Math.min(duration, 10)); // cap at 10s

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("MiniMax video request failed: " + response.getStatusCode());
        }

        JsonNode root = objectMapper.readTree(response.getBody());
        if (root.has("error")) {
            throw new RuntimeException("MiniMax video API error: " + root.get("error").asText());
        }

        // Response: { "task_id": "xxx", "status": "pending" }
        String taskId = root.has("task_id") ? root.get("task_id").asText()
                : root.path("data").has("task_id") ? root.path("data").get("task_id").asText()
                : null;

        if (taskId == null) {
            throw new RuntimeException("No task_id in MiniMax video response: " + response.getBody());
        }

        log.info("MiniMax video task submitted: {}", taskId);
        return taskId;
    }

    private String pollForResult(String taskId) throws Exception {
        int maxAttempts = 60; // 60 * 5s = 5 minutes max
        int waitSeconds = 5;

        for (int i = 0; i < maxAttempts; i++) {
            TimeUnit.SECONDS.sleep(waitSeconds);

            String statusUrl = baseUrl + "/task/info?task_id=" + taskId;
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Void> entity = new HttpEntity<>(headers);
            ResponseEntity<String> response = restTemplate.getForEntity(statusUrl, String.class, entity);

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Task status check failed (attempt {}/{}): {}", i + 1, maxAttempts, response.getStatusCode());
                continue;
            }

            JsonNode root = objectMapper.readTree(response.getBody());
            String status = root.has("status") ? root.get("status").asText()
                    : root.path("data").path("status").asText("");

            log.debug("Task {} status: {} (attempt {}/{})", taskId, status, i + 1, maxAttempts);

            if ("success".equalsIgnoreCase(status) || "completed".equalsIgnoreCase(status)) {
                // Extract video URL
                JsonNode data = root.path("data");
                JsonNode resultNode = data.path("result");
                if (!resultNode.isMissingNode()) {
                    data = resultNode;
                }
                
                String videoUrl = data.path("video").path("url").asText("");
                if (videoUrl.isEmpty()) {
                    videoUrl = data.path("video_url").asText("");
                }
                if (videoUrl.isEmpty()) {
                    videoUrl = data.path("video").asText("");
                }
                
                if (videoUrl.isEmpty()) {
                    throw new RuntimeException("No video URL in completed task response: " + response.getBody());
                }
                
                log.info("MiniMax video ready: {}", videoUrl);
                return videoUrl;
            }

            if ("failed".equalsIgnoreCase(status) || "error".equalsIgnoreCase(status)) {
                String errMsg = root.path("data").path("error").asText("");
                throw new RuntimeException("MiniMax video generation failed: " + errMsg);
            }
        }

        throw new RuntimeException("MiniMax video generation timed out after " + (maxAttempts * waitSeconds) + " seconds");
    }

    private String downloadVideo(String videoUrl, String outputDir) throws Exception {
        Files.createDirectories(Path.of(outputDir));
        String outputFile = outputDir + "/minimax_video_" + UUID.randomUUID() + ".mp4";

        // Download with RestTemplate
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");

        ResponseEntity<byte[]> response = restTemplate.getForEntity(
                videoUrl, byte[].class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to download video: " + response.getStatusCode());
        }

        try (OutputStream out = new FileOutputStream(outputFile)) {
            out.write(response.getBody());
        }

        log.info("Video downloaded to: {}", outputFile);
        return outputFile;
    }

    public boolean isEnabled() {
        return apiKey != null && !apiKey.isEmpty();
    }
}
