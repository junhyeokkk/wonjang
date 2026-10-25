# wonjang (원장)

> 돈이 움직인 기록을 **틀리지 않게, 지워지지 않게** 남기는 복식부기 원장 서버
> **오픈뱅킹 연동 복식부기 정산 시스템** — 원장 코어 + 일마감 배치 + 오픈뱅킹 외부이체 정합성

전표를 받아 분개로 기록하고, 계정별 원장·잔액·시산표를 제공합니다.
차변 ≠ 대변이면 저장 자체를 거부하고, 한 번 저장된 분개는 수정·삭제할 수 없습니다.
내부 이체는 분개 1건으로 묶고, 외부 이체는 "대기→확정/역분개" 로 원장 정합성을 유지합니다.

**"돈은 생기거나 사라지지 않는다. 모든 전표의 차변 합 = 대변 합이다."**

## 핵심 설계

| 원칙 | 어떻게 지키나 | ADR |
|------|---------------|-----|
| 차변 합 = 대변 합 | 서비스 검증 + DB `DEFERRABLE` constraint trigger (커밋 시점 검사) | [002](docs/adr/002-balance-deferred-trigger.md) |
| Append-only | DB 트리거가 entries `UPDATE`/`DELETE` 거부, 정정은 역분개로만 | [001](docs/adr/001-append-only-reversal.md) |
| 락은 필요한 곳에만 | 잔액 컬럼이 없어 입력끼리 경합 없음. 채번은 시퀀스, 마감 경계만 행 락 | [003](docs/adr/003-concurrency-lock-strategy.md) |
| 잔액은 파생 데이터 | 일마감 스냅샷 + 이후 분개 증분 합산 | [004](docs/adr/004-balance-snapshot.md) |
| 금액은 정수 | `BIGINT`/`long` 원 단위 양수, 방향은 `side` | [005](docs/adr/005-integer-amount.md) |

## 기능

| # | 기능 | API |
|---|------|-----|
| F1 | 계정과목 등록·조회·비활성화 | `POST/GET /accounts`, `PATCH /accounts/{id}/deactivate` |
| F2 | 전표 입력 (균형 검증) | `POST /vouchers`, `GET /vouchers/{id}` |
| F3 | 원장 · 잔액 | `GET /accounts/{id}/ledger?from=&to=`, `GET /accounts/{id}/balance?asOf=` |
| F4 | 역분개 | `POST /vouchers/{id}/reverse` |
| F5 | 시산표 | `GET /trial-balance?asOf=` |
| F6 | 일마감 스냅샷 (6주차 Spring Batch 로 승격) | `POST /closings` + Job(스냅샷·대사·마감잠금) |
| F7 | 멱등성 키 | `Idempotency-Key` 헤더 |
| F8 | 내부 이체 (두 계좌 간 이동 = 분개 1건) | `POST /transfers/internal` |
| F9 | 외부 이체 (오픈뱅킹, 대기→확정/역분개) | `POST /transfers/external` |

## 기술 스택

Java 21 · Spring Boot 3.4 · Spring Data JPA + JdbcTemplate · Spring Batch(6주차) · Flyway · PostgreSQL 16 · GitHub Actions

## 시작하기

```bash
cp .env.example .env
docker compose up -d          # PostgreSQL 16 — app:5432, test-db:5433 (tmpfs)
./gradlew bootRun             # http://localhost:3000, 기동 시 Flyway 마이그레이션 적용
./gradlew test                # 통합 테스트 (test-db:5433 전용 컨테이너 사용)
```

Swagger UI: http://localhost:3000/swagger-ui.html

## 측정 (목표)

- 10만 분개 기준 잔액 조회 — 스냅샷 도입 전 N ms → 후 M ms (5주차 기록)
- 동시 내부이체 100건 — 차대변·잔액 합 유지 (4주차)
- 일마감 배치 대사 — 조작된 불일치 N건 검출 (6주차)

## 문서

- [요구사항·AC](docs/requirements.md)
- [설계 결정 기록 (ADR)](docs/adr)
