package com.junhyeok.wonjang.trialbalance;

import com.junhyeok.wonjang.AC;
import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.account.domain.AccountType;
import com.junhyeok.wonjang.trialbalance.application.TrialBalanceResponse;
import com.junhyeok.wonjang.voucher.application.CreateVoucherRequest;
import com.junhyeok.wonjang.voucher.application.EntryLine;
import com.junhyeok.wonjang.voucher.application.VoucherResponse;
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
class TrialBalanceApiTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private AccountRepository accountRepository;
    @Autowired private JdbcTemplate jdbc;

    @BeforeEach
    void setUp() {
        jdbc.execute("TRUNCATE entries, vouchers, accounts RESTART IDENTITY CASCADE");
        accountRepository.save(new Account("1101", "현금", AccountType.ASSET));
        accountRepository.save(new Account("4001", "매출", AccountType.REVENUE));
    }

    @Test
    @DisplayName(AC.AC_17)
    void ac17_asOf_returnsAllAccounts_withGrandTotals() {
        createVoucher(LocalDate.of(2026, 10, 23), 10_000L);

        ResponseEntity<TrialBalanceResponse> res = get("2026-10-23");

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        TrialBalanceResponse body = res.getBody();
        assertThat(body.rows()).hasSize(2);
        assertThat(body.debitGrandTotal()).isEqualTo(10_000L);
        assertThat(body.creditGrandTotal()).isEqualTo(10_000L);
        assertThat(body.balanced()).isTrue();

        TrialBalanceResponse.Row cash = body.rows().stream()
                .filter(r -> r.accountCode().equals("1101")).findFirst().orElseThrow();
        assertThat(cash.debitTotal()).isEqualTo(10_000L);
        assertThat(cash.creditTotal()).isZero();
        assertThat(cash.balance()).isEqualTo(10_000L); // ASSET: debit-credit
        assertThat(cash.accountType()).isEqualTo("ASSET");

        TrialBalanceResponse.Row sales = body.rows().stream()
                .filter(r -> r.accountCode().equals("4001")).findFirst().orElseThrow();
        assertThat(sales.debitTotal()).isZero();
        assertThat(sales.creditTotal()).isEqualTo(10_000L);
        assertThat(sales.balance()).isEqualTo(10_000L); // REVENUE: credit-debit
        assertThat(sales.accountType()).isEqualTo("REVENUE");
    }

    // AC-18: 정상 경로에서는 항상 balanced=true. 일부러 깨뜨렸을 때 신호가 뜨는지만 본다.
    @Test
    @DisplayName(AC.AC_18)
    void ac18_unbalanced_whenEntriesForcedOutOfBalance() {
        createVoucher(LocalDate.of(2026, 10, 23), 10_000L);

        // trg_entries_immutable 가 UPDATE 를 막으므로 테스트 중만 트리거 비활성화.
        jdbc.execute("ALTER TABLE entries DISABLE TRIGGER trg_entries_immutable");
        try {
            jdbc.update("UPDATE entries SET amount = amount - 1 WHERE side = 'CREDIT'");
        } finally {
            jdbc.execute("ALTER TABLE entries ENABLE TRIGGER trg_entries_immutable");
        }

        TrialBalanceResponse body = get("2026-10-23").getBody();
        assertThat(body.balanced()).isFalse();
        assertThat(body.diff()).isEqualTo(1L); // debit - credit = +1
    }

    private void createVoucher(LocalDate date, long amount) {
        CreateVoucherRequest req = new CreateVoucherRequest(
                date, "test",
                List.of(
                        new EntryLine("1101", EntrySide.DEBIT,  amount),
                        new EntryLine("4001", EntrySide.CREDIT, amount)
                )
        );
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "test-user");
        ResponseEntity<VoucherResponse> res = restTemplate.exchange(
                "/api/vouchers", HttpMethod.POST,
                new HttpEntity<>(req, headers), VoucherResponse.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    private ResponseEntity<TrialBalanceResponse> get(String asOf) {
        return restTemplate.getForEntity(
                "/api/trial-balance?asOf=" + asOf, TrialBalanceResponse.class);
    }
}
