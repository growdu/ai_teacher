package com.aiteacher.service;

import com.aiteacher.dto.CourseGenerateRequest;
import com.aiteacher.entity.Course;
import com.aiteacher.entity.KnowledgePoint;
import com.aiteacher.mapper.CourseMapper;
import com.aiteacher.mapper.KnowledgePointMapper;
import com.aiteacher.provider.ai.ChatResponse;
import com.aiteacher.provider.ai.model.ChatChoice;
import com.aiteacher.provider.ai.model.ChatMessage;
import com.aiteacher.provider.ai.model.UsageInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.*;

/**
 * CourseGenerateService Unit Tests
 *
 * Tests the core course generation flow:
 * 1. generateCourseFromKnowledgePoint(knowledgePointId) - full pipeline
 * 2. generateOutline / generateScript - individual LLM calls
 */
@ExtendWith(MockitoExtension.class)
class CourseGenerateServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private KnowledgePointMapper knowledgePointMapper;

    @Mock
    private AIService aiService;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private CourseGenerateService courseGenerateService;

    private KnowledgePoint testKnowledgePoint;

    @BeforeEach
    void setUp() {
        testKnowledgePoint = new KnowledgePoint();
        testKnowledgePoint.setId(1L);
        testKnowledgePoint.setTenantId(1L);
        testKnowledgePoint.setSubject("物理");
        testKnowledgePoint.setGrade("高一");
        testKnowledgePoint.setContent("牛顿第二定律：F=ma");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test: generateCourseFromKnowledgePoint happy path
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void generateCourseFromKnowledgePoint_shouldSaveCourseWithOutlineAndScript() {
        // Given: knowledge point exists
        when(knowledgePointMapper.selectById(1L)).thenReturn(testKnowledgePoint);

        // Mock JDBC queries (used in the service)
        when(jdbcTemplate.query(anyString(), any(RowMapper.class)))
                .thenReturn(List.of(testKnowledgePoint));

        // Mock AI calls - outline generation
        ChatResponse outlineResponse = buildChatResponse(
                "## 课程大纲\n\n### 第一章 牛顿第二定律\n\n- 理解F=ma的物理意义\n- 掌握加速度的计算方法"
        );
        when(aiService.chat(contains("课程大纲"))).thenReturn(outlineResponse);

        // Mock AI calls - script generation
        ChatResponse scriptResponse = buildChatResponse(
                "各位同学好，今天我们来学习牛顿第二定律。\n\n牛顿第二定律表明：物体的加速度与所受合外力成正比，与物体质量成反比..."
        );
        when(aiService.chat(contains("讲稿") | contains("脚本"))).thenReturn(scriptResponse);

        // Mock course insert
        when(courseMapper.insert(any(Course.class))).thenReturn(1);

        // When
        Course result = courseGenerateService.generateCourseFromKnowledgePoint(1L);

        // Then
        assertNotNull(result, "Generated course should not be null");
        assertEquals("物理-高一-牛顿第二定律", result.getTitle());
        assertNotNull(result.getOutline(), "Outline should be generated");
        assertNotNull(result.getScript(), "Script should be generated");
        assertEquals("generated", result.getStatus());

        // Verify outline contains expected content
        assertTrue(result.getOutline().contains("牛顿第二定律"));

        // Verify script was generated
        assertTrue(result.getScript().length() > 0);

        // Verify course was persisted
        verify(courseMapper).insert(any(Course.class));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test: generateCourseFromKnowledgePoint throws when knowledge point not found
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void generateCourseFromKnowledgePoint_shouldThrowWhenNotFound() {
        // Given
        when(knowledgePointMapper.selectById(999L)).thenReturn(null);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of());

        // When / Then
        assertThrows(RuntimeException.class, () ->
                courseGenerateService.generateCourseFromKnowledgePoint(999L)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test: generateCourseFromKnowledgePoint uses correct chapter count
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void generateCourseFromKnowledgePoint_shouldRespectChapterCount() {
        // Given
        when(knowledgePointMapper.selectById(1L)).thenReturn(testKnowledgePoint);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(testKnowledgePoint));

        ChatResponse outlineResponse = buildChatResponse("## 大纲");
        when(aiService.chat(any())).thenReturn(outlineResponse);
        when(courseMapper.insert(any(Course.class))).thenReturn(1);

        CourseGenerateRequest requestWithChapters = CourseGenerateRequest.builder()
                .knowledgePointId(1L)
                .chapterCount(6)
                .build();

        // When
        Course result = courseGenerateService.generateCourseFromKnowledgePoint(1L);

        // Then
        assertNotNull(result);
        // The chapter count is passed to the AI prompt - verified via prompt capture
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(aiService).chat(promptCaptor.capture());
        assertTrue(promptCaptor.getValue().contains("6") ||
                   promptCaptor.getValue().contains("六"),
                "Prompt should contain chapter count");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Test: service handles AI service failure gracefully
    // ─────────────────────────────────────────────────────────────────────────
    @Test
    void generateCourseFromKnowledgePoint_shouldHandleAIFailure() {
        // Given
        when(knowledgePointMapper.selectById(1L)).thenReturn(testKnowledgePoint);
        when(jdbcTemplate.query(anyString(), any(RowMapper.class))).thenReturn(List.of(testKnowledgePoint));
        when(aiService.chat(any())).thenThrow(new RuntimeException("AI service unavailable"));
        when(courseMapper.insert(any(Course.class))).thenReturn(1);

        // When / Then
        assertThrows(RuntimeException.class, () ->
                courseGenerateService.generateCourseFromKnowledgePoint(1L)
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper builders
    // ─────────────────────────────────────────────────────────────────────────
    private ChatResponse buildChatResponse(String content) {
        ChatMessage message = ChatMessage.builder()
                .role("assistant")
                .content(content)
                .build();
        ChatChoice choice = ChatChoice.builder()
                .message(message)
                .index(0)
                .build();
        UsageInfo usage = UsageInfo.builder()
                .promptTokens(100)
                .completionTokens(100)
                .totalTokens(200)
                .build();
        return ChatResponse.builder()
                .id("chatcmpl-test")
                .model("gpt-4o")
                .choices(List.of(choice))
                .usage(usage)
                .created(1234567890L)
                .build();
    }
}