package com.junhyeok.wonjang.account.domain;
import com.junhyeok.wonjang.common.exception.ErrorCode;

public enum AccountErrorCode implements ErrorCode {
    ACCOUNT_NOT_FOUND,
    DUPLICATE_ACCOUNT_CODE;

    public String code() {
        return name();
    }
}