package com.aiteacher.service;

import com.aiteacher.dto.VideoGenerateRequest;
import com.aiteacher.entity.AsyncTask;
import com.aiteacher.entity.Course;
import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.mapper.CourseMapper;
import com.aiteacher.mapper.TeachingMaterialMapper;
import com.aiteacher.provider.ai.VoiceConfig;
import com.aiteacher.provider.video.MiniMaxVideoProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Video Generation Service - generates video from course
 */
@Slf4j
@Service
public class VideoGenerationService {

    @Autowired
    private AIService aiService;

    @Autowired
    private AsyncTaskService asyncTaskService;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private TeachingMaterialMapper teachingMaterialMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Value("${video.output-dir:/tmp/video}")
    private String outputDir;

    @Value("${video.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Autowired(required = false)
    private MiniMaxVideoProvider miniMaxVideoProvider;

    /**
     * Generate video asynchronously
     */
    public Long generateVideoAsync(VideoGenerateRequest request, Long userId, Long tenantId) {
        // Create async task
        AsyncTask task = asyncTaskService.createTask("video", "视频生成", userId, tenantId);
        
        // Start async generation
        CompletableFuture.runAsync(() -> {
            try {
                generateVideo(task.getId(), request, userId, tenantId);
            } catch (Exception e) {
                log.error("Video generation failed: {}", e.getMessage(), e);
                asyncTaskService.failTask(task.getId(), e.getMessage());
            }
        });
        
        return task.getId();
    }

    /**
     * Generate video synchronously (for small videos)
     */
    public TeachingMaterial generateVideo(VideoGenerateRequest request, Long userId, Long tenantId) {
        AsyncTask task = asyncTaskService.createTask("video", "视频生成", userId, tenantId);
        try {
            return generateVideo(task.getId(), request, userId, tenantId);
        } catch (Exception e) {
            asyncTaskService.failTask(task.getId(), e.getMessage());
            throw new RuntimeException("视频生成失败: " + e.getMessage(), e);
        }
    }

    private TeachingMaterial generateVideo(Long taskId, VideoGenerateRequest request, Long userId, Long tenantId) throws Exception {
        try {
            // Step 1: Get course data
            asyncTaskService.updateProgress(taskId, 10, "获取课程数据");
            Course course = courseMapper.selectById(request.getCourseId());
            if (course == null) {
                throw new RuntimeException("Course not found");
            }

            String script = request.getScript() != null ? request.getScript() : course.getScript();
            if (script == null || script.isEmpty()) {
                throw new RuntimeException("No script available");
            }

            // Step 2: Generate audio (TTS)
            asyncTaskService.updateProgress(taskId, 20, "生成语音");
            String audioPath = generateAudio(taskId, script);
            if (audioPath == null) {
                throw new RuntimeException("Audio generation failed");
            }

            // Step 3: Prepare video background
            asyncTaskService.updateProgress(taskId, 50, "准备视频画面");
            String videoPath = generateVideoWithBackground(taskId, request, audioPath);

            // Step 4: Finalize and upload
            asyncTaskService.updateProgress(taskId, 80, "上传视频");
            String fileUrl = uploadVideo(videoPath);

            // Step 5: Save teaching material
            asyncTaskService.updateProgress(taskId, 95, "保存记录");
            TeachingMaterial material = saveVideoMaterial(course, fileUrl, userId, tenantId);

            // Cleanup
            Files.deleteIfExists(Path.of(audioPath));
            Files.deleteIfExists(Path.of(videoPath));

            asyncTaskService.completeTask(taskId, material.getId().toString());
            return material;

        } catch (Exception e) {
            asyncTaskService.failTask(taskId, e.getMessage());
            throw e;
        }
    }

    private String generateAudio(Long taskId, String script) throws Exception {
        try {
            // Clean script for TTS (remove markdown formatting)
            String cleanScript = script.replaceAll("#+\\s*", "").replaceAll("\\*+", "").trim();
            
            // Truncate if too long (TTS has limits)
            if (cleanScript.length() > 5000) {
                cleanScript = cleanScript.substring(0, 5000);
            }

            // Generate audio using TTS
            InputStream audioStream = aiService.synthesizeSpeech(cleanScript);
            
            // Save to temp file
            Path outputPath = Paths.get(outputDir);
            Files.createDirectories(outputPath);
            String audioFile = outputDir + "/audio_" + System.currentTimeMillis() + ".mp3";
            
            try (OutputStream out = new FileOutputStream(audioFile)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = audioStream.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            
            return audioFile;
        } catch (Exception e) {
            log.error("Audio generation failed: {}", e.getMessage());
            throw new RuntimeException("语音生成失败: " + e.getMessage());
        }
    }

    private String generateVideoWithBackground(Long taskId, VideoGenerateRequest request, String audioPath) throws Exception {
        Path outputPath = Paths.get(outputDir);
        Files.createDirectories(outputPath);
        String videoFile = outputPath + "/video_" + System.currentTimeMillis() + ".mp4";

        // Use MiniMax Video API if available
        if (miniMaxVideoProvider != null && miniMaxVideoProvider.isEnabled()) {
            try {
                asyncTaskService.updateProgress(taskId, 40, "调用MiniMax视频生成");
                // Build prompt from course script
                String prompt = buildVideoPrompt(request);
                String downloadedPath = miniMaxVideoProvider.generateVideo(prompt, 6, outputDir);
                // Replace audio with our TTS audio
                return replaceAudio(downloadedPath, audioPath, videoFile);
            } catch (Exception e) {
                log.warn("MiniMax video generation failed, falling back to FFmpeg: {}", e.getMessage());
            }
        }

        // Fallback: create simple video with FFmpeg
        if (request.getPptId() != null) {
            return createSlideShowVideo(audioPath, videoFile);
        } else {
            return createSimpleVideo(audioPath, videoFile);
        }
    }

    private String buildVideoPrompt(VideoGenerateRequest request) {
        // Build a descriptive prompt for MiniMax video generation
        Course course = null;
        try {
            course = courseMapper.selectById(request.getCourseId());
        } catch (Exception ignored) {}
        
        String title = course != null ? course.getTitle() : "";
        String script = request.getScript() != null ? request.getScript() 
                : course != null ? course.getScript() : "";
        
        // Truncate and clean script for prompt
        if (script.length() > 500) {
            script = script.substring(0, 500) + "...";
        }
        script = script.replaceAll("#+\\s*", "").replaceAll("\\*+", "").replaceAll("\n+", " ");
        
        return String.format("教育视频：%s。内容：%s", title, script);
    }

    private String replaceAudio(String videoPath, String audioPath, String outputFile) throws Exception {
        // Use FFmpeg to replace the video's audio with our TTS audio
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-i", videoPath,       // input video
                "-i", audioPath,       // new audio
                "-c:v", "copy",        // keep original video stream
                "-c:a", "aac",         // re-encode audio
                "-shortest",
                "-y",
                outputFile
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg replace audio: {}", line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg replace audio failed: " + exitCode);
        }
        // Cleanup source video
        Files.deleteIfExists(Path.of(videoPath));
        return outputFile;
    }

