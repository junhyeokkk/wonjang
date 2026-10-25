package com.junhyeok.wonjang.ledger.presentation;

import com.junhyeok.wonjang.ledger.application.BalanceResponse;
import com.junhyeok.wonjang.ledger.application.LedgerResponse;
import com.junhyeok.wonjang.ledger.application.LedgerService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

// F3 원장·잔액 조회 API
@RestController
@RequestMapping("/api/accounts")
public class LedgerController {

    private final LedgerService ledgerService;

    public LedgerController(LedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    // 기간별 원장 조회 (from, to 생략 가능)
    @GetMapping("/{id}/ledger")
    public LedgerResponse getLedger(
            @PathVariable long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        return ledgerService.getLedger(id, from, to);
    }

    // 특정 날짜 기준 잔액 조회 (asOf 생략 시 오늘)
    @GetMapping("/{id}/balance")
    public BalanceResponse getBalance(
            @PathVariable long id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf
    ) {
        return ledgerService.getBalance(id, asOf);
    }
}
