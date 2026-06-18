package com.aiteacher.exception;

import com.aiteacher.common.R;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @Value("${spring.profiles.active:prod}")
    private String activeProfile;

    @ExceptionHandler(BusinessException.class)
    public R<Void> handleBusinessException(BusinessException e) {
        log.warn("Business exception: {}", e.getMessage());
        if (isProdProfile()) {
            return R.fail(e.getCode(), "系统繁忙，请稍后再试");
        }
        return R.fail(e.getCode(), e.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Map<String, String>> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Validation exception: {}", errors);
        if (isProdProfile()) {
            return R.fail(400, "系统繁忙，请稍后再试");
        }
        return R.fail(400, "Validation failed");
    }

    @ExceptionHandler(BindException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public R<Map<String, String>> handleBindException(BindException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        log.warn("Bind exception: {}", errors);
        if (isProdProfile()) {
            return R.fail(400, "系统繁忙，请稍后再试");
        }
        return R.fail(400, "Bind failed");
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public R<Void> handleException(Exception e) {
        log.error("Unexpected exception: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        if (isProdProfile()) {
            return R.fail(500, "系统繁忙，请稍后再试");
        }
        return R.fail(500, "Internal server error: " + e.getMessage());
    }

    private boolean isProdProfile() {
        return "prod".equals(activeProfile);
    }
}