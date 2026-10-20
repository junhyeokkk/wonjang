package com.junhyeok.wonjang.account.domain;

// 계정 유형
// 자산 = 부채 + 자본
public enum AccountType {
    ASSET,          // 자산 - 차변 증가
    LIABILITY,      // 부채 - 대변 증가
    EQUITY,         // 자본 - 대변 증가
    REVENUE,        // 수익 - 대변 증가
    EXPENSE         // 비용 - 차변 증가
}