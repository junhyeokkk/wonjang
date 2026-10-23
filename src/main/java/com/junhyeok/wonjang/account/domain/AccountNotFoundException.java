package com.junhyeok.wonjang.account.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * 요청한 ID의 계정이 존재하지 않을 때 던진다.
 * 응답: 404 NOT_FOUND - detail 없음.
 */
public class AccountNotFoundException extends DomainException {

    public AccountNotFoundException(Long id) {
        super(
                AccountErrorCode.ACCOUNT_NOT_FOUND,
                "Account not found: id=" + id,
                HttpStatus.NOT_FOUND
        );
    }

    public AccountNotFoundException(String code) {
        super(
                AccountErrorCode.ACCOUNT_NOT_FOUND,
                "Account not found: code=" + code,
                HttpStatus.NOT_FOUND
        );
    }
}