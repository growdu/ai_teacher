package com.aiteacher.service;

import com.aiteacher.dto.VideoGenerateRequest;
import com.aiteacher.entity.AsyncTask;
import com.aiteacher.entity.Course;
import com.aiteacher.mapper.CourseMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * VideoGenerationService Unit Tests
 *
 * Tests: async task creation, private method fallback paths.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class VideoGenerationServiceTest {

    @Mock
    private AIService aiService;

    @Mock
    private AsyncTaskService asyncTaskService;

    @Mock
    private CourseMapper courseMapper;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private VideoGenerationService videoGenerationService;

    @TempDir
    java.nio.file.Path tempDir;

    private VideoGenerateRequest validRequest;
    private AsyncTask mockTask;
    private Course mockCourse;

    @BeforeEach
    void setUp() throws Exception {
        validRequest = VideoGenerateRequest.builder()
                .courseId(1L)
                .pptId(10L)
                .avatarId("avatar_01")
                .voiceId("voice_01")
                .hasAvatar(false)
                .hasPptBackground(true)
                .script("这是测试脚本")
                .build();

        mockTask = new AsyncTask();
        mockTask.setId(999L);
        mockTask.setStatus("pending");

        mockCourse = new Course();
        mockCourse.setId(1L);
        mockCourse.setTitle("测试课程");
        mockCourse.setScript("课程脚本内容");

        // Pre-stub task creation for all tests
        lenient().doReturn(mockTask).when(asyncTaskService)
                .createTask(anyString(), anyString(), anyLong(), anyLong());

        // Inject temp output dir via reflection
        setField(videoGenerationService, "outputDir", tempDir.toString());
        setField(videoGenerationService, "ffmpegPath", "ffmpeg");
        setField(videoGenerationService, "objectMapper", new com.fasterxml.jackson.databind.ObjectMapper());
    }

    private static void setField(Object target, String name, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(name);
        f.setAccessible(true);
        f.set(target, value);
    }

    // Test: async endpoint creates task and returns taskId
    @Test
    void generateVideoAsync_shouldCreateTaskAndReturnTaskId() {
        when(asyncTaskService.createTask(eq("video"), eq("视频生成"), anyLong(), anyLong()))
                .thenReturn(mockTask);

        Long taskId = videoGenerationService.generateVideoAsync(validRequest, 1L, 1L);

        assertEquals(999L, taskId);
        verify(asyncTaskService).createTask(eq("video"), eq("视频生成"), eq(1L), eq(1L));
    }

    // Test: extractSlideTexts returns empty when files are missing
    @Test
    void extractSlideTexts_shouldReturnEmptyJsonWhenFilesNotFound() throws Exception {
        var method = VideoGenerationService.class.getDeclaredMethod(
                "extractSlideTexts", String.class, String.class);
        method.setAccessible(true);

        String result = (String) method.invoke(
                videoGenerationService, "/nonexistent/script.js", "/tmp/fake.pptx");

        assertEquals("[]", result);
    }
}
