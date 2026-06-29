# CLAUDE.md

이 파일은 이 저장소에서 작업하는 Claude Code(및 모든 AI 에이전트)를 위한 지침이다.
여기 적힌 **절대 원칙**은 어떤 경우에도 위반하지 않는다.

## 프로젝트 개요

- **wonjang (원장)**: 복식부기(double-entry) 원장 기반 결제·정산 시스템
- **기술 스택**: NestJS + TypeScript (strict) + PostgreSQL 16 (TypeORM, migration 기반)
- **핵심 불변식**: "돈은 생기거나 사라지지 않는다. 모든 거래의 합은 0이다."

### 로드맵

| 단계 | 내용 | 상태 |
|------|------|------|
| 1 | 원장 코어 (accounts / transactions / entries) | 진행 중 |
| 2 | 결제 웹훅 수신 + 멱등성 처리 | 예정 |
| 3 | 일별 정산 배치 | 예정 |
| 4 | 대사(reconciliation) 배치 | 예정 |
| 5 | 환불 / 부분취소 (역분개) | 예정 |

## 절대 원칙 (위반 금지)

1. **합 = 0 강제**: 모든 거래(transaction)의 entries 합은 반드시 0이어야 한다.
   균형이 맞지 않는 거래는 애플리케이션과 DB 양쪽에서 거부한다.
2. **Append-only**: entries는 추가만 한다. entries에 대한 `UPDATE`/`DELETE` 코드를
   **절대 작성하지 않는다**. 정정이 필요하면 오직 **역분개(reversal)** 로만 처리한다.
3. **금액은 항상 정수**: `float`/`number` 산술 연산 금지. 애플리케이션에서는 TypeScript
   `bigint`, DB에서는 `BIGINT`를 사용한다. (근거: [ADR-001](docs/adr/001-integer-amount.md))
4. **스키마 변경은 마이그레이션으로만**: 스키마는 마이그레이션 파일로만 바꾼다.
   `synchronize`는 **영원히 false**다. ([src/app.module.ts](src/app.module.ts#L23))
5. **잔액은 파생 데이터**: 잔액은 컬럼으로 저장하지 않는다. 항상 entries 집계로
   파생 계산한다 (`SUM(amount)`).

## 개발 환경

- **DB**: `docker compose up` 으로 PostgreSQL 16 기동. 접속 정보는 `.env`의 `DATABASE_URL`.
  호스트 포트는 **5432** ([docker-compose.yml](docker-compose.yml#L9-L10), `.env`의
  `DATABASE_URL` 모두 5432로 일치). `DATABASE_URL`의 포트는 docker-compose 매핑과
  항상 일치시킨다.
- **패키지 매니저**: `pnpm`
- **마이그레이션 명령**:
  - `pnpm migration:create src/database/migrations/<Name>` — 빈 마이그레이션 생성
  - `pnpm migration:run` — 미적용 마이그레이션 실행
  - `pnpm migration:revert` — 마지막 마이그레이션 롤백
- **DataSource**: [src/database/data-source.ts](src/database/data-source.ts) (TypeORM CLI용)
- **새 패키지 설치**: 설치 시 빌드 스크립트 승인이 필요하면 `pnpm approve-builds`를
  실행하도록 사용자에게 안내한다.

## 작업 규칙

- **ADR**: 설계 결정이 생기면 [docs/adr/000-template.md](docs/adr/000-template.md) 형식으로
  ADR 작성을 **제안**한다. (예: [ADR-001](docs/adr/001-integer-amount.md))
- **커밋 메시지**: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`,
  `test:`, `perf:`, `ci:`).
- **테스트**: 비즈니스 로직에는 테스트를 함께 작성한다. (`pnpm test` / `pnpm test:cov`)
- **코드 작성 전 승인**: 사용자가 명시적으로 요청한 경우, 구현 계획을 먼저 제안하고
  승인을 받은 뒤 코드를 작성한다.

## 자주 쓰는 명령

```bash
pnpm install            # 의존성 설치
docker compose up -d    # PostgreSQL 기동
pnpm migration:run      # 마이그레이션 적용
pnpm start:dev          # 개발 서버 (watch)
pnpm test               # 단위 테스트
pnpm lint               # ESLint + 자동 수정
pnpm build              # 프로덕션 빌드
```

## 용어집

| 용어 | 영문 | 의미 |
|------|------|------|
| 원장 | Ledger | 모든 돈의 이동이 기록되는 원본 장부 |
| 분개 / 거래 | Transaction | 하나의 사건 단위. 2개 이상 entry로 구성되며 합은 0 |
| 기입 | Entry | 특정 계좌의 금액 변화 한 줄 (+/- 부호 있는 정수) |
| 계좌 | Account | 돈이 머무는 논리적 위치 |
| 역분개 | Reversal | 잘못된 거래를 반대 방향 거래 추가로 정정하는 것 |
| 정산 | Settlement | 기간 거래를 집계해 가맹점별 지급액을 확정하는 것 |
| 대사 | Reconciliation | 외부 기관(PG) 기록과 내부 원장의 일치 여부 대조 |
| 멱등성 | Idempotency | 같은 요청이 여러 번 와도 한 번 처리한 것과 같음을 보장 |
