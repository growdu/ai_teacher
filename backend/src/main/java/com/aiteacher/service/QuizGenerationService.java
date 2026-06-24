package com.aiteacher.service;

import com.aiteacher.dto.QuizGenerateRequest;
import com.aiteacher.dto.QuizGenerateResponse;
import com.aiteacher.entity.Course;
import com.aiteacher.mapper.CourseMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Quiz Generation Service - generates quiz questions using LLM
 */
@Slf4j
@Service
public class QuizGenerationService {

    @Autowired
    private AIService aiService;

    @Autowired
    private CourseMapper courseMapper;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Generate quiz questions for a course
     */
    public QuizGenerateResponse generateQuiz(QuizGenerateRequest request, Long userId) {
        try {
            // 1. Get course data
            Course course = courseMapper.selectById(request.getCourseId());
            if (course == null) {
                throw new RuntimeException("Course not found");
            }

            // 2. Build prompt for quiz generation
            String prompt = buildQuizPrompt(request, course);
            log.info("Quiz prompt: {}", prompt);

            // 3. Call LLM to generate quiz
            log.info("=== BEFORE AI CHAT CALL ===");
            String response = aiService.chat(prompt);
            log.info("=== AFTER AI CHAT CALL, response length={} ===", response.length());
            log.info("AI response starts with: {}", response.substring(0, Math.min(200, response.length())));

            // 4. Parse response
            QuizGenerateResponse result = parseQuizResponse(response, request.getCourseId());
            log.info("Parsed questions count: {}", result.getQuestions() == null ? 0 : result.getQuestions().size());
            return result;

        } catch (Exception e) {
            log.error("Quiz generation failed: {}", e.getMessage(), e);
            throw new RuntimeException("试题生成失败: " + e.getMessage(), e);
        }
    }

    private String buildQuizPrompt(QuizGenerateRequest request, Course course) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("请为以下课程内容生成").append(request.getCount() != null ? request.getCount() : 5).append("道试题。\n\n");
        prompt.append("课程标题: ").append(course.getTitle()).append("\n");
        
        if (course.getScript() != null && !course.getScript().isEmpty()) {
            prompt.append("课程内容:\n").append(course.getScript()).append("\n\n");
        }
        
        prompt.append("要求:\n");
        prompt.append("- 难度: ").append(request.getDifficulty() != null ? request.getDifficulty() : "medium").append("\n");
        prompt.append("- 题型: ").append(request.getType() != null ? request.getType() : "mixed").append("\n");
        prompt.append("- 科目: ").append(request.getSubject() != null ? request.getSubject() : "未知").append("\n");
        prompt.append("- 年级: ").append(request.getGrade() != null ? request.getGrade() : "未知").append("\n\n");
        
        prompt.append("请以JSON格式返回，格式如下：\n");
        prompt.append("{\n");
        prompt.append("  \"questions\": [\n");
        prompt.append("    {\n");
        prompt.append("      \"type\": \"choice\",\n");
        prompt.append("      \"content\": \"题目内容\",\n");
        prompt.append("      \"options\": [\"A. 选项1\", \"B. 选项2\", \"C. 选项3\", \"D. 选项4\"],\n");
        prompt.append("      \"answer\": \"B\",\n");
        prompt.append("      \"explanation\": \"解析\"\n");
        prompt.append("    }\n");
        prompt.append("  ]\n");
        prompt.append("}\n");
        
        return prompt.toString();
    }

    private QuizGenerateResponse parseQuizResponse(String response, Long courseId) {
        try {
            // Try to parse as JSON
            response = response.trim();
            if (response.startsWith("```json")) {
                response = response.substring(7);
            }
            if (response.endsWith("```")) {
                response = response.substring(0, response.length() - 3);
            }
            response = response.trim();

            QuizGenerateResponse quizResponse = objectMapper.readValue(response, QuizGenerateResponse.class);
            quizResponse.setCourseId(courseId);
            return quizResponse;

        } catch (Exception e) {
            log.warn("Failed to parse quiz response as JSON: {}", e.getMessage());
            // Fallback: create a simple quiz from text
            return createSimpleQuiz(response, courseId);
        }
    }

    private QuizGenerateResponse createSimpleQuiz(String text, Long courseId) {
        List<QuizGenerateResponse.Question> questions = new ArrayList<>();
        
        // Simple parsing - split by question markers
        String[] lines = text.split("\n");
        QuizGenerateResponse.Question currentQuestion = null;
        
        for (String line : lines) {
            line = line.trim();
            if (line.isEmpty()) continue;
            
            if (line.matches("\\d+[.、].*") || line.matches(".*\\?.*") || line.matches(".*\\.$")) {
                if (currentQuestion != null) {
                    questions.add(currentQuestion);
                }
                currentQuestion = QuizGenerateResponse.Question.builder()
                        .type("essay")
                        .content(line)
                        .build();
            }
        }
        
        if (currentQuestion != null) {
            questions.add(currentQuestion);
        }
        
        return QuizGenerateResponse.builder()
                .courseId(courseId)
                .questions(questions.isEmpty() ? List.of(
                        QuizGenerateResponse.Question.builder()
                                .type("essay")
                                .content(text)
                                .build()
                ) : questions)
                .build();
    }
}
