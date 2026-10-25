package com.junhyeok.wonjang.voucher.domain;

import com.junhyeok.wonjang.common.exception.DomainException;
import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * 비활성(deactivated) 계정에 분개를 기록하려 할 때 던진다.
 * 응답: 422 UNPROCESSABLE_ENTITY - detail: { accountCode }.
 */
public class AccountInactiveException extends DomainException {
    public AccountInactiveException(String accountCode) {
        super(
                VoucherErrorCode.ACCOUNT_INACTIVE,
                "Account is inactive: code=" + accountCode,
                HttpStatus.UNPROCESSABLE_ENTITY,
                Map.of("accountCode", accountCode)
        );
    }
}

