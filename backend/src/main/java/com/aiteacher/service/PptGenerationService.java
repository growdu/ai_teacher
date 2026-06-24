package com.aiteacher.service;

import com.aiteacher.dto.CourseOutline;
import com.aiteacher.dto.PptGenerateRequest;
import com.aiteacher.entity.Course;
import com.aiteacher.entity.TeachingMaterial;
import com.aiteacher.mapper.CourseMapper;
import com.aiteacher.mapper.TeachingMaterialMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * PPT Generation Service - generates PPT from course outline
 */
@Slf4j
@Service
public class PptGenerationService {

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private TeachingMaterialMapper teachingMaterialMapper;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${ppt.node-script:./scripts/generate-ppt.js}")
    private String nodeScriptPath;

    @Value("${ppt.output-dir:/tmp/ppt}")
    private String outputDir;

    /**
     * Generate PPT from course
     */
    public TeachingMaterial generatePpt(Long courseId, String template, Long userId) {
        try {
            // 1. Get course data
            Course course = courseMapper.selectById(courseId);
            if (course == null) {
                throw new RuntimeException("Course not found: " + courseId);
            }

            // 2. Parse outline
            CourseOutline outline = parseOutline(course.getOutline());

            // 3. Build PPT config
            PptGenerateRequest config = buildPptConfig(course, outline, template);

            // 4. Generate PPT
            String outputPath = generatePptFile(config);

            // 5. Upload to file storage
            File pptFile = new File(outputPath);
            String fileUrl = uploadPptFile(pptFile, course.getTitle());

            // 6. Save teaching material
            TeachingMaterial material = saveTeachingMaterial(course, fileUrl, pptFile.length());

            // 7. Cleanup temp file
            Files.deleteIfExists(pptFile.toPath());

            return material;

        } catch (Exception e) {
            log.error("Failed to generate PPT: {}", e.getMessage(), e);
            throw new RuntimeException("PPT生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * Generate PPT from request directly
     */
    public TeachingMaterial generatePptFromRequest(PptGenerateRequest request, Long userId) {
        try {
            // 1. Generate PPT
            String outputPath = generatePptFile(request);

            // 2. Upload to file storage
            File pptFile = new File(outputPath);
            String fileUrl = uploadPptFile(pptFile, request.getTitle());

            // 3. Save teaching material
            TeachingMaterial material = new TeachingMaterial();
            material.setCourseId(request.getCourseId());
            material.setMaterialType("ppt");
            material.setTitle(request.getTitle());
            material.setFileUrl(fileUrl);
            material.setFileSize(pptFile.length());
            material.setStatus("generated");
            material.setCreatedAt(java.time.LocalDateTime.now());

            teachingMaterialMapper.insert(material);

            // 4. Cleanup
            Files.deleteIfExists(pptFile.toPath());

            return material;

        } catch (Exception e) {
            log.error("Failed to generate PPT: {}", e.getMessage(), e);
            throw new RuntimeException("PPT生成失败: " + e.getMessage(), e);
        }
    }

    private CourseOutline parseOutline(String outlineJson) {
        try {
            return objectMapper.readValue(outlineJson, CourseOutline.class);
        } catch (Exception e) {
            log.warn("Failed to parse outline, using default: {}", e.getMessage());
            CourseOutline outline = new CourseOutline();
            outline.setTitle("课程");
            outline.setChapters(new ArrayList<>());
            return outline;
        }
    }

    private PptGenerateRequest buildPptConfig(Course course, CourseOutline outline, String template) {
        List<PptGenerateRequest.SlideData> slides = new ArrayList<>();

        // 1. Title slide
        slides.add(PptGenerateRequest.SlideData.builder()
                .type("title")
                .build());

        // 2. Chapter slides
        if (outline.getChapters() != null) {
            for (int i = 0; i < outline.getChapters().size(); i++) {
                CourseOutline.Chapter chapter = outline.getChapters().get(i);

                // Chapter header
                slides.add(PptGenerateRequest.SlideData.builder()
                        .type("chapter")
                        .title(chapter.getTitle())
                        .duration(chapter.getDuration())
                        .build());

                // Content slide
                List<String> contentList = new ArrayList<>();
                if (chapter.getKeyPoints() != null) {
                    contentList.addAll(chapter.getKeyPoints());
                }
                if (chapter.getTeachingNotes() != null) {
                    contentList.add(chapter.getTeachingNotes());
                }

                slides.add(PptGenerateRequest.SlideData.builder()
                        .type("content")
                        .title(chapter.getTitle())
                        .contentList(contentList)
                        .keyPoints(chapter.getKeyPoints())
                        .build());
            }
        }

        // 3. Summary slide
        slides.add(PptGenerateRequest.SlideData.builder()
                .type("summary")
                .title("课堂小结")
                .points(outline.getChapters() != null ?
                        outline.getChapters().stream().map(CourseOutline.Chapter::getTitle).toList() :
                        new ArrayList<>())
                .build());

        // 4. End slide
        slides.add(PptGenerateRequest.SlideData.builder()
                .type("end")
                .build());

        return PptGenerateRequest.builder()
                .title(outline.getTitle() != null ? outline.getTitle() : course.getTitle())
                .subtitle(outline.getDescription())
                .template(template != null ? template : "default")
                .slides(slides)
                .courseId(course.getId())
                .build();
    }

    private String generatePptFile(PptGenerateRequest config) throws Exception {
        // Create output directory
        Path outputPath = Path.of(outputDir);
        Files.createDirectories(outputPath);

        // Create temp config file
        String configFile = outputDir + "/config_" + System.currentTimeMillis() + ".json";
        String outputFile = outputDir + "/ppt_" + System.currentTimeMillis() + ".pptx";

        try {
            // Write config to file
            String configJson = objectMapper.writeValueAsString(config);
            Files.writeString(Path.of(configFile), configJson);

            // Execute node script
            ProcessBuilder pb = new ProcessBuilder(
                    "node",
                    nodeScriptPath,
                    outputFile,
                    configFile
            );
            pb.environment().put("NODE_PATH", "/usr/local/lib/node_modules");
            pb.redirectErrorStream(true);

            Process process = pb.start();

            // Read output
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.info("PPT generation: {}", line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException("PPT generation script failed with exit code: " + exitCode);
            }

            // Check output file exists
            if (!Files.exists(Path.of(outputFile))) {
                throw new RuntimeException("PPT file was not generated");
            }

            return outputFile;

        } finally {
            // Cleanup config file
            Files.deleteIfExists(Path.of(configFile));
        }
    }

    private String uploadPptFile(File file, String title) {
        try {
            // uploadFile returns the actual object name stored in MinIO (ppt/UUID.pptx)
            String objectName = fileStorageService.uploadFile(file, "ppt", title + ".pptx");
            return fileStorageService.getFileUrl(objectName);
        } catch (Exception e) {
            log.error("Failed to upload PPT: {}", e.getMessage());
            throw new RuntimeException("PPT上传失败: " + e.getMessage(), e);
        }
    }

    private TeachingMaterial saveTeachingMaterial(Course course, String fileUrl, Long fileSize) {
        TeachingMaterial material = new TeachingMaterial();
        material.setCourseId(course.getId());
        material.setTenantId(course.getTenantId());
        material.setMaterialType("ppt");
        material.setTitle(course.getTitle());
        material.setFileUrl(fileUrl);
        material.setFileSize(fileSize);
        material.setStatus("generated");
        material.setCreatedAt(java.time.LocalDateTime.now());

        teachingMaterialMapper.insert(material);
        return material;
    }
}