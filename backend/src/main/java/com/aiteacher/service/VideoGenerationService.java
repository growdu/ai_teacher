package com.aiteacher.service;

import com.aiteacher.dto.VideoGenerateRequest;
import com.aiteacher.entity.AsyncTask;
import com.aiteacher.entity.Course;
import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.mapper.CourseMapper;
import com.aiteacher.mapper.TeachingMaterialMapper;
import com.aiteacher.provider.ai.VoiceConfig;
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

        // Create a simple video with audio
        // If PPT is provided, use PPT slides as background
        if (request.getPptId() != null) {
            // TODO: Convert PPT to images and create video
            // For now, create a simple slide show video
            return createSlideShowVideo(audioPath, videoFile);
        } else {
            // Create a simple black background video with audio
            return createSimpleVideo(audioPath, videoFile);
        }
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
            MultipartFile multipartFile = new org.springframework.web.multipart.commons.CommonsMultipartFile(
                    new org.apache.commons.fileupload.disk.DiskFileItem(
                            "file",
                            "video/mp4",
                            false,
                            videoFile.getName(),
                            (int) videoFile.length(),
                            videoFile.getParentFile()
                    )
            );
            
            String objectName = "video/" + UUID.randomUUID().toString() + ".mp4";
            fileStorageService.uploadFile(multipartFile, "video");
            
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