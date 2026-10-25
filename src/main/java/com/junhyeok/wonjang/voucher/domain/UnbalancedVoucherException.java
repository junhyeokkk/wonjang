package com.junhyeok.wonjang.voucher.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 전표의 차변 합과 대변 합이 다를 때 던진다.
 * 응답: 422 UNPROCESSABLE_ENTITY - detail: { debitTotal, creditTotal, diff }
 */
public class UnbalancedVoucherException extends DomainException {
    public UnbalancedVoucherException(long debitTotal, long creditTotal) {
        super(
                VoucherErrorCode.UNBALANCED_VOUCHER,
                "Debit total must equal credit total: debit=" + debitTotal + ", credit=" + creditTotal,
                HttpStatus.UNPROCESSABLE_ENTITY,
                Map.of(
                        "debitTotal", debitTotal,
                        "creditTotal", creditTotal,
                        "diff", debitTotal - creditTotal
                )
        );
    }
}

