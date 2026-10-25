package com.junhyeok.wonjang.voucher.application;

import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountNotFoundException;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.voucher.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class VoucherService {

    private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final VoucherRepository voucherRepo;
    private final EntryJdbcRepository entryRepo;
    private final AccountRepository accountRepo;
    private final VoucherJdbcRepository voucherJdbc;

    public VoucherService(VoucherRepository voucherRepo,
                          EntryJdbcRepository entryRepo,
                          AccountRepository accountRepo,
                          VoucherJdbcRepository voucherJdbc
                          ){
        this.voucherRepo = voucherRepo;
        this.entryRepo = entryRepo;
        this.accountRepo = accountRepo;
        this.voucherJdbc = voucherJdbc;
    }

    @Transactional
    public VoucherResponse create(CreateVoucherRequest req, String userId) {
        // 1. 균형 사전 검증
        long debit = sumBySide(req.entries(), EntrySide.DEBIT);
        long credit = sumBySide(req.entries(), EntrySide.CREDIT);
        if(debit != credit) {
            throw new UnbalancedVoucherException(debit, credit);
        }

        // 2. accountCode -> Account (NotFound,Inactive 던짐)
        List<EntryJdbcRepository.NewEntry> rows = buildEntryRows(req.entries());

        // 3. voucher INSERT
        String placeholder = "TMP-" +java.util.UUID.randomUUID().toString().substring(0, 8);
        Voucher voucher = new Voucher(placeholder, req.voucherDate(), req.description(), null, userId);
        Voucher saved = voucherRepo.save(voucher);

        voucherRepo.flush();

        // 4. voucher_no 조립 + UPDATE
        String voucherNo = "V-" + req.voucherDate().format(YMD) + "-" + String.format("%06d", saved.getId());
        voucherJdbc.updateVoucherNo(saved.getId(), voucherNo);

        // 5. entries batch INSERT
        entryRepo.insertAll(saved.getId(), rows);

        return VoucherResponse.from(voucher, voucherNo, debit, credit);
    }

    private long sumBySide(List<EntryLine> entries, EntrySide side) {
        return entries.stream()
                .filter(e -> e.side() == side)
                .mapToLong(EntryLine::amount)
                .sum();
    }

    private List<EntryJdbcRepository.NewEntry> buildEntryRows(List<EntryLine> entries){
        List<EntryJdbcRepository.NewEntry> rows = new ArrayList<>(entries.size());
        short lineNo = 1;
        for(EntryLine entry : entries){
            Account acc = accountRepo.findByCode(entry.accountCode())
                    .orElseThrow(() -> new AccountNotFoundException(entry.accountCode()));
            if(!acc.isActive()) {
                throw new AccountInactiveException(entry.accountCode());
            }
            rows.add(new EntryJdbcRepository.NewEntry(acc.getId(), entry.side(), entry.amount(), lineNo++));
        }
        return rows;
    }
}

