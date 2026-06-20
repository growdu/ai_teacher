package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.config.TenantContext;
import com.aiteacher.dto.QuizGenerateRequest;
import com.aiteacher.dto.QuizGenerateResponse;
import com.aiteacher.service.QuizGenerationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuizGenerationService quizGenerationService;

    @PostMapping("/generate")
    public R<QuizGenerateResponse> generate(@Valid @RequestBody QuizGenerateRequest request) {
        Long userId = TenantContext.getUserId();
        if (userId == null) {
            throw new com.aiteacher.exception.BusinessException(401, "未授权");
        }
        return R.ok(quizGenerationService.generateQuiz(request, userId));
    }
}
