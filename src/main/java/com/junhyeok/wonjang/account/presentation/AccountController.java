package com.junhyeok.wonjang.account.presentation;

import com.junhyeok.wonjang.account.application.AccountResponse;
import com.junhyeok.wonjang.account.application.AccountService;
import com.junhyeok.wonjang.account.application.CreateAccountRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import java.util.List;

// @RestController = @Controller + @ResponseBody (반환값을 JSON으로 직렬화)
// @RequestMapping: 이 컨트롤러의 base URL
@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    // POST /api/accounts — 계정 등록, 성공 시 201 Created
    // @RequestBody: JSON 바디를 DTO로 역직렬화
    // @Valid: CreateAccountRequest의 @NotBlank 등 어노테이션 검증 실행
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse createAccount(@RequestBody @Valid CreateAccountRequest request) {
        return accountService.createAccount(request);
    }

    // GET /api/accounts — 전체 목록
    @GetMapping
    public List<AccountResponse> getAccounts() {
        return accountService.getAccounts();
    }

    // PATCH /api/accounts/{id}/deactivate — 비활성화
    // @PathVariable: URL의 {id} 부분을 파라미터로 바인딩
    @PatchMapping("/{id}/deactivate")
    public AccountResponse deactivateAccount(@PathVariable Long id) {
        return accountService.deactivateAccount(id);
    }
}