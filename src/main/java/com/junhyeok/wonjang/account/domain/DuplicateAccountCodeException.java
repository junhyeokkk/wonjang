package com.junhyeok.wonjang.account.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * 이미 존재하는 계정 코드로 계정을 만들려 할 때 던진다.
 * 응답: 409 CONFLICT - detail 없음
 */
public class DuplicateAccountCodeException extends DomainException {

    public DuplicateAccountCodeException(String code) {
        super(AccountErrorCode.DUPLICATE_ACCOUNT_CODE, "Account code already exists: " + code, HttpStatus.CONFLICT);
    }
}