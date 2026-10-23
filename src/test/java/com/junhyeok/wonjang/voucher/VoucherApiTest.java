package com.junhyeok.wonjang.voucher;

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

    // AC-04: 정상 전표 → 201 + voucher_no 포맷 + debit=credit
    @Test
    @DisplayName("ac04_정상전표_생성됨_201")
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

    // AC-05: 차변≠대변 → 422 UNBALANCED_VOUCHER
    @Test
    @DisplayName("ac05_불균형전표_422")
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

    // AC-06: amount <= 0 → 400 (Bean Validation @Positive)
    @Test
    @DisplayName("ac06_음수금액_거부")
    void ac06_nonPositiveAmount_rejected() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                LocalDate.of(2026, 10, 23),
                "0원",
                List.of(
                        new EntryLine("1101", EntrySide.DEBIT,  0L),       // 0 거부
                        new EntryLine("4001", EntrySide.CREDIT, 0L)
                )
        );

        ResponseEntity<String> res = postRaw(req);
        // GlobalExceptionHandler 가 MethodArgumentNotValidException → 422
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    // AC-07: 존재하지 않는 accountCode → 404 ACCOUNT_NOT_FOUND
    @Test
    @DisplayName("ac07_존재하지않는계정_404")
    void ac07_unknownAccountCode_rejectedWith404() {
        CreateVoucherRequest req = new CreateVoucherRequest(
                LocalDate.of(2026, 10, 23),
                "없는계정",
                List.of(
                        new EntryLine("9999", EntrySide.DEBIT,  10000L),   // 없는 code
                        new EntryLine("4001", EntrySide.CREDIT, 10000L)
                )
        );

        ResponseEntity<String> res = postRaw(req);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(res.getBody()).contains("ACCOUNT_NOT_FOUND");
    }

    // AC-08: 비활성 계정 → 422 ACCOUNT_INACTIVE
    @Test
    @DisplayName("ac08_비활성계정_422")
    void ac08_inactiveAccount_rejectedWith422() {
        // cash 비활성화
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

    // AC-09: 분개 1줄 → 400/422 (@Size(min=2))
    @Test
    @DisplayName("ac09_분개한줄_거부")
    void ac09_singleEntry_rejected() {
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
