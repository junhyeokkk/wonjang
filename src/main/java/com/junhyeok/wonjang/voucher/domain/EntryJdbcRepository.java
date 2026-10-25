package com.junhyeok.wonjang.voucher.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class EntryJdbcRepository {

    private final JdbcTemplate jdbc;

    public EntryJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertAll(long voucherId, List<NewEntry> lines){
        String sql =  """
                    INSERT INTO entries (voucher_id, account_id, side, amount, line_no)
                    VALUES (?, ?, ?, ?, ?)
                """;
        jdbc.batchUpdate(sql, lines, lines.size(), (ps, line) -> {
            ps.setLong(1, voucherId);
            ps.setLong(2, line.accountId());
            ps.setString(3, line.side().name());
            ps.setLong(4, line.amount());
            ps.setShort(5, line.lineNo());
        });
    }

    public record NewEntry(long accountId, EntrySide side, long amount, short lineNo) {}
}

