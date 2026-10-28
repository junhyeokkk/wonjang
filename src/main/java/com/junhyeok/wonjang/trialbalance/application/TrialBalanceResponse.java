package com.junhyeok.wonjang.trialbalance.application;

import java.time.LocalDate;
import java.util.List;

// GET /api/trial-balance?asOf=yyyy-MM-dd 응답.
// rows: 계정별 차변합·대변합·잔액. balanced: 총합 일치 여부 (AC-18).
public record TrialBalanceResponse(
        LocalDate asOf,
        List<Row> rows,
        long debitGrandTotal,
        long creditGrandTotal,
        boolean balanced,
        long diff
) {
    // balance: 계정 유형별 정부호 잔액 (ASSET/EXPENSE 는 debit-credit, 나머지는 credit-debit).
    public record Row(
            String accountCode,
            String accountName,
            String accountType,
            long debitTotal,
            long creditTotal,
            long balance
    ) {}
}
