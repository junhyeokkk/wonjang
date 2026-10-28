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

    // F2: 일반 전표.
    public Voucher(String voucherNo, LocalDate voucherDate, String description,
                   String idempotencyKey, String createdBy) {
        this.voucherNo = voucherNo;
        this.voucherDate = voucherDate;
        this.description = description;
        this.idempotencyKey = idempotencyKey;
        this.createdBy = createdBy;
    }

    // F4: 역분개 전표. reverses_voucher_id 는 updatable=false 라 INSERT 시 세팅해야 한다.
    public static Voucher reversal(String voucherNo, LocalDate reverseDate, String description,
                                   long reversesVoucherId, String createdBy) {
        Voucher v = new Voucher(voucherNo, reverseDate, description, null, createdBy);
        v.reversesVoucherId = reversesVoucherId;
        return v;
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
