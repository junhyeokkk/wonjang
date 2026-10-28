package com.junhyeok.wonjang.voucher.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Long> {

    // 원본 전표를 역분개한 전표가 이미 있는지 조회 (AC-15 중복 방지).
    // reverses_voucher_id 는 UNIQUE 라 결과는 최대 1건.
    Optional<Voucher> findByReversesVoucherId(Long reversesVoucherId);
}

