package com.junhyeok.wonjang.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

// @RestControllerAdvice: 모든 @RestController의 예외를 한 곳에서 처리
// NestJS의 ExceptionFilter와 같은 역할
@RestControllerAdvice
public class GlobalExceptionHandler {

    // DomainException 처리
    @ExceptionHandler(DomainException.class)
    public ResponseEntity<ErrorResponse> handle(DomainException e){
        return ResponseEntity
                .status(e.getStatus())
                .body(ErrorResponse.of(e));
    }
    // @Valid 검증 실패 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentNotValidException e){
        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(ErrorResponse.of("VALIDATION_ERROR", e.getMessage(), e.getBindingResult().getFieldError()));
    }

}