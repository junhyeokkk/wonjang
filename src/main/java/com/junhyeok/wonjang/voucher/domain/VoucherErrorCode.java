package com.junhyeok.wonjang.voucher.domain;

import com.junhyeok.wonjang.common.exception.ErrorCode;

public enum VoucherErrorCode implements ErrorCode {
    UNBALANCED_VOUCHER,         // 차변 != 대변
    INVALID_AMOUNT,             // amount <= 0  or JSON 소수 입력
    ACCOUNT_INACTIVE,           // 비활성 계정에 분개 시도
    VOUCHER_NOT_FOUND,          // 존재하지 않는 전표 id (F4 역분개 대상)
    ALREADY_REVERSED,           // 이미 역분개된 전표를 또 역분개 (AC-15)
    CANNOT_REVERSE_REVERSAL     // 역분개 전표를 다시 역분개 (AC-27)
    ;
    @Override public String code() { return name(); }
}

