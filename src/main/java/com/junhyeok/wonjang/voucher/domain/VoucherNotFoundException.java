package com.junhyeok.wonjang.voucher.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.Map;

// 존재하지 않는 전표 id 로 역분개 등을 시도.
// 응답: 404 NOT_FOUND
public class VoucherNotFoundException extends DomainException {
    public VoucherNotFoundException(long voucherId) {
        super(
                VoucherErrorCode.VOUCHER_NOT_FOUND,
                "Voucher not found: id=" + voucherId,
                HttpStatus.NOT_FOUND,
                Map.of("voucherId", voucherId)
        );
    }
}
