package com.junhyeok.wonjang.voucher.domain;

import com.junhyeok.wonjang.common.exception.ErrorCode;

public enum VoucherErrorCode implements ErrorCode {
    UNBALANCED_VOUCHER, // 차변 != 대변
    INVALID_AMOUNT,     // amount <= 0  or JSON 소수 입력
    ACCOUNT_INACTIVE    // 비활성 계정에 분개 시도
    ;
    @Override public String code() { return name(); }
}

