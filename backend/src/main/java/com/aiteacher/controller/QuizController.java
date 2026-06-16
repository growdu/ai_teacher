package com.aiteacher.controller;

import com.aiteacher.common.R;
import com.aiteacher.dto.QuizGenerateRequest;
import com.aiteacher.dto.QuizGenerateResponse;
import com.aiteacher.service.QuizGenerationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/quiz")
public class QuizController {

    @Autowired
    private QuizGenerationService quizGenerationService;

    @PostMapping("/generate")
    public R<QuizGenerateResponse> generate(
            @Valid @RequestBody QuizGenerateRequest request,
            Authentication authentication) {
        Long userId = getUserId(authentication);
        return R.ok(quizGenerationService.generateQuiz(request, userId));
    }

    private Long getUserId(Authentication authentication) {
        if (authentication != null && authentication.getDetails() != null) {
            return (Long) authentication.getDetails();
        }
        return 1L;
    }
}