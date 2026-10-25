package com.junhyeok.wonjang;

// 수용기준(AC) 번호 → 한줄 설명
// 테스트의 @DisplayName 에서 참조하면 리포트에 번호+설명이 같이 뜬다.
//
// 예:
//   @Test
//   @DisplayName(AC.AC_11 + " — 2026-11-01 요청 시 10월말 잔액이 openingBalance 로 나온다")
//   void ac11_...() { ... }
//
// 정본은 docs/requirements.md (그 정본은 Notion 02).
// AC 추가·수정 시 세 곳 (Notion 02, docs/requirements.md, 이 파일) 전부 갱신.
public final class AC {

    private AC() {}

    // F1 계정과목
    public static final String AC_01 = "AC-01: 코드·이름·유형으로 계정 등록";
    public static final String AC_02 = "AC-02: 같은 코드 중복 등록 시 409 (UNIQUE)";
    public static final String AC_03 = "AC-03: 분개 있는 계정은 비활성화만 가능";

    // F2 전표 입력
    public static final String AC_04 = "AC-04: 분개 2줄+, 차변=대변 → 201";
    public static final String AC_05 = "AC-05: 차변!=대변 → 422 + 모자란 쪽/금액";
    public static final String AC_06 = "AC-06: 서비스 우회해 DB 직접 입력도 불균형 거부 (트리거)";
    public static final String AC_07 = "AC-07: 분개 중 하나라도 실패하면 전표 전체 롤백";
    public static final String AC_08 = "AC-08: 금액은 양의 정수, 음수·0 거부";
    public static final String AC_09 = "AC-09: 비활성 계정으로는 분개 불가";

    // F3 원장·잔액
    public static final String AC_10 = "AC-10: 원장은 전표일↑, 같은 날짜면 생성순↑, 누적 잔액 포함";
    public static final String AC_11 = "AC-11: 기간 필터 시 기초 잔액(from 이전)을 첫 줄로 포함";
    public static final String AC_12 = "AC-12: 잔액은 계정 유형별 부호 방향";

    // F4 역분개
    public static final String AC_13 = "AC-13: POST /vouchers/{id}/reverse → 차변↔대변 반전 전표 생성, 원본 그대로";
    public static final String AC_14 = "AC-14: 역분개 전표는 원본 id 를 reverses_voucher_id 로 가리킴";
    public static final String AC_15 = "AC-15: 이미 역분개된 전표를 또 역분개하면 409";
    public static final String AC_16 = "AC-16: entries UPDATE/DELETE 시 DB 트리거가 예외";
    public static final String AC_27 = "AC-27: 역분개 전표를 다시 역분개하면 422 (CANNOT_REVERSE_REVERSAL)";
    public static final String AC_28 = "AC-28: 역분개 일자는 요청일 기본, 마감일 이후여야 함";

    // F5 시산표
    public static final String AC_17 = "AC-17: 기준일 입력 시 전 계정 합·잔액 + 총합 2개";
    public static final String AC_18 = "AC-18: 총합 불일치 시 balanced:false + 차이액";

    // F6 스냅샷
    public static final String AC_19 = "AC-19: 일마감 시 계정별 잔액을 balance_snapshots 에 저장";
    public static final String AC_20 = "AC-20: 잔액 조회는 스냅샷 + 이후 분개, 전체 합산과 동일";
    public static final String AC_21 = "AC-21: 마감일 이전 날짜 전표 입력 시 422";

    // F7 멱등성
    public static final String AC_22 = "AC-22: 같은 Idempotency-Key 두 번 → 첫 응답 재반환, 전표 1개";
    public static final String AC_23 = "AC-23: 같은 키 다른 본문이면 422";

    // 비기능
    public static final String AC_24 = "AC-24: 100 스레드 동시 입력 → 전부 성공, 시산표 균형";
    public static final String AC_25 = "AC-25: 모든 에러 응답은 {code, message, detail}";
    public static final String AC_26 = "AC-26: 모든 쓰기 API 는 X-User-Id + 시각 기록";
}