    private String createSimpleVideo(String audioPath, String videoOutput) throws Exception {
        // Use FFmpeg to create a video with a static image and audio
        // First create a placeholder image
        String imageFile = outputDir + "/placeholder_" + System.currentTimeMillis() + ".png";
        
        // Create a simple 1280x720 PNG image using ImageMagick or similar
        // For now, we'll just create the video directly with a solid color
        
        ProcessBuilder pb = new ProcessBuilder(
                ffmpegPath,
                "-f", "lavfi",
                "-i", "color=c=blue:s=1280x720:d=" + getAudioDuration(audioPath),
                "-i", audioPath,
                "-c:v", "libx264",
                "-c:a", "aac",
                "-shortest",
                "-y",
                videoOutput
        );
        
        pb.redirectErrorStream(true);
        Process process = pb.start();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                log.debug("FFmpeg: {}", line);
            }
        }
        
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("FFmpeg failed with exit code: " + exitCode);
        }
        
        // Cleanup
        Files.deleteIfExists(Path.of(imageFile));
        
        return videoOutput;
    }

    private String createSlideShowVideo(String audioPath, String videoOutput) throws Exception {
        // TODO: Implement slide show video generation
        // For now, fall back to simple video
        return createSimpleVideo(audioPath, videoOutput);
    }

    private int getAudioDuration(String audioFile) {
        // Estimate duration based on file size (rough estimate)
        // In production, use ffprobe to get exact duration
        File file = new File(audioFile);
        long sizeInBytes = file.length();
        // Assume ~128kbps for MP3
        return (int) (sizeInBytes * 8 / 128000);
    }

    private String uploadVideo(String videoPath) {
        try {
            File videoFile = new File(videoPath);
            String objectName = "video/" + UUID.randomUUID().toString() + ".mp4";
            fileStorageService.uploadFile(videoFile, "video", videoFile.getName());
            
            return fileStorageService.getFileUrl(objectName);
        } catch (Exception e) {
            log.error("Video upload failed: {}", e.getMessage());
            throw new RuntimeException("视频上传失败: " + e.getMessage());
        }
    }

    private TeachingMaterial saveVideoMaterial(Course course, String fileUrl, Long userId, Long tenantId) {
        TeachingMaterial material = new TeachingMaterial();
        material.setCourseId(course.getId());
        material.setTenantId(tenantId);
        material.setMaterialType("video");
        material.setTitle(course.getTitle() + " - 教学视频");
        material.setFileUrl(fileUrl);
        material.setStatus("generated");
        material.setCreatedAt(java.time.LocalDateTime.now());
        
        teachingMaterialMapper.insert(material);
        return material;
    }
}