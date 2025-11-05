package com.example.demo.adapter.web;

import com.example.demo.application.service.MultiMerchantOrderException;
import com.example.demo.application.service.OrderNotFoundException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global exception handler for web layer.
 * Requirements: 1.1.4, 1.1.5, 1.3.7
 */
@RestControllerAdvice
@Slf4j
public class WebExceptionHandler {
    
    @ExceptionHandler(IllegalStateException.class)
    public ErrorResponse handleException(IllegalStateException ex) {
        return errorResponse(ex, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ErrorResponse handleException(ConstraintViolationException ex) {
        return errorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    /**
     * Handles validation errors from @Valid annotations.
     * Requirement: 1.1.4
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ErrorResponse handleException(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
            .map(error -> error.getField() + ": " + error.getDefaultMessage())
            .reduce((a, b) -> a + "; " + b)
            .orElse("请求参数验证失败");
        
        return ErrorResponse.builder(ex, HttpStatus.BAD_REQUEST, message)
                .title("ValidationError")
                .build();
    }

    /**
     * Handles multi-merchant order exception.
     * Requirement: 1.1.5
     */
    @ExceptionHandler(MultiMerchantOrderException.class)
    public ErrorResponse handleException(MultiMerchantOrderException ex) {
        return errorResponse(ex, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(OrderNotFoundException.class)
    public ErrorResponse handleException(OrderNotFoundException ex) {
        return errorResponse(ex, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(Exception.class)
    public ErrorResponse handleException(Exception ex) {
        log.error(ex.getMessage(), ex);
        return errorResponse(ex, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    private static ErrorResponse errorResponse(Exception exception, HttpStatus status) {
        return ErrorResponse.builder(exception, status, exception.getMessage())
                .title(exception.getClass().getSimpleName())
                .build();
    }
}
