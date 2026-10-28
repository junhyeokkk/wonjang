package com.junhyeok.wonjang.voucher;

import com.junhyeok.wonjang.AC;
import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.account.domain.AccountType;
import com.junhyeok.wonjang.voucher.application.CreateVoucherRequest;
import com.junhyeok.wonjang.voucher.application.EntryLine;
import com.junhyeok.wonjang.voucher.application.ReverseVoucherRequest;
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

    // --- F4 역분개 ---

    @Test
    @DisplayName(AC.AC_13)
    void ac13_reverse_createsFlippedVoucher_originalIntact() {
        VoucherResponse original = post(validRequest()).getBody();

        ResponseEntity<VoucherResponse> res = postReverse(original.id(), null);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        VoucherResponse rev = res.getBody();
        assertThat(rev.id()).isNotEqualTo(original.id());
        assertThat(rev.debitTotal()).isEqualTo(10_000L);
        assertThat(rev.creditTotal()).isEqualTo(10_000L);
        assertThat(rev.voucherNo()).matches("V-\\d{8}-\\d{6}");

        // 원본은 그대로 — entries 수 확인
        Integer originalEntryCount = jdbc.queryForObject(
                "SELECT COUNT(*) FROM entries WHERE voucher_id = ?", Integer.class, original.id());
        assertThat(originalEntryCount).isEqualTo(2);

        // 역분개 전표 side 가 반전됐는지
        List<String> flippedSides = jdbc.query(
                "SELECT side FROM entries WHERE voucher_id = ? ORDER BY line_no",
                (rs, i) -> rs.getString("side"), rev.id());
        // 원본은 DEBIT, CREDIT 순 → 역분개는 CREDIT, DEBIT 순
        assertThat(flippedSides).containsExactly("CREDIT", "DEBIT");
    }

    @Test
    @DisplayName(AC.AC_14)
    void ac14_reverse_pointsToOriginalId() {
        VoucherResponse original = post(validRequest()).getBody();
        VoucherResponse rev = postReverse(original.id(), null).getBody();

        Long reversesId = jdbc.queryForObject(
                "SELECT reverses_voucher_id FROM vouchers WHERE id = ?",
                Long.class, rev.id());
        assertThat(reversesId).isEqualTo(original.id());
    }

    @Test
    @DisplayName(AC.AC_15)
    void ac15_doubleReverse_rejectedWith409() {
        VoucherResponse original = post(validRequest()).getBody();
        postReverse(original.id(), null); // 1회차 성공

        ResponseEntity<String> res = postReverseRaw(original.id(), null); // 2회차 실패
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(res.getBody()).contains("ALREADY_REVERSED");
    }

    @Test
    @DisplayName(AC.AC_27)
    void ac27_reverseOfReversal_rejectedWith422() {
        VoucherResponse original = post(validRequest()).getBody();
        VoucherResponse reversal = postReverse(original.id(), null).getBody();

        ResponseEntity<String> res = postReverseRaw(reversal.id(), null);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(res.getBody()).contains("CANNOT_REVERSE_REVERSAL");
    }

    private CreateVoucherRequest validRequest() {
        return new CreateVoucherRequest(
                LocalDate.of(2026, 10, 23),
                "현금 매출",
                List.of(
                        new EntryLine("1101", EntrySide.DEBIT,  10_000L),
                        new EntryLine("4001", EntrySide.CREDIT, 10_000L)
                )
        );
    }

    private ResponseEntity<VoucherResponse> postReverse(Long id, ReverseVoucherRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "test-user");
        return restTemplate.exchange(
                "/api/vouchers/" + id + "/reverse",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                VoucherResponse.class
        );
    }

    private ResponseEntity<String> postReverseRaw(Long id, ReverseVoucherRequest req) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", "test-user");
        return restTemplate.exchange(
                "/api/vouchers/" + id + "/reverse",
                HttpMethod.POST,
                new HttpEntity<>(req, headers),
                String.class
        );
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
