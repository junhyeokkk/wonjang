package com.junhyeok.wonjang.account.domain;

public record AccountResponse(Long id, String code, String name, String type, boolean active) {

    public static AccountResponse from(Account account) {
        return new AccountResponse(
                account.getId(),
                account.getCode(),
                account.getName(),
                account.getType().name(), // enum -> String
                account.isActive()
        );
    }
}
