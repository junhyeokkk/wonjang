# ADR-005: 금액은 BIGINT 원 단위 양의 정수로 저장하고, 방향은 side로만 표현한다

- 날짜: 2026-10-19 (최초 2026-06-11, NestJS 시절 결정을 Java 기준으로 개정)
- 상태: 승인됨

## 맥락 (Context)

원장의 금액을 어떤 타입으로 저장할지, 차변/대변 방향을 어떻게 표현할지 정해야 한다.
1원의 오차도 허용되지 않으며, "차변 합 = 대변 합" 검증이 정확해야 한다.

## 검토한 선택지 (Options)

1. **FLOAT / DOUBLE** — 이진 부동소수점이라 0.1 + 0.2 ≠ 0.3 류의 오차. 사용 불가.
2. **NUMERIC + Java `BigDecimal`** — 정확하지만 KRW 단일 통화에는 과하다. 연산·비교 코드가 장황해진다.
3. **BIGINT + Java `long` (최소 화폐 단위 정수)** — KRW는 소수점이 없어 원 단위로 손실 없이 표현된다.

방향 표현:

- **(a) 부호 있는 금액** (`+` 차변 / `−` 대변) — 합 = 0으로 검증이 단순하지만, `−50,000`이
  대변인지 입력 오류인지 모호하다.
- **(b) `side` 컬럼 + 양수 금액** — 회계 장부 표기와 같고, 음수는 언제나 오류다.

## 결정 (Decision)

**BIGINT / `long`, 방향은 (b) `side`(`DEBIT`/`CREDIT`) + `amount > 0`.**

- DB: `amount BIGINT NOT NULL CHECK (amount > 0)`, `side CHAR(6) CHECK (side IN ('DEBIT','CREDIT'))`
- Java: `long`. `float`/`double`/`BigDecimal` 산술 금지. 합산은 `Math.addExact`로 오버플로를 예외로 만든다.
- JSON: 숫자로 직렬화한다 (KRW 범위는 JS 안전 정수 2^53 이내). 입력에서 `500000.5` 같은
  소수가 `long`으로 **잘려 들어오지 않도록** Jackson `ACCEPT_FLOAT_AS_INT`를 끈다 → `INVALID_AMOUNT`(422).
- 표시용 포맷(콤마·통화 기호)은 API 밖의 관심사다.

## 결과 (Consequences)

- 얻는 것: 연산 오차 원천 차단. 균형 검증은 `SUM(DEBIT) = SUM(CREDIT)` 비교 하나.
- 트레이드오프: 잔액 계산 시 계정 유형별로 부호 방향을 따로 적용해야 한다 (AC-12).
- 재검토 조건: 다중 통화(USD 등, 비범위) 도입 시 통화별 최소 단위 메타데이터(`currency_exponent`)와 함께 재검토.
