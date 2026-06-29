# ADR-002: 계정 타입(account.type)은 enum이 아닌 VARCHAR + 애플리케이션 제약으로 표현한다

- 날짜: 2026-06-12
- 상태: 승인됨

## 맥락 (Context)

`accounts.type`은 계정의 회계적 성격(자산/부채/수익/비용/자본)을 나타낸다.
허용되지 않은 값이 저장되는 것을 막아야 하며, 단계가 진행되며 계정 타입이
추가될 가능성(예: EXPENSE, EQUITY)이 있다.

초기 스키마([InitLedgerSchema](../../src/database/migrations/1781231113103-InitLedgerSchema.ts))는
이미 `type VARCHAR(20)`으로 생성되어 있다.

## 검토한 선택지 (Options)

1. **PostgreSQL enum 타입** — DB 레벨에서 값 집합을 강제한다. 타입 안전성이 가장
   강하지만, 값을 추가/변경할 때마다 `ALTER TYPE ... ADD VALUE` 마이그레이션이
   필요하고, enum 값 제거/순서 변경이 까다롭다. 이미 생성된 VARCHAR 컬럼을
   enum으로 바꾸려면 추가 변환 마이그레이션이 든다.
2. **VARCHAR + CHECK 제약** — DB 레벨 제약을 유지하면서 마이그레이션은 단순하다.
   값 추가 시 CHECK 제약만 교체하면 된다.
3. **VARCHAR + 애플리케이션 레벨 제약** — DB는 `VARCHAR(20)` 그대로 두고,
   애플리케이션(엔티티)에서 TypeScript union 타입(`AccountType`)으로 허용값을
   강제한다. 마이그레이션이 가장 단순하고 기존 스키마를 그대로 존중한다.

## 결정 (Decision)

**기존 `VARCHAR(20)`을 유지하고, 허용값은 애플리케이션 레벨의 TypeScript union
타입(`AccountType = 'ASSET' | 'LIABILITY' | 'REVENUE' | 'EXPENSE' | 'EQUITY'`)으로
강제한다.** enum은 채택하지 않는다.

결정적 이유: 초기 스키마가 이미 VARCHAR로 생성되어 있어 enum 전환에 별도 변환
마이그레이션이 들고, 프로젝트 초기 단계에서 계정 타입 집합이 아직 유동적이다.
애플리케이션 제약만으로 충분한 안전성을 확보하면서 스키마 변경 비용을 최소화한다.

## 결과 (Consequences)

- 얻는 것: 초기 스키마 존중, 마이그레이션 단순화, 계정 타입 추가가 코드 변경만으로 가능
- 트레이드오프: DB 레벨에서는 임의 문자열이 들어갈 수 있다(앱을 우회한 직접 INSERT 시).
  원장 무결성의 핵심 불변식(합=0, append-only)은 DB 트리거/제약으로 막지만,
  `type` 값 검증은 애플리케이션 경계에 의존한다.
- 재검토 조건: 타입 집합이 안정화되고 DB 직접 접근 경로가 늘어나면, `CHECK` 제약
  추가(선택지 2)를 우선 검토한다. 그래도 부족하면 enum 전환을 재검토한다.
