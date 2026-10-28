package com.junhyeok.wonjang.ledger;

import com.junhyeok.wonjang.AC;
import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.account.domain.AccountType;
import com.junhyeok.wonjang.ledger.application.BalanceResponse;
import com.junhyeok.wonjang.ledger.application.LedgerResponse;
import com.junhyeok.wonjang.voucher.application.CreateVoucherRequest;
import com.junhyeok.wonjang.voucher.application.EntryLine;
import com.junhyeok.wonjang.voucher.domain.EntrySide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class LedgerApiTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private AccountRepository accountRepository;
    @Autowired private JdbcTemplate jdbc;

    private Long cashAccountId;    // 1101 ASSET
    private Long salesAccountId;   // 4001 REVENUE

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE entries, vouchers, accounts RESTART IDENTITY CASCADE");

        Account cash  = accountRepository.save(new Account("1101", "현금", AccountType.ASSET));
        Account sales = accountRepository.save(new Account("4001", "매출", AccountType.REVENUE));
        cashAccountId = cash.getId();
        salesAccountId = sales.getId();
    }

    @Test
    @DisplayName(AC.AC_10)
    void ac10_ledger_orderedByDateThenEntryId_withRunningBalance() {
        // V1 2026-10-10 debit 10000
        postVoucher(LocalDate.of(2026, 10, 10), "V1", 10000L);
        // V2 2026-10-20 debit 20000 (더 뒤 날짜, 생성은 두 번째)
        postVoucher(LocalDate.of(2026, 10, 20), "V2", 20000L);
        // V3 2026-10-10 debit  5000 (V1 과 같은 날, 생성은 세 번째)
        postVoucher(LocalDate.of(2026, 10, 10), "V3", 5000L);

        ResponseEntity<LedgerResponse> res = restTemplate.getForEntity(
                "/api/accounts/{id}/ledger", LedgerResponse.class, cashAccountId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        var lines = res.getBody().lines();
        assertThat(lines).hasSize(3);

        // 전표일↑, 같은 날짜면 생성순(entry.id)↑ → V1, V3, V2
        assertThat(lines.get(0).description()).isEqualTo("V1");
        assertThat(lines.get(1).description()).isEqualTo("V3");
        assertThat(lines.get(2).description()).isEqualTo("V2");

        // ASSET 누적 잔액 (DEBIT +)
        assertThat(lines.get(0).balance()).isEqualTo(10000L);
        assertThat(lines.get(1).balance()).isEqualTo(15000L);
        assertThat(lines.get(2).balance()).isEqualTo(35000L);

        assertThat(res.getBody().closingBalance()).isEqualTo(35000L);
    }

    @Test
    @DisplayName(AC.AC_11)
    void ac11_periodFilter_includesOpeningBalance() {
        // from 이전 분개 — 10월 거래
        postVoucher(LocalDate.of(2026, 10, 15), "pre", 10000L);
        // from 이후 분개 — 11월 거래
        postVoucher(LocalDate.of(2026, 11, 5),  "in",  20000L);

        ResponseEntity<LedgerResponse> res = restTemplate.getForEntity(
                "/api/accounts/{id}/ledger?from=2026-11-01&to=2026-11-30",
                LedgerResponse.class, cashAccountId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().openingBalance()).isEqualTo(10000L);

        // 기간 내 분개는 1개, 그 줄의 balance 는 opening + 20000
        var lines = res.getBody().lines();
        assertThat(lines).hasSize(1);
        assertThat(lines.get(0).description()).isEqualTo("in");
        assertThat(lines.get(0).balance()).isEqualTo(30000L);
        assertThat(res.getBody().closingBalance()).isEqualTo(30000L);
    }

    @Test
    @DisplayName(AC.AC_12)
    void ac12_balanceSignByAccountType() {
        // 현금 매출 10000 — 현금(ASSET) DEBIT, 매출(REVENUE) CREDIT
        postVoucher(LocalDate.of(2026, 10, 10), "매출", 10000L);

        // ASSET: DEBIT 로 쌓이면 잔액 +
        ResponseEntity<BalanceResponse> cashRes = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, cashAccountId);
        assertThat(cashRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(cashRes.getBody().balance()).isEqualTo(10000L);

        // REVENUE: CREDIT 로 쌓이면 잔액 + (부호 반전)
        ResponseEntity<BalanceResponse> salesRes = restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, salesAccountId);
        assertThat(salesRes.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(salesRes.getBody().balance()).isEqualTo(10000L);
    }

    // AC-12 확장: LIABILITY/EQUITY/EXPENSE 도 부호 방향이 올바른지 검증.
    //   자본투입 : CASH DEBIT 100,000 / CAPITAL CREDIT 100,000
    //   차입    : CASH DEBIT  50,000 / LOAN    CREDIT  50,000
    //   임차료  : RENT DEBIT  30,000 / CASH    CREDIT  30,000
    // 기대 잔액:
    //   CASH    (ASSET)      debit-credit = 150,000 - 30,000 = 120,000
    //   LOAN    (LIABILITY)  credit-debit =  50,000 -      0 =  50,000
    //   CAPITAL (EQUITY)     credit-debit = 100,000 -      0 = 100,000
    //   RENT    (EXPENSE)    debit-credit =  30,000 -      0 =  30,000
    @Test
    @DisplayName(AC.AC_12 + " — LIABILITY/EQUITY/EXPENSE 커버")
    void ac12_balanceSign_allAccountTypes() {
        Long loanId    = accountRepository.save(new Account("2101", "차입금", AccountType.LIABILITY)).getId();
        Long capitalId = accountRepository.save(new Account("3101", "자본금", AccountType.EQUITY)).getId();
        Long rentId    = accountRepository.save(new Account("5101", "임차료", AccountType.EXPENSE)).getId();

        postPair(LocalDate.of(2026, 10, 10), "자본투입", "1101", "3101", 100_000L);
        postPair(LocalDate.of(2026, 10, 11), "차입",     "1101", "2101",  50_000L);
        postPair(LocalDate.of(2026, 10, 12), "임차료",   "5101", "1101",  30_000L);

        assertThat(balanceOf(cashAccountId)).isEqualTo(120_000L);
        assertThat(balanceOf(loanId)).isEqualTo(50_000L);
        assertThat(balanceOf(capitalId)).isEqualTo(100_000L);
        assertThat(balanceOf(rentId)).isEqualTo(30_000L);
    }

    private long balanceOf(long accountId) {
        return restTemplate.getForEntity(
                "/api/accounts/{id}/balance", BalanceResponse.class, accountId)
                .getBody().balance();
    }

    // DEBIT·CREDIT 쪽 계정 코드와 금액을 받아 균형 전표 하나 생성.
    private void postPair(LocalDate date, String desc, String debitCode, String creditCode, long amount) {
        CreateVoucherRequest req = new CreateVoucherRequest(
                date, desc,
                List.of(
                        new EntryLine(debitCode,  EntrySide.DEBIT,  amount),
                        new EntryLine(creditCode, EntrySide.CREDIT, amount)
                )
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "test-user");
        ResponseEntity<Void> res = restTemplate.exchange(
                "/api/vouchers", HttpMethod.POST,
                new HttpEntity<>(req, headers), Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // 현금 debit / 매출 credit 균형 전표 하나 생성
    private void postVoucher(LocalDate date, String desc, long amount) {
        CreateVoucherRequest req = new CreateVoucherRequest(
                date, desc,
                List.of(
                        new EntryLine("1101", EntrySide.DEBIT,  amount),
                        new EntryLine("4001", EntrySide.CREDIT, amount)
                )
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "test-user");
        ResponseEntity<Void> res = restTemplate.exchange(
                "/api/vouchers", HttpMethod.POST,
                new HttpEntity<>(req, headers), Void.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
