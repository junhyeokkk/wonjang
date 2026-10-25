package com.junhyeok.wonjang.voucher;

import com.junhyeok.wonjang.AC;
import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.account.domain.AccountType;
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
class VoucherApiTest {

    @Autowired private TestRestTemplate restTemplate;
    @Autowired private AccountRepository accountRepository;
    @Autowired private JdbcTemplate jdbc;

    private Long cashAccountId;      // 현금 1101 (ASSET)
    private Long salesAccountId;     // 매출 4001 (REVENUE)

    @BeforeEach
    void setUp() {
        // 세 테이블 모두 cleanup — FK 때문에 CASCADE 필요
        jdbc.execute("TRUNCATE entries, vouchers, accounts RESTART IDENTITY CASCADE");

        // 테스트용 Account 2개 저장
        Account cash  = accountRepository.save(new Account("1101", "현금", AccountType.ASSET));
        Account sales = accountRepository.save(new Account("4001", "매출", AccountType.REVENUE));
        cashAccountId = cash.getId();
        salesAccountId = sales.getId();
    }

    @Test
    @DisplayName(AC.AC_04)
    void ac04_validVoucher_createdWith201() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                LocalDate.of(2026, 10, 23),
                "현금 매출",
                List.of(
                        new EntryLine("1101", EntrySide.DEBIT,  10000L),
                        new EntryLine("4001", EntrySide.CREDIT, 10000L)
                )
        );

        ResponseEntity<VoucherResponse> res = post(req);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody().voucherNo()).matches("V-20261023-\\d{6}");
        assertThat(res.getBody().debitTotal()).isEqualTo(10000L);
        assertThat(res.getBody().creditTotal()).isEqualTo(10000L);
    }

    @Test
    @DisplayName(AC.AC_05)
    void ac05_unbalancedVoucher_rejectedWith422() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                LocalDate.of(2026, 10, 23),
                "불균형",
                List.of(
                        new EntryLine("1101", EntrySide.DEBIT,  10000L),
                        new EntryLine("4001", EntrySide.CREDIT,  9000L)   // 1000 모자람
                )
        );

        ResponseEntity<String> res = postRaw(req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).contains("UNBALANCED_VOUCHER");
    }

    @Test
    @DisplayName(AC.AC_08)
    void ac08_nonPositiveAmount_rejected() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                LocalDate.of(2026, 10, 23),
                "0원",
                List.of(
                        new EntryLine("1101", EntrySide.DEBIT,  0L),
                        new EntryLine("4001", EntrySide.CREDIT, 0L)
                )
        );

        ResponseEntity<String> res = postRaw(req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    @DisplayName(AC.AC_09)
    void ac09_inactiveAccount_rejectedWith422() {
        Account cash = accountRepository.findById(cashAccountId).orElseThrow();
        cash.deactivate();
        accountRepository.save(cash);

        CreateVoucherRequest req = new CreateVoucherRequest(
                LocalDate.of(2026, 10, 23),
                "비활성",
                List.of(
                        new EntryLine("1101", EntrySide.DEBIT,  10000L),
                        new EntryLine("4001", EntrySide.CREDIT, 10000L)
                )
        );

        ResponseEntity<String> res = postRaw(req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).contains("ACCOUNT_INACTIVE");
    }

    // AC 번호 없음 — 없는 계정 code 는 404 로 매핑 (공통 리소스 조회 실패)
    @Test
    @DisplayName("validation_unknownAccountCode_rejectedWith404")
    void validation_unknownAccountCode_rejectedWith404() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                LocalDate.of(2026, 10, 23),
                "없는계정",
                List.of(
                        new EntryLine("9999", EntrySide.DEBIT,  10000L),
                        new EntryLine("4001", EntrySide.CREDIT, 10000L)
                )
        );

        ResponseEntity<String> res = postRaw(req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).contains("ACCOUNT_NOT_FOUND");
    }

    // AC 번호 없음 — AC-04 의 "2줄+" 경계 (@Size(min=2) Bean Validation)
    @Test
    @DisplayName("validation_singleEntry_rejected")
    void validation_singleEntry_rejected() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                LocalDate.of(2026, 10, 23),
                "한줄",
                List.of(new EntryLine("1101", EntrySide.DEBIT, 10000L))
        );

        ResponseEntity<String> res = postRaw(req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // --- helpers ---

    private ResponseEntity<VoucherResponse> post(CreateVoucherRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "test-user");
        return restTemplate.exchange(
                "/api/vouchers",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                VoucherResponse.class
        );
    }

    private ResponseEntity<String> postRaw(CreateVoucherRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "test-user");
        return restTemplate.exchange(
                "/api/vouchers",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                String.class
        );
    }
}
