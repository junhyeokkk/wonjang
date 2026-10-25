package com.junhyeok.wonjang.ledger.application;

import com.junhyeok.wonjang.account.domain.Account;

import java.time.LocalDate;

// GET /api/accounts/{id}/balance?asOf= 응답
// asOf 날짜까지의 누적 잔액 (계정 유형별 부호 적용)
public record BalanceResponse(
        String code,
        String name,
        String type,
        LocalDate asOf,
        long balance
) {
    public static BalanceResponse of(Account account, LocalDate asOf, long balance){
        return new BalanceResponse(
                account.getCode(),
                account.getName(),
                account.getType().name(),
                asOf,
                balance
        );
    }
}
