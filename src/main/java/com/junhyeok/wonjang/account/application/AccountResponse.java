package com.junhyeok.wonjang.account.application;

import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountType;
import java.time.OffsetDateTime;

// Account 반환 타입
public record AccountResponse(
        Long id,
        String code,
        String name,
        AccountType type,
        boolean active,
        OffsetDateTime createdAt
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCode(),
                account.getName(),
                account.getType(),
                account.isActive(),
                account.getCreatedAt()
        );
    }
}