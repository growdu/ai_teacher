package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.dto.CourseGenerateRequest;
import com.aiteacher.dto.CourseGenerateResponse;
import com.aiteacher.entity.Course;
import com.aiteacher.service.CourseGenerateService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * CourseController Integration Tests
 * Tests course generation, listing, retrieval, update, and deletion
 */
@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private CourseGenerateService courseGenerateService;

    @Test
    void getCourseById_withValidId_shouldReturnCourse() throws Exception {
        Course mockCourse = new Course();
        mockCourse.setId(1L);
        mockCourse.setTitle("牛顿第二定律");
        mockCourse.setStatus("generated");
        mockCourse.setCreatedAt(LocalDateTime.now());

        when(courseGenerateService.getById(1L)).thenReturn(mockCourse);

        mockMvc.perform(get("/api/course/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.title").value("牛顿第二定律"))
                .andExpect(jsonPath("$.data.status").value("generated"));
    }

    @Test
    void getCourseById_withNonExistentId_shouldReturnEmpty() throws Exception {
        when(courseGenerateService.getById(999L)).thenReturn(null);

        mockMvc.perform(get("/api/course/999"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    void pageCourses_shouldReturnPaginatedResults() throws Exception {
        Course course1 = new Course();
        course1.setId(1L);
        course1.setTitle("课程A");
        course1.setStatus("generated");

        Course course2 = new Course();
        course2.setId(2L);
        course2.setTitle("课程B");
        course2.setStatus("draft");

        Page<Course> mockPage = new Page<>(1, 10);
        mockPage.setTotal(2);
        mockPage.setRecords(List.of(course1, course2));

        when(courseGenerateService.page(any(Page.class), any(LambdaQueryWrapper.class)))
                .thenReturn(mockPage);

        mockMvc.perform(get("/api/course/page")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(2))
                .andExpect(jsonPath("$.data.records").isArray())
                .andExpect(jsonPath("$.data.records[0].title").value("课程A"))
                .andExpect(jsonPath("$.data.records[1].title").value("课程B"));
    }

    @Test
    void deleteCourse_withValidId_shouldReturnTrue() throws Exception {
        when(courseGenerateService.updateById(any(Course.class))).thenReturn(true);

        mockMvc.perform(delete("/api/course/1")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void updateCourse_shouldReturnUpdatedResult() throws Exception {
        Course updated = new Course();
        updated.setId(1L);
        updated.setTitle("更新后的课程标题");

        when(courseGenerateService.updateById(any(Course.class))).thenReturn(true);

        mockMvc.perform(put("/api/course/1")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(true));
    }

    @Test
    void generateCourse_withValidRequest_shouldReturnCourseResponse() throws Exception {
        CourseGenerateResponse response = CourseGenerateResponse.builder()
                .courseId(10L)
                .title("物理-高一-牛顿第二定律")
                .status("generating")
                .build();

        when(courseGenerateService.generateCourse(any(CourseGenerateRequest.class), any(Long.class)))
                .thenReturn(response);

        CourseGenerateRequest request = CourseGenerateRequest.builder()
                .knowledgePointId(1L)
                .chapterCount(4)
                .build();

        mockMvc.perform(post("/api/course/generate")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.courseId").value(10))
                .andExpect(jsonPath("$.data.title").value("物理-高一-牛顿第二定律"));
    }
}