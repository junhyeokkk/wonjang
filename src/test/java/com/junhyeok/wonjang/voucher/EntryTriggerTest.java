package com.junhyeok.wonjang.voucher;

import com.junhyeok.wonjang.AC;
import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.account.domain.AccountType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 서비스 레이어를 '우회'해 DB 로 직접 들어갔을 때 트리거·제약이 거부하는지 검증.
// VoucherApiTest 는 HTTP 를 거쳐 서비스 사전 검증을 확인하는 반면,
// 여기서는 JdbcTemplate + TransactionTemplate 으로 트랜잭션을 수동 제어해
// commit 시점의 deferred constraint trigger / CHECK / FK 가 실제로 동작하는지 본다.
@SpringBootTest
@ActiveProfiles("test")
class EntryTriggerTest {

    @Autowired private JdbcTemplate jdbc;
    @Autowired private AccountRepository accountRepository;
    @Autowired private PlatformTransactionManager txManager;

    private TransactionTemplate tx;
    private Long cashAccountId;
    private Long salesAccountId;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(txManager);
        jdbc.execute("TRUNCATE entries, vouchers, accounts RESTART IDENTITY CASCADE");

        Account cash = accountRepository.save(new Account("1101", "현금", AccountType.ASSET));
        Account sales = accountRepository.save(new Account("4001", "매출", AccountType.REVENUE));
        cashAccountId = cash.getId();
        salesAccountId = sales.getId();
    }

    // AC-06: 서비스 사전 검증을 우회해도, voucher 단위 차변≠대변이면
    //        commit 시점(deferred) 트리거가 거부한다.
    // 흐름:
    //   1) tx 안에서 vouchers 1건 INSERT
    //   2) entries DEBIT 10000 + CREDIT 5000 두 줄 INSERT (불균형)
    //   3) commit 시도 → DataIntegrityViolationException + 메시지 'unbalanced'
    //   4) 밖에서 재조회 시 vouchers/entries 둘 다 0건 (전체 롤백)
    @Test
    @DisplayName(AC.AC_06)
    void ac06_unbalancedDirectInsert_rejectedByDeferredTrigger() {
        // commit 시점 constraint trigger 라 Spring 이 TransactionSystemException 으로 랩핑한다.
        // 타입은 묶지 않고, cause 체인 어딘가에 트리거 메시지가 있는지만 본다.
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                long voucherId = insertVoucher("V-20261023-000001", LocalDate.of(2026, 10, 23));
                insertEntry(voucherId, cashAccountId, "DEBIT", 10_000L, (short) 1);
                insertEntry(voucherId, salesAccountId, "CREDIT", 5_000L, (short) 2);
            }))
            .hasStackTraceContaining("unbalanced");

        assertThat(countVouchers()).isZero();
        assertThat(countEntries()).isZero();
    }

    // AC-07: 같은 트랜잭션 안에서 entries 중 하나가 실패하면 전표 전체가 사라진다.
    // 흐름:
    //   1) tx 안에서 vouchers 1건 INSERT
    //   2) entries 1줄 정상 INSERT (DEBIT 10000)
    //   3) entries 2줄째를 FK 위반(없는 account_id=99999) 로 INSERT
    //      → 즉시 DataIntegrityViolationException
    //   4) 트랜잭션 롤백 후 조회 → vouchers/entries 둘 다 0건
    @Test
    @DisplayName(AC.AC_07)
    void ac07_partialFailure_rollsBackWholeVoucher() {
        assertThatThrownBy(() -> tx.executeWithoutResult(status -> {
                long voucherId = insertVoucher("V-20261023-000002", LocalDate.of(2026, 10, 23));
                insertEntry(voucherId, cashAccountId, "DEBIT", 10_000L, (short) 1);
                insertEntry(voucherId, 99_999L, "CREDIT", 10_000L, (short) 2);
            }))
            .isInstanceOf(DataIntegrityViolationException.class);

        assertThat(countVouchers()).isZero();
        assertThat(countEntries()).isZero();
    }

    // AC-16: entries 에 UPDATE/DELETE 직접 실행 → BEFORE 트리거(trg_entries_immutable)가 거부.
    @Test
    @DisplayName(AC.AC_16)
    void ac16_entriesUpdateOrDelete_rejectedByAppendOnlyTrigger() {
        // 1) 균형 전표 정상 커밋 → UPDATE/DELETE 대상 데이터 존재
        tx.executeWithoutResult(status -> {
            long voucherId = insertVoucher("V-20261023-000010", LocalDate.of(2026, 10, 23));
            insertEntry(voucherId, cashAccountId,  "DEBIT",  10_000L, (short) 1);
            insertEntry(voucherId, salesAccountId, "CREDIT", 10_000L, (short) 2);
        });

        // 2) UPDATE 시도 → BEFORE 트리거가 즉시 거부
        assertThatThrownBy(() ->
                jdbc.update("UPDATE entries SET amount = 1 WHERE line_no = 1"))
                .hasStackTraceContaining("append-only");

        // 3) DELETE 시도 → 같은 트리거가 거부
        assertThatThrownBy(() ->
                jdbc.update("DELETE FROM entries WHERE line_no = 1"))
                .hasStackTraceContaining("append-only");

        // 4) 데이터 그대로
        assertThat(countEntries()).isEqualTo(2);
    }

    // --- helpers ---

    // vouchers INSERT, 생성된 id 반환.
    // created_by 는 NOT NULL 이라 'trigger-test' 같은 값 하나 박아둔다.
    private long insertVoucher(String voucherNo, LocalDate date) {
        return jdbc.queryForObject(
            "INSERT INTO vouchers(voucher_no, voucher_date, description, created_by) " +
            "VALUES (?, ?, ?, ?) RETURNING id",
            Long.class, voucherNo, date, "trigger-test", "test");
    }

    // entries 한 줄 INSERT. side 는 'DEBIT'/'CREDIT' 문자열로 직접 넘긴다.
    private void insertEntry(long voucherId, long accountId, String side, long amount, short lineNo) {
        jdbc.update(
            "INSERT INTO entries(voucher_id, account_id, side, amount, line_no) VALUES (?, ?, ?, ?, ?)",
            voucherId, accountId, side, amount, lineNo);
    }

    private int countVouchers() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM vouchers", Integer.class);
    }

    private int countEntries() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM entries", Integer.class);
    }
}
