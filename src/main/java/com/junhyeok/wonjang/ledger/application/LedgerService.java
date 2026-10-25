package com.junhyeok.wonjang.ledger.application;

import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountNotFoundException;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.account.domain.AccountType;
import com.junhyeok.wonjang.ledger.domain.LedgerJdbcRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

// F3 원장·잔액 조회.
// - ledger + balance 를 한 서비스에서 (계정 유형별 부호 로직 공유)
// - 계정 유형별 부호 (ADR-004 / AC-12):
//     ASSET, EXPENSE          → +1 (차변 증가 = 잔액 증가)
//     LIABILITY, EQUITY, REVENUE → -1 (대변 증가 = 잔액 증가)
//   공식: balance = signMultiplier * (SUM(DEBIT) - SUM(CREDIT))
@Service
public class LedgerService {

    private final AccountRepository accountRepo;
    private final LedgerJdbcRepository ledgerRepo;

    public LedgerService(AccountRepository accountRepo, LedgerJdbcRepository ledgerRepo) {
        this.accountRepo = accountRepo;
        this.ledgerRepo = ledgerRepo;
    }


    // 기간별 원장 조회
    // ex : 11월 한 달 거래 내역 보여줘
    @Transactional(readOnly = true)
    public LedgerResponse getLedger(long accountId, LocalDate from, LocalDate to) {

        // 1. Account 조회
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));

        // 2. sign 결정
        int sign = signMultiplierOf(account.getType());

        // 3. 기초 잔액 -  조회 기간 시작 전까지 쌓인 금액
        long opening = sign * ledgerRepo.sumDebitMinusCreditBefore(accountId, from);

        // 4. 기간별 원장 라인 목록 조회
        List<LedgerLine> lines = ledgerRepo.findLines(accountId, from, to, opening, sign);

        // 5. 마감 잔액
        long closing = lines.isEmpty() ? opening : lines.get(lines.size()-1).balance();

        return LedgerResponse.of(account, opening, lines, closing);
    }

    // 특정 날짜 기준 잔액 조회
    // ex : 지금 얼마 있어?
    @Transactional(readOnly = true)
    public BalanceResponse getBalance(long accountId, LocalDate asOf) {

        // 1. account 조회
        Account account = accountRepo.findById(accountId)
                .orElseThrow(() -> new AccountNotFoundException(accountId));
        // 2. sign 결정
        int sign = signMultiplierOf(account.getType());

        // 3. 잔액 계산 — asOf 까지 쌓인 금액
        long balance = sign * ledgerRepo.sumDebitMinusCreditAsOf(accountId, asOf);

        // 4. asOf가 null이면 오늘로 응답
        LocalDate asOfForResponse = (asOf != null) ? asOf : LocalDate.now();

        return BalanceResponse.of(account, asOfForResponse, balance);
    }


    // 계정 유형별 부호 보정
    // ASSET, EXPENSE -> +1
    // LIABILITY, EQUITY, REVENUE -> -1
    private static int signMultiplierOf(AccountType type) {
        return switch (type) {
            case ASSET, EXPENSE -> +1;              // 자산, 비용
            case LIABILITY, EQUITY, REVENUE -> -1;  // 부채, 자본, 수익
        };
    }
}
