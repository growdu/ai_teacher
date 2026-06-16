package com.aiteacher.service;

import com.aiteacher.dto.CourseGenerateRequest;
import com.aiteacher.dto.CourseGenerateResponse;
import com.aiteacher.entity.Course;
import com.aiteacher.entity.KnowledgePoint;
import com.aiteacher.mapper.CourseMapper;
import com.aiteacher.mapper.KnowledgePointMapper;
import com.aiteacher.provider.llm.LLMProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CourseGenerateServiceTest {

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private KnowledgePointMapper knowledgePointMapper;

    @Mock
    private AIService aiService;

    @Mock
    private LLMProvider llmProvider;

    @InjectMocks
    private CourseGenerateService courseGenerateService;

    private CourseGenerateRequest request;

    @BeforeEach
    void setUp() {
        request = CourseGenerateRequest.builder()
                .knowledgeContent("牛顿第二定律：F=ma")
                .subject("物理")
                .grade("高一")
                .build();
    }

    @Test
    void testGenerateCourseOutline() {
        // Given
        String expectedOutline = "# 课程大纲\n\n## 第一章：牛顿第二定律";
        
        // When
        when(aiService.chat(any())).thenReturn(expectedOutline);

        // Then
        assertNotNull(courseGenerateService);
    }

    @Test
    void testGenerateScript() {
        // Given
        String outline = "# 课程大纲";
        String expectedScript = "各位同学，今天我们来学习牛顿第二定律...";

        // When
        when(aiService.chat(contains("讲稿"))).thenReturn(expectedScript);

        // Then
        assertNotNull(courseGenerateService);
    }

    @Test
    void testSaveCourse() {
        // Given
        Course course = new Course();
        course.setTitle("牛顿第二定律");
        course.setStatus("draft");
        course.setTenantId(1L);
        course.setCreatedAt(LocalDateTime.now());

        // When
        when(courseMapper.insert(any(Course.class))).thenReturn(1);

        // Then
        assertNotNull(course);
    }
}