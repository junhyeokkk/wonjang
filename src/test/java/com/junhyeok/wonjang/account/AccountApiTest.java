package com.junhyeok.wonjang.account;

import com.junhyeok.wonjang.AC;
import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.account.domain.AccountType;
import com.junhyeok.wonjang.account.application.AccountResponse;
import com.junhyeok.wonjang.account.application.CreateAccountRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpMethod.PATCH;

// @SpringBootTest: 전체 Spring 컨텍스트 로드 (실제 서버 기동)
// RANDOM_PORT: 랜덤 포트로 Tomcat 기동 → 포트 충돌 방지
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")  // application-test.yml 활성화 → Testcontainers PostgreSQL
class AccountApiTest {

    @Autowired
    private TestRestTemplate restTemplate;  // 테스트용 HTTP 클라이언트

    @Autowired
    private AccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();  // 테스트 간 격리
    }

    @Test
    @DisplayName(AC.AC_01)
    void ac01_validAccount_createdWith201() {

        CreateAccountRequest request = new CreateAccountRequest("1101", "현금", AccountType.ASSET);

        ResponseEntity<AccountResponse> response =
                restTemplate.postForEntity("/api/accounts", request, AccountResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().code()).isEqualTo("1101");
    }

    @Test
    @DisplayName(AC.AC_02)
    void ac02_duplicateCode_rejectedWith409() {

        // 같은 코드 선점 → 중복 상황 만들기
        accountRepository.save(new Account("1101", "현금", AccountType.ASSET));

        CreateAccountRequest request = new CreateAccountRequest("1101", "현금", AccountType.ASSET);
        ResponseEntity<Void> response =
                restTemplate.postForEntity("/api/accounts", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    @DisplayName(AC.AC_03)
    void ac03_deactivate_setsActiveFalse() {

        Account saved = accountRepository.save(new Account("1101", "현금", AccountType.ASSET));

        ResponseEntity<AccountResponse> response = restTemplate.exchange(
                "/api/accounts/{id}/deactivate",
                PATCH,
                null,
                AccountResponse.class,
                saved.getId()
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().active()).isFalse();
    }

    // @Valid 검증 — AC 번호 없음 (공통 입력 검증)
    @Test
    @DisplayName("validation_blankCode_rejectedWith422")
    void validation_blankCode_rejectedWith422() {

        CreateAccountRequest request = new CreateAccountRequest("", "현금", AccountType.ASSET);

        ResponseEntity<Void> response =
                restTemplate.postForEntity("/api/accounts", request, Void.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}