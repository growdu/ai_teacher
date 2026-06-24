package com.aiteacher.service;

import com.aiteacher.dto.VideoGenerateRequest;
import com.aiteacher.entity.AsyncTask;
import com.aiteacher.entity.Course;
import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.mapper.CourseMapper;
import com.aiteacher.mapper.TeachingMaterialMapper;
import com.aiteacher.provider.ai.VoiceConfig;
import com.aiteacher.provider.video.MiniMaxVideoProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
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

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${video.output-dir:/tmp/video}")
    private String outputDir;

    @Value("${video.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    @Autowired(required = false)
    private MiniMaxVideoProvider miniMaxVideoProvider;

    /**
     * Get task status - delegates to AsyncTaskService
     */
    public Map<String, Object> getTaskStatus(Long taskId) {
        return asyncTaskService.getTaskStatus(taskId);
    }

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
            return createSlideShowVideo(taskId, request.getPptId(), audioPath, videoFile);
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

    /**
     * Create a slideshow video from PPT slide texts + audio.
     * Steps:
     *  1. Fetch the TeachingMaterial (PPTX) by pptId
     *  2. Download PPTX to temp file
     *  3. Extract slide texts via Node.js script
     *  4. Render each slide as an image using FFmpeg drawtext
     *  5. Concatenate all slide images + audio into final MP4
     */
    private String createSlideShowVideo(Long taskId, Long pptId, String audioPath, String videoOutput) throws Exception {
        log.info("Creating slideshow video for pptId={}", pptId);

        // 1. Fetch the PPT teaching material
        TeachingMaterial pptMaterial = teachingMaterialMapper.selectById(pptId);
        if (pptMaterial == null || pptMaterial.getFileUrl() == null) {
            log.warn("PPTX material not found for id={}, falling back to simple video", pptId);
            return createSimpleVideo(audioPath, videoOutput);
        }

        // 2. Download PPTX to temp file
        Files.createDirectories(Path.of(outputDir));
        String pptxPath = outputDir + "/pptx_" + System.currentTimeMillis() + ".pptx";
        try {
            downloadFile(pptMaterial.getFileUrl(), pptxPath);
        } catch (Exception e) {
            log.warn("Failed to download PPTX {}, falling back: {}", pptMaterial.getFileUrl(), e.getMessage());
            return createSimpleVideo(audioPath, videoOutput);
        }

        // 3. Extract slide texts via Node.js script
        String extractScript = System.getenv("PPT_NODE_SCRIPT") != null
            ? System.getenv("PPT_NODE_SCRIPT").replace("generate-ppt.js", "extract-pptx-text.js")
            : "/app/scripts/extract-pptx-text.js";

        String slideTextsJson = extractSlideTexts(extractScript, pptxPath);

        // 4. Get audio duration
        int audioDuration = getAudioDuration(audioPath);
        if (audioDuration <= 0) audioDuration = 10;

        // 5. Build FFmpeg commands: render each slide + concat
        String result = buildSlideshowVideo(taskId, slideTextsJson, audioPath, videoOutput, audioDuration);

        // 6. Cleanup
        Files.deleteIfExists(Path.of(pptxPath));

        return result;
    }

    private String extractSlideTexts(String scriptPath, String pptxPath) throws Exception {
        // Verify script exists
        if (!Files.exists(Path.of(scriptPath))) {
            log.warn("extract-pptx-text.js not found at {}, using course script", scriptPath);
            return "[]";
        }

        ProcessBuilder pb = new ProcessBuilder(
            "node", scriptPath, pptxPath
        );
        pb.environment().put("NODE_PATH", "/usr/local/lib/node_modules");
        pb.redirectErrorStream(true);

        Process process = pb.start();
        StringBuilder output = new StringBuilder();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("extract-pptx-text.js exited with {}, output: {}", exitCode, output);
            return "[]";
        }

        String result = output.toString();
        if (result.isEmpty()) {
            return "[]";
        }
        // Validate JSON
        try {
            objectMapper.readTree(result);
        } catch (Exception e) {
            log.warn("Invalid JSON from extract-pptx-text.js: {}", e.getMessage());
            return "[]";
        }
        return result;
    }

    private String buildSlideshowVideo(Long taskId, String slideTextsJson, String audioPath,
                                        String videoOutput, int audioDuration) throws Exception {
        List<SlideText> slides;
        try {
            var node = objectMapper.readTree(slideTextsJson);
            slides = objectMapper.readValue(node.traverse(),
                objectMapper.getTypeFactory().constructCollectionType(List.class, SlideText.class));
        } catch (Exception e) {
            log.warn("Failed to parse slide texts, using empty: {}", e.getMessage());
            slides = List.of();
        }

        if (slides.isEmpty()) {
            // Fall back to simple video if no slide texts
            log.info("No slide texts extracted, falling back to simple video");
            return createSimpleVideo(audioPath, videoOutput);
        }

        // Determine per-slide duration: distribute audio duration evenly
        int numSlides = slides.size();
        int perSlideSec = Math.max(3, audioDuration / numSlides);

        log.info("Rendering {} slides, {}s each", numSlides, perSlideSec);
        asyncTaskService.updateProgress(taskId, 60, "渲染幻灯片 (" + numSlides + "张)");

        // Build concat file listing all slide images
        String concatFile = outputDir + "/concat_" + System.currentTimeMillis() + ".txt";
        StringBuilder concatContent = new StringBuilder();

        for (int i = 0; i < slides.size(); i++) {
            SlideText slide = slides.get(i);
            String slideImgPath = renderSlideImage(slide, i, perSlideSec);
            // FFmpeg concat demuxer expects absolute path with 'file:' prefix
            concatContent.append("file '").append(slideImgPath).append("'\n");
            concatContent.append("duration ").append(perSlideSec).append("\n");
        }
        // Last segment doesn't need duration (FFmpeg behavior)
        concatContent.append("file '").append(renderSlideImage(slides.get(slides.size() - 1), slides.size() - 1, perSlideSec)).append("'\n");

        Files.writeString(Path.of(concatFile), concatContent.toString());

        // Build filter complex for smooth crossfade transitions
        String filterComplex =
            // Pad each slide to same duration
            "[0:v]split=" + slides.size() + "[v0][v1][v2];" +
            buildOverlayChain(slides.size()) +
            // Scale to 1280x720 and add audio
            "[ov]scale=1280:720:force_original_aspect_ratio=decrease,pad=1280:720:(ow-iw)/2:(oh-ih)/2,setsar=1[v];" +
            "[v][0:a]concat=n=1:v=1:a=1[vout][aout]";

        // Actually use simpler approach: just concat slides with audio
        // Build a concat file and use concat muxer
        String tmpVideo = outputDir + "/slides_raw_" + System.currentTimeMillis() + ".mp4";

        try {
            // Step 1: concat all slide images (no audio yet)
            ProcessBuilder concatPb = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-f", "concat",
                "-safe", "0",
                "-i", concatFile,
                "-vsync", "vfr",
                "-pix_fmt", "yuv420p",
                tmpVideo
            );
            concatPb.redirectErrorStream(true);
            Process concatProc = concatPb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(concatProc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.debug("FFmpeg concat: {}", line);
                }
            }
            int exitCode = concatProc.waitFor();
            if (exitCode != 0) {
                log.warn("FFmpeg concat failed (exit {}), falling back to simple video", exitCode);
                return createSimpleVideo(audioPath, videoOutput);
            }

            // Step 2: replace audio with TTS audio
            ProcessBuilder audioPb = new ProcessBuilder(
                ffmpegPath,
                "-y",
                "-i", tmpVideo,
                "-i", audioPath,
                "-c:v", "copy",
                "-c:a", "aac",
                "-shortest",
                videoOutput
            );
            audioPb.redirectErrorStream(true);
            Process audioProc = audioPb.start();
            try (BufferedReader r = new BufferedReader(new InputStreamReader(audioProc.getInputStream()))) {
                String line;
                while ((line = r.readLine()) != null) {
                    log.debug("FFmpeg audio: {}", line);
                }
            }
            int audioExit = audioProc.waitFor();
            if (audioExit != 0) {
                log.warn("FFmpeg audio replace failed, using video without audio");
                Files.copy(Path.of(tmpVideo), Path.of(videoOutput));
            }

            return videoOutput;

        } finally {
            // Cleanup temp concat file
            Files.deleteIfExists(Path.of(concatFile));
            Files.deleteIfExists(Path.of(tmpVideo));
        }
    }

    private String buildOverlayChain(int numSlides) {
        // Not used in simplified v1 — keeping for future crossfade enhancement
        return "";
    }

    /**
     * Render a single slide's texts onto a 1280x720 image using FFmpeg drawtext.
     */
    private String renderSlideImage(SlideText slide, int index, int durationSec) throws Exception {
        String imgPath = outputDir + "/slide_" + index + "_" + System.currentTimeMillis() + ".png";

        // Build multiline text from slide texts
        String slideText = String.join("\n", slide.texts);
        if (slideText.isEmpty()) {
            slideText = "第 " + (index + 1) + " 页";
        }

        // Escape text for FFmpeg drawtext filter
        String escaped = slideText
            .replace("\\", "\\\\")
            .replace("'", "\\'")
            .replace(":", "\\:")
            .replace("\n", "\\n");

        // Build drawtext command
        // Use fontfile fallback: /usr/share/fonts/truetype/dejavu/DejaVuSans.ttf
        // or built-in if not available
        String fontFile = "/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf";
        if (!Files.exists(Path.of(fontFile))) {
            fontFile = "/usr/share/fonts/truetype/liberation/LiberationSans-Regular.ttf";
        }
        if (!Files.exists(Path.of(fontFile))) {
            fontFile = "/usr/share/fonts/dejavu/DejaVuSans.ttf";
        }
        // Fall back to raw text file if no font
        if (!Files.exists(Path.of(fontFile))) {
            log.warn("No font file found for drawtext, creating blank slide image");
            return createBlankSlideImage(index, durationSec);
        }

        int fontSize = Math.max(18, Math.min(36, 600 / Math.max(slide.texts.size(), 1)));
        int lineHeight = fontSize + 6;
        int boxH = Math.min(600, slide.texts.size() * lineHeight + 40);
        int boxY = (720 - boxH) / 2;

        // Build text lines using drawtext with line wrapping via fixed width
        String[] lines = slide.texts.toArray(new String[0]);
        if (lines.length == 0) lines = new String[]{slideText};

        StringBuilder textParts = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].replace("\\", "\\\\").replace("'", "\\'").replace(":", "\\:");
            textParts.append("'" + line + "'");
            if (i < lines.length - 1) textParts.append(":").append("\n");
        }

        ProcessBuilder pb = new ProcessBuilder(
            ffmpegPath,
            "-y",
            "-f", "lavfi",
            "-i", "color=c=0x1a1a2e:s=1280x720:d=" + durationSec,
            "-vf", String.format(
                "drawtext=fontfile='%s':text=%s:fontsize=%d:fontcolor=0xFFFFFF:x=(w-text_w)/2:y=%d:" +
                "borderw=2:bordercolor=0x000000@0.5:line_spacing=6",
                fontFile, textParts.toString(), fontSize, boxY + 20
            ),
            "-frames:v", "1",
            "-pix_fmt", "yuv420p",
            imgPath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {
                log.debug("FFmpeg drawtext: {}", line);
            }
        }
        int exitCode = process.waitFor();
        if (exitCode != 0 || !Files.exists(Path.of(imgPath))) {
            log.warn("drawtext failed ({}), creating blank slide", exitCode);
            return createBlankSlideImage(index, durationSec);
        }
        return imgPath;
    }

    private String createBlankSlideImage(int index, int durationSec) throws Exception {
        String imgPath = outputDir + "/blank_slide_" + index + "_" + System.currentTimeMillis() + ".png";
        ProcessBuilder pb = new ProcessBuilder(
            ffmpegPath,
            "-y",
            "-f", "lavfi",
            "-i", "color=c=0x1a1a2e:s=1280x720:d=" + durationSec,
            "-frames:v", "1",
            "-pix_fmt", "yuv420p",
            imgPath
        );
        pb.redirectErrorStream(true);
        Process process = pb.start();
        try (BufferedReader r = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = r.readLine()) != null) {}
        }
        process.waitFor();
        return imgPath;
    }

    private void downloadFile(String url, String destPath) throws Exception {
        // Simple URL download using Java HttpURLConnection
        java.net.URL urlObj = new java.net.URL(url);
        java.net.URLConnection conn = urlObj.openConnection();
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(10000);
        try (InputStream in = conn.getInputStream();
             FileOutputStream out = new FileOutputStream(destPath)) {
            byte[] buf = new byte[8192];
            int len;
            while ((len = in.read(buf)) != -1) {
                out.write(buf, 0, len);
            }
        }
    }

    // DTO for parsed slide text
    @lombok.Data
    private static class SlideText {
        private int slideNumber;
        private List<String> texts;
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
            // uploadFile returns the actual object name stored in MinIO (video/UUID.mp4)
            String objectName = fileStorageService.uploadFile(videoFile, "video", videoFile.getName());

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