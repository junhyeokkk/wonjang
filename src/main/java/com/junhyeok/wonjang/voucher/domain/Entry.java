package com.junhyeok.wonjang.voucher.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "entries") // append-only (UPDATE/DELETE 금지)
public class Entry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_id", nullable = false, updatable = false)
    private Long voucherId;

    @Column(name = "account_id", nullable = false, updatable = false)
    private Long accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6, updatable = false, columnDefinition = "CHAR(6)")
    private EntrySide side;

    @Column(nullable = false, updatable = false)
    private long amount;

    @Column(name = "line_no", nullable = false, updatable = false)
    private short lineNo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    protected Entry() {}

    public Long getId() {
        return id;
    }

    public Long getVoucherId() {
        return voucherId;
    }

    public Long getAccountId() {
        return accountId;
    }

    public EntrySide getSide() {
        return side;
    }

    public long getAmount() {
        return amount;
    }

    public short getLineNo() {
        return lineNo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}

