package com.junhyeok.wonjang.ledger.domain;

import com.junhyeok.wonjang.ledger.application.LedgerLine;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// 조회 전용 Repository
// 왜 JdbcTemplate? ADR / F2 교훈:
//   - 쓰기(entries INSERT)는 trigger 타이밍 때문에 JdbcTemplate 강제였고,
//   - 조회는 JPA OK 지만 "SUM() OVER (ORDER BY ...)" 윈도우 함수 + 결과 매핑은
//     raw SQL + RowMapper 가 훨씬 간결.
@Repository
public class LedgerJdbcRepository {

    private final JdbcTemplate jdbc;

    public LedgerJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // from 직전까지 쌓인 (차변 합 - 대변 합) 부호 보정은 서비스에서
    // from == null 이면 기초 잔액 개념 없음 → 0
    public long sumDebitMinusCreditBefore(long accountId, LocalDate from) {
        if(from == null) return 0L;

        String sql = """
                SELECT COALESCE(SUM(CASE WHEN e.side='DEBIT' THEN e.amount ELSE - e.amount END),0)
                FROM entries e 
                JOIN vouchers v ON v.id = e.voucher_id
                WHERE e.account_id = ? AND v.voucher_date < ?
                """;
        Long result = jdbc.queryForObject(sql, Long.class, accountId, from);

        return result != null ? result : 0L;
    }

    // 기간 내 원장 줄 목록 (전표일↑, 같은 날이면 entry.id↑)
    // 각 줄 balance = opening + sign * (그 줄까지의 차변-대변 누적)
    // from, to 는 선택 (null 이면 해당 방향 제한 없음)
    public List<LedgerLine> findLines(long accountId, LocalDate from, LocalDate to,
                                      long openingBalance, int signMultiplier) {

        String sql = """
           SELECT
             v.voucher_date,
             v.voucher_no,
             v.description,
             CASE WHEN e.side='DEBIT'  THEN e.amount ELSE 0 END AS debit,
             CASE WHEN e.side='CREDIT' THEN e.amount ELSE 0 END AS credit,
             SUM(CASE WHEN e.side='DEBIT' THEN e.amount ELSE -e.amount END)
               OVER (ORDER BY v.voucher_date, e.id
                     ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) AS running_raw
           FROM entries e
           JOIN vouchers v ON v.id = e.voucher_id
           WHERE e.account_id = ?
             AND (CAST(? AS DATE) IS NULL OR v.voucher_date >= ?)
             AND (CAST(? AS DATE) IS NULL OR v.voucher_date <= ?)
           ORDER BY v.voucher_date, e.id
           """;

        return jdbc.query(
                sql,
                (rs, rowNum) -> {
                    LocalDate voucherDate = rs.getObject("voucher_date", LocalDate.class);
                    String voucherNo      = rs.getString("voucher_no");
                    String description    = rs.getString("description");
                    long debit            = rs.getLong("debit");
                    long credit           = rs.getLong("credit");
                    long runningRaw       = rs.getLong("running_raw");

                    long balance = openingBalance + signMultiplier * runningRaw;

                    return new LedgerLine(voucherDate, voucherNo, description, debit, credit, balance);
                },
                accountId, from, from, to, to
        );
    }

    // asOf 까지 쌓인 (차변 합 - 대변 합) 부호 보정은 서비스에서
    // asOf == null 이면 전 기간 합산
    public long sumDebitMinusCreditAsOf(long accountId, LocalDate asOf) {

        String sql = """
           SELECT COALESCE(SUM(CASE WHEN e.side='DEBIT' THEN e.amount ELSE -e.amount END), 0)
           FROM entries e JOIN vouchers v ON v.id = e.voucher_id
           WHERE e.account_id = ?
           AND (CAST(? AS DATE) IS NULL OR v.voucher_date <= ?)
           """;
        Long result = jdbc.queryForObject(sql, Long.class, accountId, asOf, asOf);

        return result != null ? result : 0L;
    }
}
