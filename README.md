# wonjang (원장)

> Double-entry ledger & settlement system built with NestJS

복식부기 원장을 기반으로 결제 기록 → 일별 정산 → 대사(reconciliation)까지 구현하는 백엔드 프로젝트입니다.
핵심 목표는 단 하나의 불변식을 시스템 전체에서 보장하는 것입니다.

**"돈은 생기거나 사라지지 않는다. 모든 거래의 합은 0이다."**

## 로드맵

| 단계 | 내용 | 상태 |
|------|------|------|
| 1 | 복식부기 원장 코어 (accounts / transactions / entries) | 진행 중 |
| 2 | 결제 웹훅 수신 + 멱등성 처리 | 진행 예정 |
| 3 | 일별 정산 배치 | 진행 예정|
| 4 | 대사(reconciliation) 배치 | 진행 예정 |
| 5 | 환불 / 부분취소 (역분개) | 진행 예정 |

## 핵심 설계 원칙

- **합 = 0 강제**: 균형이 맞지 않는 거래는 애플리케이션과 DB 양쪽에서 거부된다
- **Append-only**: 원장 기록은 수정/삭제하지 않는다. 정정은 역분개로만 한다
- **잔액은 파생 데이터**: 원본은 entries이며, 잔액은 집계로 계산한다
- **금액은 정수**: 부동소수점을 사용하지 않는다 (KRW 원 단위 BIGINT)

자세한 결정 배경은 [docs/adr](./docs/adr)에 기록합니다.

## 기술 스택

- NestJS / TypeScript (strict)
- PostgreSQL 16 (TypeORM, migration 기반 스키마 관리)
- Docker Compose (로컬 개발/테스트 환경)
- Jest (단위/통합 테스트), GitHub Actions (CI)

## 용어집

| 용어 | 영문 | 의미 |
|------|------|------|
| 원장 | Ledger | 모든 돈의 이동이 기록되는 장부. 이 시스템의 원본 데이터 |
| 분개 / 거래 | Transaction | 하나의 사건 단위. 2개 이상의 entry로 구성되며 합은 0 |
| 기입 | Entry | 특정 계좌의 금액 변화 한 줄 (+/- 부호 있는 정수) |
| 계좌 | Account | 돈이 머무는 논리적 위치 (예: PG 미수금, 가맹점 미지급금, 수수료 수익) |
| 역분개 | Reversal | 잘못된 거래를 수정하는 대신, 반대 방향의 거래를 추가하는 것 |
| 정산 | Settlement | 일정 기간의 거래를 집계해 가맹점별 지급액을 확정하는 것 |
| 대사 | Reconciliation | 외부 기관(PG)의 기록과 내부 원장이 일치하는지 대조하는 것 |
| 멱등성 | Idempotency | 같은 요청이 여러 번 와도 결과가 한 번 처리한 것과 같음을 보장하는 성질 |

## 시작하기

> 1단계 구현 후 작성 예정 (docker compose up → migration → seed → 실행)

## 문서

- [설계 결정 기록 (ADR)](./docs/adr)
- 개발 과정 블로그 시리즈: (링크 추가 예정)