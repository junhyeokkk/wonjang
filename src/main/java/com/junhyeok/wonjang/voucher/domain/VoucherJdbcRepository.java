package com.junhyeok.wonjang.voucher.domain;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class VoucherJdbcRepository {

    private final JdbcTemplate jdbc;

    public VoucherJdbcRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void updateVoucherNo(long id, String voucherNo) {
        jdbc.update("UPDATE vouchers SET voucher_no = ? WHERE id = ?", voucherNo, id);
    }
}
