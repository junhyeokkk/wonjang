package com.junhyeok.wonjang.common.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

// @RestControllerAdvice: 모든 @RestController의 예외를 한 곳에서 처리
// NestJS의 ExceptionFilter와 같은 역할
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

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

    // JSON 파싱 실패. ADR-005 로 `"amount": 100.5` 같은 소수 입력도 Jackson 이 여기로 떨어뜨린다.
    // 400 INVALID_JSON.
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handle(HttpMessageNotReadableException e){
        // TODO: 400 응답. ErrorResponse.of("INVALID_JSON", "요청 본문을 파싱할 수 없습니다",
        //       e.getMostSpecificCause().getMessage())
        return null;
    }

    // 필수 헤더 누락 (주로 쓰기 API 의 X-User-Id). 400 MISSING_HEADER. detail 은 e.getHeaderName().
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handle(MissingRequestHeaderException e){
        // TODO: 400 응답. ErrorResponse.of("MISSING_HEADER",
        //       "필수 헤더가 누락되었습니다: " + e.getHeaderName(), e.getHeaderName())
        return null;
    }

    // 경로/쿼리 파라미터 타입 변환 실패 (예: GET /accounts/abc 에서 Long 변환 실패).
    // 400 INVALID_PARAMETER. detail 은 e.getName().
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handle(MethodArgumentTypeMismatchException e){
        // TODO: 400 응답. ErrorResponse.of("INVALID_PARAMETER",
        //       "파라미터 타입이 잘못되었습니다: " + e.getName(), e.getName())
        return null;
    }

    // 안전망. 위에서 안 잡힌 모든 예외는 500. 내부 메시지 노출 금지 — 로그로만.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handle(Exception e){
        // TODO: log.error("unhandled exception", e);
        // TODO: 500 응답. ErrorResponse.of("INTERNAL_ERROR", "서버 오류", null)
        return null;
    }

}