package com.junhyeok.wonjang.common.exception;

import org.springframework.validation.FieldError;

// API 에러 응답 형식: { "code": "...", "message": "...", "detail": ... }
// record: 불변 DTO — 필드, 생성자, getter, equals/hashCode/toString 자동 생성
public record ErrorResponse(String code, String message, Object detail) {

    // 정적 팩토리 메서드 of(DomainException e) → new ErrorResponse(...) 반환
    public static ErrorResponse of(DomainException e) {
        return new ErrorResponse(e.getCode().code(), e.getMessage(), e.getDetail());
    }

    // 정적 팩토리 메서드 of(String code, String message) → detail=null로 반환
    public static ErrorResponse of(String code, String message){
        return new ErrorResponse(code, message, null);
    }

    // 정적 팩토리 메서드 of(String code, String message) → detail=null로 반환
    public static ErrorResponse of(String code, String message, Object detail){
        return new ErrorResponse(code, message, detail);
    }


}