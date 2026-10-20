package com.junhyeok.wonjang.common.exception;
import org.springframework.http.HttpStatus;

// 모든 도메인 예외의 부모 클래스
public class DomainException extends RuntimeException { // RuntimeException을 상속 -> @Transactional이 자동 롤백 처리

    // code, status, detail 필드
    private final ErrorCode code;       // 에러 코드 (예: "ACCOUNT_NOT_FOUND")
    private final HttpStatus status;    // HTTP 상태
    private final Object detail;        // detail: 추가 정보

    // 3-arg 생성자 (code, message, status) → detail=null로 위임
    public DomainException(ErrorCode code, String message, HttpStatus status) {
        this(code, message, status, null);  // this()로 4-arg 호출
    }

    // 4-arg 생성자 (code, message, status, detail) → super(message) 호출
    public DomainException(ErrorCode code, String message, HttpStatus status, Object detail) {
        super(message);
        this.code = code;
        this.status = status;
        this.detail = detail;
    }

    public ErrorCode getCode() {
        return code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public Object getDetail() {
        return detail;
    }
}