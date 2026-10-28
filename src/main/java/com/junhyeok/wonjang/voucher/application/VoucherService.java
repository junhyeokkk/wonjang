package com.junhyeok.wonjang.voucher.application;

import com.junhyeok.wonjang.account.domain.Account;
import com.junhyeok.wonjang.account.domain.AccountNotFoundException;
import com.junhyeok.wonjang.account.domain.AccountRepository;
import com.junhyeok.wonjang.voucher.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
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

    // F4 역분개:
    //   1) 원본 조회 — 없으면 404 (VoucherNotFoundException)
    //   2) 원본이 역분개 전표면 422 (CannotReverseReversalException)   AC-27
    //   3) 이미 역분개됐으면 409 (AlreadyReversedException)              AC-15
    //   4) 원본 entries 를 side 반전해서 새 전표에 복사                   AC-13
    //   5) 새 전표의 reverses_voucher_id 로 원본 id 세팅                   AC-14
    //   6) voucher_no 는 생성과 동일 패턴 "V-yyyyMMdd-NNNNNN"
    @Transactional
    public VoucherResponse reverse(long originalId, ReverseVoucherRequest req, String userId) {
        Voucher original = voucherRepo.findById(originalId)
                .orElseThrow(() -> new VoucherNotFoundException(originalId));

        if (original.getReversesVoucherId() != null) {
            throw new CannotReverseReversalException(originalId, original.getReversesVoucherId());
        }

        voucherRepo.findByReversesVoucherId(originalId).ifPresent(existing -> {
            throw new AlreadyReversedException(originalId, existing.getId());
        });

        LocalDate reverseDate = req.reverseDate() != null ? req.reverseDate() : LocalDate.now();
        String description = req.description() != null
                ? req.description()
                : "Reversal of " + original.getVoucherNo();

        // 1) voucher INSERT (reversesVoucherId 세팅, voucher_no 는 placeholder 뒤에 UPDATE)
        String placeholder = "TMP-" + java.util.UUID.randomUUID().toString().substring(0, 8);
        Voucher reversal = Voucher.reversal(placeholder, reverseDate, description, originalId, userId);
        Voucher saved = voucherRepo.save(reversal);
        voucherRepo.flush();

        String voucherNo = "V-" + reverseDate.format(YMD) + "-" + String.format("%06d", saved.getId());
        voucherJdbc.updateVoucherNo(saved.getId(), voucherNo);

        // 2) 원본 entries → side 반전해서 새 전표에 batch INSERT
        List<EntryJdbcRepository.NewEntry> originals = entryRepo.findByVoucherId(originalId);
        List<EntryJdbcRepository.NewEntry> reversed = new ArrayList<>(originals.size());
        long debit = 0, credit = 0;
        for (EntryJdbcRepository.NewEntry e : originals) {
            EntrySide flipped = e.side() == EntrySide.DEBIT ? EntrySide.CREDIT : EntrySide.DEBIT;
            reversed.add(new EntryJdbcRepository.NewEntry(e.accountId(), flipped, e.amount(), e.lineNo()));
            if (flipped == EntrySide.DEBIT) debit += e.amount(); else credit += e.amount();
        }
        entryRepo.insertAll(saved.getId(), reversed);

        return VoucherResponse.from(saved, voucherNo, debit, credit);
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

