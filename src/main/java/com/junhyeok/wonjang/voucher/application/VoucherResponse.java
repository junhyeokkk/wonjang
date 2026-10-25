package com.junhyeok.wonjang.voucher.application;

import com.junhyeok.wonjang.voucher.domain.Voucher;
import java.time.LocalDate;

public record VoucherResponse(
        Long id,
        String voucherNo,
        LocalDate voucherDate,
        long debitTotal,
        long creditTotal
) {
    public static VoucherResponse from(Voucher v, String voucherNo, long debitTotal, long creditTotal){
        return new VoucherResponse(
                v.getId(),
                voucherNo,
                v.getVoucherDate(),
                debitTotal,
                creditTotal
        );
    }
}

