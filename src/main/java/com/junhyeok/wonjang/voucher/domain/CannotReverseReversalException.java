package com.junhyeok.wonjang.voucher.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.Map;

// 역분개 전표를 다시 역분개 시도 (AC-27). 응답: 422 UNPROCESSABLE_ENTITY
public class CannotReverseReversalException extends DomainException {
    public CannotReverseReversalException(long voucherId, long originalVoucherId) {
        super(
                VoucherErrorCode.CANNOT_REVERSE_REVERSAL,
                "Cannot reverse a reversal voucher: id=" + voucherId + ", reverses=" + originalVoucherId,
                HttpStatus.UNPROCESSABLE_ENTITY,
                Map.of(
                        "voucherId", voucherId,
                        "reversesVoucherId", originalVoucherId
                )
        );
    }
}
