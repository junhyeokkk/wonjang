package com.junhyeok.wonjang.voucher.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.Map;

// 이미 역분개된 전표를 다시 역분개 시도 (AC-15). 응답: 409 CONFLICT
public class AlreadyReversedException extends DomainException {
    public AlreadyReversedException(long originalVoucherId, long reversalVoucherId) {
        super(
                VoucherErrorCode.ALREADY_REVERSED,
                "Voucher already reversed: id=" + originalVoucherId + ", reversalId=" + reversalVoucherId,
                HttpStatus.CONFLICT,
                Map.of(
                        "voucherId", originalVoucherId,
                        "reversalVoucherId", reversalVoucherId
                )
        );
    }
}
