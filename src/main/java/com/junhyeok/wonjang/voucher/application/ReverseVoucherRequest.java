package com.junhyeok.wonjang.voucher.application;

import jakarta.validation.constraints.Size;

import java.time.LocalDate;

// POST /api/vouchers/{id}/reverse 바디.
// 둘 다 선택:
//   reverseDate  — null 이면 서비스에서 LocalDate.now() (AC-28 기본)
//   description  — null 이면 서비스에서 "Reversal of V-..." 자동 생성
public record ReverseVoucherRequest(
        LocalDate reverseDate,
        @Size(max = 200) String description
) {
}
