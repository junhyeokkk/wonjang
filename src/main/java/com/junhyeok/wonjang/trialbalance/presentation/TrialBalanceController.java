package com.junhyeok.wonjang.trialbalance.presentation;

import com.junhyeok.wonjang.trialbalance.application.TrialBalanceResponse;
import com.junhyeok.wonjang.trialbalance.application.TrialBalanceService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/trial-balance")
public class TrialBalanceController {

    private final TrialBalanceService service;

    public TrialBalanceController(TrialBalanceService service) {
        this.service = service;
    }

    // GET /api/trial-balance?asOf=2026-10-23
    // asOf 생략 시 전 기간 합산.
    @GetMapping
    public TrialBalanceResponse getTrialBalance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOf) {
        return service.getTrialBalance(asOf);
    }
}
