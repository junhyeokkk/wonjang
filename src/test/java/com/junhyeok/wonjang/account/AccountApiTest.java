package com.junhyeok.wonjang.account;

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

    // AC-02: 정상 등록 → 201
    @Test
    @DisplayName("ac02_validAccount_createdWith201")
    void ac02_validAccount_createdWith201() {

        // 1. 요청 DTO 생성
        CreateAccountRequest request = new CreateAccountRequest("1101", "현금", AccountType.ASSET);

        // 2. POST 요청
        ResponseEntity<AccountResponse> response =
                restTemplate.postForEntity("/api/accounts", request, AccountResponse.class);

        // 3. 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().code()).isEqualTo("1101");
    }

    // AC-01: 코드 중복 → 409
    @Test
    @DisplayName("ac01_duplicateCode_rejectedWith409")
    void ac01_duplicateCode_rejectedWith409() {

        // 1. DB에 "1101" 계정 미리 저장 (충돌 상황 만들기)
        accountRepository.save(new Account("1101", "현금", AccountType.ASSET));

        // 2. 같은 코드로 다시 POST
        CreateAccountRequest request = new CreateAccountRequest("1101", "현금", AccountType.ASSET);
        ResponseEntity<Void> response =
                restTemplate.postForEntity("/api/accounts", request, Void.class);

        // 3. 409 CONFLICT 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // AC-03: 비활성화 → active=false
    @Test
    @DisplayName("ac03_deactivate_setsActiveFalse")
    void ac03_deactivate_setsActiveFalse() {

        // 1. 계정 저장 후 id 획득
        Account saved = accountRepository.save(new Account("1101", "현금", AccountType.ASSET));

        // 2. PATCH 요청 (exchange는 HTTP 메서드를 직접 지정할 수 있다)
        ResponseEntity<AccountResponse> response = restTemplate.exchange(
                "/api/accounts/{id}/deactivate",
                PATCH,
                null,
                AccountResponse.class,
                saved.getId()
        );

        // 3. active=false 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().active()).isFalse();
    }

    // @Valid 검증 실패 → 422
    @Test
    @DisplayName("validation_blankCode_rejectedWith422")
    void validation_blankCode_rejectedWith422() {

        // 1. code = "" 로 요청
        CreateAccountRequest request = new CreateAccountRequest("", "현금", AccountType.ASSET);

        ResponseEntity<Void> response =
                restTemplate.postForEntity("/api/accounts", request, Void.class);

        // 2. 검증
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }
}