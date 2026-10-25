package com.junhyeok.wonjang.ledger.application;

import java.time.LocalDate;

// 원장 한 줄 — 전표 하나당 한 분개(해당 계정)
// debit/credit 중 한쪽은 0, balance 는 그 줄까지 누적 잔액
public record LedgerLine(
        LocalDate voucherDate,
        String voucherNo,
        String description,
        long debit,
        long credit,
        long balance
) {
}
