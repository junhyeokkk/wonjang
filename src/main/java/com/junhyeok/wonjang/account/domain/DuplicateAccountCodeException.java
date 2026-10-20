package com.junhyeok.wonjang.account.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

// 계정 코드 중복 시 — HTTP 409 Conflict
public class DuplicateAccountCodeException extends DomainException {

    // 계정 중복 발생
    public DuplicateAccountCodeException(String code) {
        super(AccountErrorCode.DUPLICATE_ACCOUNT_CODE, "Account code already exists: " + code, HttpStatus.CONFLICT);
    }
}