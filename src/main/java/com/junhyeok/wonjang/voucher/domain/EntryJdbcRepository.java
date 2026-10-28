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

    // 역분개용 원본 entries 조회. line_no 순.
    public List<NewEntry> findByVoucherId(long voucherId) {
        String sql = """
                    SELECT account_id, side, amount, line_no
                    FROM entries
                    WHERE voucher_id = ?
                    ORDER BY line_no
                """;
        return jdbc.query(sql, (rs, i) -> new NewEntry(
                rs.getLong("account_id"),
                EntrySide.valueOf(rs.getString("side")),
                rs.getLong("amount"),
                rs.getShort("line_no")
        ), voucherId);
    }

    public record NewEntry(long accountId, EntrySide side, long amount, short lineNo) {}
}

