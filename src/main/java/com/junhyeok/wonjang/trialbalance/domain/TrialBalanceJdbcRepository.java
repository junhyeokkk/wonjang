package com.junhyeok.wonjang.trialbalance.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

// asOf 기준 전 계정의 차변합·대변합. 분개 없는 계정도 (0,0) 으로 나오게 LEFT JOIN.
@Repository
public class TrialBalanceJdbcRepository {

    private final JdbcTemplate jdbc;

    public TrialBalanceJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Row> findSumsAsOf(LocalDate asOf) {
        String sql = """
                SELECT a.code AS account_code,
                       a.name AS account_name,
                       a.type AS account_type,
                       COALESCE(SUM(CASE WHEN e.side='DEBIT'  THEN e.amount ELSE 0 END), 0) AS debit_total,
                       COALESCE(SUM(CASE WHEN e.side='CREDIT' THEN e.amount ELSE 0 END), 0) AS credit_total
                FROM accounts a
                LEFT JOIN entries e ON e.account_id = a.id
                LEFT JOIN vouchers v ON v.id = e.voucher_id
                WHERE (CAST(? AS DATE) IS NULL OR v.voucher_date <= ? OR v.id IS NULL)
                GROUP BY a.code, a.name, a.type
                ORDER BY a.code
                """;
        return jdbc.query(sql, (rs, i) -> new Row(
                rs.getString("account_code"),
                rs.getString("account_name"),
                rs.getString("account_type"),
                rs.getLong("debit_total"),
                rs.getLong("credit_total")
        ), asOf, asOf);
    }

    public record Row(String accountCode, String accountName, String accountType,
                      long debitTotal, long creditTotal) {}
}
