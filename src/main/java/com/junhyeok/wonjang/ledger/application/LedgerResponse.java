package com.junhyeok.wonjang.ledger.application;

import com.junhyeok.wonjang.account.domain.Account;

import java.util.List;

// GET /api/accounts/{id}/ledger?from=&to= 응답
// Notion 04 API 명세:
// {
//   "account": { "code", "name", "type" },
//   "openingBalance": from 이전까지 누적 (AC-11),
//   "lines": [ LedgerLine ... ],
//   "closingBalance": lines 마지막 balance (또는 opening + 기간내 net)
// }
public record LedgerResponse(
        AccountSummary account,
        long openingBalance,
        List<LedgerLine> lines,
        long closingBalance
) {

    public static LedgerResponse of(Account acc, long opening, List<LedgerLine> lines, long closing) {
        return new LedgerResponse(
                AccountSummary.from(acc),
                opening,
                lines,
                closing
        );
    }

    // 응답 account 부분 - Account 엔티티에서 노출할 3필드만 추출
    public record AccountSummary(String code, String name, String type) {
        // Account -> AccountSummary 변환
        public static AccountSummary from(Account account) {
            return new AccountSummary(
                    account.getCode(),
                    account.getName(),
                    account.getType().name() // enum -> String
            );
        }
    }
}
