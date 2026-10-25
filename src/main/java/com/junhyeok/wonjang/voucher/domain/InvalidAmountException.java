package com.junhyeok.wonjang.voucher.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

/**
 * 분개의 금액이 0 이하일 때 던진다.
 * 응답: 422 UNPROCESSABLE_ENTITY - detail 없음
 */
public class InvalidAmountException extends DomainException {
    public InvalidAmountException(long amount) {

        super(
                VoucherErrorCode.INVALID_AMOUNT,
                "Amount must be positive: amount=" + amount,
                HttpStatus.UNPROCESSABLE_ENTITY
        );
    }
}

