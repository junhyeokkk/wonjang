package com.junhyeok.wonjang.account.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

// 계정을 찾지 못했을 때 — HTTP 404
public class AccountNotFoundException extends DomainException {

    // 계정을 찾을 수 없음
    public AccountNotFoundException(Long id) {
        super(AccountErrorCode.ACCOUNT_NOT_FOUND, "Account not found: id=" + id, HttpStatus.NOT_FOUND);
    }
}