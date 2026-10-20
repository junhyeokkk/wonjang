package com.junhyeok.wonjang.account.application;

import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountNotFoundException;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.account.domain.DuplicateAccountCodeException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

// @Service: Spring이 Bean으로 등록 — @Autowired 없이 생성자 주입 가능
@Service
public class AccountService {

    private final AccountRepository accountRepository;

    // 생성자 주입 — 생성자가 하나면 @Autowired 생략 가능
    public AccountService(AccountRepository accountRepository) {
        this.accountRepository = accountRepository;
    }

    // AC-01, AC-02: 계정 등록
    @Transactional
    public AccountResponse createAccount(CreateAccountRequest request) {

        // code 중복 검사
        if(accountRepository.existsByCode(request.code())) {
            throw new DuplicateAccountCodeException(request.code());
        }

         Account saved = accountRepository.save(
                 new Account(request.code(), request.name(), request.type())
        );

        return AccountResponse.from(saved);
    }

    // 전체 계정 목록 조회
    @Transactional(readOnly = true) // 더티체크 생략 -> 조회성능 증가
    public List<AccountResponse> getAccounts() {
        return accountRepository.findAll()
                .stream()
                .map(AccountResponse::from)
                .toList();
    }

    // AC-03: 비활성화
    @Transactional
    public AccountResponse deactivateAccount(Long id) {

        Account account = accountRepository.findById(id).orElseThrow(() -> new AccountNotFoundException(id));

        account.deactivate(); // 비활성화

        return AccountResponse.from(account);
    }
}