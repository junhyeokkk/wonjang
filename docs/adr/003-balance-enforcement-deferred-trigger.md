# ADR-003: 거래 균형(합=0)은 DEFERRABLE constraint trigger로 커밋 시점에 강제한다

- 날짜: 2026-06-29
- 상태: 승인됨

## 맥락 (Context)

절대 원칙 #1은 "모든 거래(transaction)의 entries 합은 반드시 0이며, 균형이 맞지
않는 거래는 애플리케이션과 DB **양쪽**에서 거부한다"이다. 애플리케이션 레벨은
[balance.ts](../../src/modules/transaction/balance.ts)의 `assertBalanced`가 담당한다.
남은 문제는 **DB 레벨에서 어떻게 합=0을 강제하는가**이다.

복식부기 거래는 본질적으로 2줄 이상의 entry로 구성된다. 정상적인 기록 과정에서도
첫 entry를 INSERT한 직후에는 일시적으로 합이 0이 아니다(예: 차변 +1000을 넣고
대변 -1000을 넣기 직전). 즉, **개별 INSERT 시점에 합을 검사하면 정상 거래조차
중간 상태에서 거부**된다. 검증은 한 거래의 모든 entry가 들어온 뒤에 이뤄져야 한다.

[entries](../../src/database/migrations/1781231113103-InitLedgerSchema.ts) 테이블에는
이미 `amount <> 0` CHECK와 append-only 트리거가 있다.

## 검토한 선택지 (Options)

1. **즉시(NOT DEFERRABLE) 행 트리거로 매 INSERT마다 합 검사** — 구현은 단순하나,
   위에서 설명한 중간 상태 문제로 정상적인 다중 entry 거래를 기록할 수 없다. 탈락.
2. **애플리케이션 트랜잭션 안에서만 합을 검증하고 DB 제약은 두지 않음** — 절대
   원칙 #1("DB 양쪽에서 거부")을 위반한다. 앱을 우회한 직접 INSERT를 막지 못한다.
3. **DEFERRABLE INITIALLY DEFERRED constraint trigger** — 검증을 **커밋 시점**으로
   미룬다. 한 DB 트랜잭션 안에서 차변/대변을 모두 넣는 동안 중간 불균형은 허용되고,
   커밋 직전에 거래별 `SUM(amount)=0`을 검사한다. 정상 거래는 통과, 불균형 거래는
   커밋이 거부된다.
4. **애플리케이션 코드에서 거래 단위 트랜잭션을 강제 + (3)** — (3)이 동작하려면
   한 거래의 entries가 하나의 DB 트랜잭션으로 묶여야 한다. posting 서비스가 이를
   보장한다([posting.service.ts](../../src/modules/transaction/posting.service.ts)).

## 결정 (Decision)

**`entries`에 `AFTER INSERT`, `DEFERRABLE INITIALLY DEFERRED` constraint trigger
(`trg_entries_balanced`)를 두어 커밋 시점에 거래별 `SUM(amount)=0`을 강제한다.**
([EnforceBalancedTransaction](../../src/database/migrations/1781231130000-EnforceBalancedTransaction.ts))

entries는 append-only이므로 `AFTER INSERT`만 감시하면 충분하다(UPDATE/DELETE는
기존 `prevent_entry_mutation` 트리거가 차단). `amount <> 0` CHECK와 결합하면 합이
0이 되려면 최소 2줄이 필요하므로, 단일 entry 거래도 자연히 배제된다.

posting 서비스는 한 거래의 모든 entry를 **단일 DB 트랜잭션**으로 커밋해 이 트리거의
지연 검증이 의도대로 동작하도록 보장한다.

결정적 이유: 복식부기의 중간 불균형을 허용하면서도 커밋 단위로 불변식을 DB에서
강제할 수 있는 유일한 선택지가 지연 트리거다. 앱과 DB 양쪽 방어선(절대 원칙 #1)을
모두 만족한다.

## 결과 (Consequences)

- 얻는 것: 앱을 우회한 직접 INSERT도 불균형이면 커밋 시 거부된다. 정상 거래의
  중간 상태는 방해받지 않는다. 검증 로직이 DB에 있어 클라이언트 종류와 무관하다.
- 트레이드오프:
  - 한 거래의 entries는 반드시 같은 DB 트랜잭션으로 커밋해야 한다. 서로 다른
    트랜잭션으로 나눠 넣으면 각 커밋 시점에 불균형으로 거부된다(설계상 의도된 동작).
  - 위반 시 오류가 **커밋 시점**에 발생한다. 애플리케이션은 `assertBalanced`로
    1차 차단하므로 DB 오류는 앱 우회/버그 상황의 최후 방어선이다.
  - constraint trigger는 행 단위로 실행되어 거래의 entry 수만큼 호출되나, 각 호출은
    동일한 `SUM` 1회로 비용이 작다. 대량 적재 경로가 생기면 재검토한다.
- 재검토 조건: 거래당 entry 수가 매우 커지거나 대량 일괄 적재 경로가 도입되어
  행 단위 트리거 비용이 문제되면, statement-level + transition table 방식이나
  명시적 잔액 검증 단계로 전환을 검토한다.
