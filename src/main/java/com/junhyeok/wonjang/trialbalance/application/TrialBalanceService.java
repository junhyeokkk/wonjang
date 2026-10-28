package com.junhyeok.wonjang.trialbalance.application;

import com.junhyeok.wonjang.trialbalance.domain.TrialBalanceJdbcRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TrialBalanceService {

    private final TrialBalanceJdbcRepository repo;

    public TrialBalanceService(TrialBalanceJdbcRepository repo) {
        this.repo = repo;
    }

    // AC-17: asOf 까지의 전 계정 차변합·대변합·잔액 + 총합 2개.
    // AC-18: 총합 불일치 시 balanced=false + diff.
    //        정상 경로에서는 항상 balanced=true 여야 한다 (전표 균형 불변식).
    public TrialBalanceResponse getTrialBalance(LocalDate asOf) {
        List<TrialBalanceJdbcRepository.Row> raw = repo.findSumsAsOf(asOf);

        List<TrialBalanceResponse.Row> rows = new ArrayList<>(raw.size());
        long debitGrand = 0;
        long creditGrand = 0;
        for (TrialBalanceJdbcRepository.Row r : raw) {
            long balance = balanceOf(r.accountType(), r.debitTotal(), r.creditTotal());
            rows.add(new TrialBalanceResponse.Row(
                    r.accountCode(), r.accountName(), r.accountType(),
                    r.debitTotal(), r.creditTotal(), balance
            ));
            debitGrand += r.debitTotal();
            creditGrand += r.creditTotal();
        }

        long diff = debitGrand - creditGrand;
        boolean balanced = diff == 0;
        return new TrialBalanceResponse(asOf, rows, debitGrand, creditGrand, balanced, diff);
    }

    // AC-12: ASSET/EXPENSE 는 debit-credit, 나머지는 credit-debit.
    private long balanceOf(String type, long debit, long credit) {
        return switch (type) {
            case "ASSET", "EXPENSE" -> debit - credit;
            default -> credit - debit;
        };
    }
}
