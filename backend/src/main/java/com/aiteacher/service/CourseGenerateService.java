package com.aiteacher.service;

import com.aiteacher.dto.CourseGenerateRequest;
import com.aiteacher.dto.CourseGenerateResponse;
import com.aiteacher.dto.CourseOutline;
import com.aiteacher.entity.Course;
import com.aiteacher.entity.KnowledgePoint;
import com.aiteacher.entity.User;
import com.aiteacher.mapper.CourseMapper;
import com.aiteacher.mapper.KnowledgePointMapper;
import com.aiteacher.mapper.UserMapper;
import com.aiteacher.provider.ai.ChatRequest;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.model.ChatMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Course Generation Service - generates course outlines and scripts using LLM
 */
@Slf4j
@Service
public class CourseGenerateService {

    @Autowired
    private AIService aiService;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private KnowledgePointMapper knowledgePointMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String OUTLINE_PROMPT_TEMPLATE = """
            你是一位专业的教育专家，请为以下知识点生成课程大纲。

            知识点：%s
            学科：%s
            学段：%s
            章节数量：%d

            请以JSON格式返回课程大纲，格式如下：
            {
              "title": "课程标题",
              "description": "课程简介",
              "chapters": [
                {
                  "title": "章节标题",
                  "duration": 10,
                  "keyPoints": ["重点1", "重点2"],
                  "teachingNotes": "教学备注"
                }
              ],
              "totalDuration": 总时长（分钟）
            }

            要求：
            1. 内容科学严谨，符合教学规范
            2. 章节逻辑清晰，由浅入深
            3. 每个章节时长建议10-15分钟
            4. 重点难点要突出
            """;

    private static final String SCRIPT_PROMPT_TEMPLATE = """
            你是一位经验丰富的教师，请为以下课程大纲生成详细的讲稿脚本。

            课程大纲：
            %s

            要求：
            1. 语言生动流畅，适合口头讲解
            2. 内容准确详实，有实例说明
            3. 注意知识点的过渡和衔接
            4. 控制讲解节奏，适当留白
            5. 如果有数学公式或图表，请使用文字描述

            请直接返回讲稿内容，不要添加额外说明。
            """;

    /**
     * Generate a complete course from knowledge point
     */
    public CourseGenerateResponse generateCourse(CourseGenerateRequest request, Long userId) {
        try {
            // 1. Save knowledge point
            KnowledgePoint knowledgePoint = saveKnowledgePoint(request, userId);

            // 2. Generate course outline using LLM
            CourseOutline outline = generateOutline(request);

            // 3. Generate script using LLM
            String script = generateScript(outline);

            // 4. Save course
            Course course = saveCourse(knowledgePoint, outline, script, userId);

            return CourseGenerateResponse.builder()
                    .courseId(course.getId())
                    .title(outline.getTitle())
                    .outline(outline)
                    .script(script)
                    .status(course.getStatus())
                    .build();

        } catch (Exception e) {
            log.error("Failed to generate course: {}", e.getMessage(), e);
            throw new RuntimeException("课程生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * Generate course outline using LLM
     */
    public CourseOutline generateOutline(CourseGenerateRequest request) {
        String prompt = String.format(
                OUTLINE_PROMPT_TEMPLATE,
                request.getKnowledgePoint(),
                request.getSubject() != null ? request.getSubject() : "通用",
                request.getGrade() != null ? request.getGrade() : "通用",
                request.getChaptersCount() != null ? request.getChaptersCount() : 4
        );

        try {
            ChatResponse response = aiService.chat(ChatRequest.of(prompt));
            String content = response.getContent();
            
            // Parse JSON response
            return objectMapper.readValue(content, CourseOutline.class);
        } catch (Exception e) {
            log.error("Failed to generate outline: {}", e.getMessage());
            throw new RuntimeException("课程大纲生成失败: " + e.getMessage(), e);
        }
    }

    /**
     * Generate script using LLM
     */
    public String generateScript(CourseOutline outline) {
        try {
            String outlineJson = objectMapper.writeValueAsString(outline);
            String prompt = String.format(SCRIPT_PROMPT_TEMPLATE, outlineJson);

            ChatResponse response = aiService.chat(ChatRequest.of(prompt));
            return response.getContent();
        } catch (Exception e) {
            log.error("Failed to generate script: {}", e.getMessage());
            throw new RuntimeException("讲稿生成失败: " + e.getMessage(), e);
        }
    }

    private KnowledgePoint saveKnowledgePoint(CourseGenerateRequest request, Long userId) {
        User user = userMapper.selectById(userId);
        Long tenantId = user != null ? user.getTenantId() : 1L;

        KnowledgePoint knowledgePoint = new KnowledgePoint();
        knowledgePoint.setTenantId(tenantId);
        knowledgePoint.setContent(request.getKnowledgePoint());
        knowledgePoint.setSubject(request.getSubject());
        knowledgePoint.setGrade(request.getGrade());
        knowledgePoint.setCreatedAt(java.time.LocalDateTime.now());

        knowledgePointMapper.insert(knowledgePoint);
        return knowledgePoint;
    }

    private Course saveCourse(KnowledgePoint knowledgePoint, CourseOutline outline, String script, Long userId) {
        User user = userMapper.selectById(userId);
        Long tenantId = user != null ? user.getTenantId() : 1L;

        Course course = new Course();
        course.setTenantId(tenantId);
        course.setKnowledgePointId(knowledgePoint.getId());
        course.setTitle(outline.getTitle());
        
        try {
            course.setOutline(objectMapper.writeValueAsString(outline));
        } catch (Exception e) {
            course.setOutline("{}");
        }
        
        course.setScript(script);
        course.setStatus("generated");
        course.setCreatorId(userId);
        course.setCreatedAt(java.time.LocalDateTime.now());

        courseMapper.insert(course);
        return course;
    }
}