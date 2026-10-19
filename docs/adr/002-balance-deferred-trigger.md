# ADR-002: 전표 균형(차변 합 = 대변 합)은 DEFERRABLE constraint trigger로 DB에서도 강제한다

- 날짜: 2026-10-19
- 상태: 제안됨 (2주차에 실제 동작 확인 후 승인)

## 맥락 (Context)

절대 원칙 #1은 "차변 합 ≠ 대변 합인 전표는 애플리케이션과 DB **양쪽**에서 거부한다"이다.
서비스 코드에서만 검사하면 직접 SQL·배치·다른 클라이언트가 DB에 쓸 때 깨진다.

전표는 본질적으로 2줄 이상의 분개로 구성된다. 첫 줄을 INSERT한 직후에는 정상 전표도
일시적으로 불균형이다. 따라서 **행 단위 즉시 검사로는 정상 전표조차 거부**된다.

## 검토한 선택지 (Options)

1. **서비스 검증만** — DB 직접 접근에 무방비. 원칙 #1 위반. 탈락.
2. **즉시(NOT DEFERRABLE) 행 트리거** — 위의 중간 상태 문제로 다중 분개 전표를 넣을 수
   없다. 탈락.
3. **분개를 한 행(JSON)에 묶어 저장하고 CHECK** — 원장 조회·집계가 불편해진다. 탈락.
4. **서비스 검증 + `DEFERRABLE INITIALLY DEFERRED` constraint trigger** — 검사를 커밋
   시점으로 미룬다. 트랜잭션 안의 중간 불균형은 허용하고, 커밋 직전에 voucher 단위로
   차변 합 = 대변 합을 확인한다.

## 결정 (Decision)

**4번.** `entries`에 `AFTER INSERT DEFERRABLE INITIALLY DEFERRED` constraint trigger
(`trg_voucher_balanced`, [V3](../../src/main/resources/db/migration/V3__triggers.sql))를 둔다.

```sql
SELECT COALESCE(SUM(amount) FILTER (WHERE side = 'DEBIT'),  0),
       COALESCE(SUM(amount) FILTER (WHERE side = 'CREDIT'), 0)
FROM entries WHERE voucher_id = NEW.voucher_id;   -- d <> c 이면 RAISE
```

- entries는 append-only([ADR-001](001-append-only-reversal.md))라 `AFTER INSERT`만 감시하면 된다.
- `amount > 0` CHECK와 결합하면 균형을 맞추려면 최소 2줄이 필요해 단일 분개 전표도 배제된다.
- 서비스는 한 전표의 voucher + entries 전체를 **단일 DB 트랜잭션**으로 커밋한다.
- entries INSERT는 `JdbcTemplate`으로 한다. JPA `save()`는 flush 시점이 불명확해 지연 트리거와
  상호작용을 추론하기 어렵다.

## 결과 (Consequences)

- 얻는 것: 앱을 우회한 직접 INSERT도 불균형이면 커밋에서 거부된다 (AC-06).
- 트레이드오프:
  - PostgreSQL 종속.
  - 위반 오류가 **커밋 시점**에 `TransactionSystemException` 계열로 올라온다. 서비스의 1차 검증이
    정상 경로를 막고, DB 오류는 버그·우회 상황의 최후 방어선이다. ControllerAdvice에서 이 경우도
    `UNBALANCED_VOUCHER`(422)로 매핑한다.
  - 행 트리거라 전표의 분개 수만큼 같은 SUM이 반복 실행된다. 결과는 같고 비용만 늘어난다.
    분개 수가 적어 무시 가능하다고 보고, 5주차 측정 때 수치로 확인한다.
- 재검토 조건: 전표당 분개 수가 매우 커지거나 대량 적재 경로가 생기면 statement-level 트리거 +
  transition table 방식으로 전환을 검토한다.
