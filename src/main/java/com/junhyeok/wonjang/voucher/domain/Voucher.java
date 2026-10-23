package com.junhyeok.wonjang.voucher.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "vouchers")
public class Voucher {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "voucher_no", nullable = false, unique = true, length = 20, updatable = false)
    private String voucherNo;

    @Column(name = "voucher_date", nullable = false, updatable = false)
    private LocalDate voucherDate;

    @Column(length = 200, updatable = false)
    private String description;

    @Column(name = "reverses_voucher_id", unique = true, updatable = false)
    private Long reversesVoucherId;

    @Column(name = "idempotency_key", unique = true, length = 64, updatable = false)
    private String idempotencyKey;

    @Column(name = "created_by", nullable = false, length = 50, updatable = false)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        this.createdAt = OffsetDateTime.now();
    }

    protected Voucher() {}

    // F2: 일반 전표 생성. 역분개는 F4에서 별도 factory 추가 예정.
    public Voucher(String voucherNo, LocalDate voucherDate, String description,
                   String idempotencyKey, String createdBy) {
        this.voucherNo = voucherNo;
        this.voucherDate = voucherDate;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.createdBy = createdBy;
        // reversesVoucherId stays null — F4에서 reversal factory 로 세팅
    }

    public Long getId() { return id; }
    public String getVoucherNo() { return voucherNo; }
    public LocalDate getVoucherDate() { return voucherDate; }
    public String getDescription() { return description; }
    public Long getReversesVoucherId() { return reversesVoucherId; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getCreatedBy() { return createdBy; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}
