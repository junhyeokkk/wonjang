# CLAUDE.md

이 파일은 이 저장소에서 작업하는 Claude Code(및 모든 AI 에이전트)를 위한 지침이다.
여기 적힌 **절대 원칙**은 어떤 경우에도 위반하지 않는다.

## 프로젝트 개요

- **wonjang (원장)**: 전표를 받아 분개로 기록하고, 계정별 원장·잔액·시산표를 제공하는
  **복식부기 원장 서버**. API만 제공한다 (UI는 Swagger로 대신).
- **기술 스택**: Java 21 · Spring Boot 3.4 · Spring Data JPA · Flyway · PostgreSQL 16 · Testcontainers · GitHub Actions
- **핵심 불변식**: "돈은 생기거나 사라지지 않는다. 모든 전표의 차변 합 = 대변 합이다."
- **명세 정본**: Notion「원장 (wonjang) - 복식부기 원장 서버」 하위 문서
  (00 개요 · 02 요구사항(AC) · 03 데이터 모델 · 04 API 명세 · 06 테스트 전략 · 07 주차별 계획).
  설계 결정의 정본은 [docs/adr](docs/adr).
- **비범위**: 로그인·권한(`X-User-Id` 헤더만), UI, 다중 통화, 세무 규칙, 마이크로서비스·MQ.
- **레거시**: NestJS 뼈대는 `legacy/` 브랜치에 보존. `main`은 Java·Spring으로 재구축.

### 로드맵 (2026.11 ~ 2026.12, Notion 07 기준)

| 주차 | 내용 | AC | 상태 |
|------|------|----|------|
| 0 | 문서 정리 · CI · 테스트 베이스 | — | 진행 중 |
| 1 | 공통(에러 형식·X-User-Id) + F1 계정과목 | 01~03, 25, 26 | 예정 |
| 2 | F2 전표 입력 (서비스 + DB 균형 검증) | 04~09 | 예정 |
| 3 | F3 원장·잔액 · F4 역분개 · F5 시산표 (MVP) | 10~18 | 예정 |
| 4 | 동시성 테스트 · F7 멱등성 키 | 22~24 | 예정 |
| 5 | F6 일마감 스냅샷 · 성능 측정 | 19~21 | 예정 |
| 6 | README · ADR 확정 · 완료 기준 체크 | — | 예정 |

## 절대 원칙 (위반 금지)

1. **균형 강제**: 모든 전표(voucher)는 분개(entry) 2줄 이상이며 **차변 합 = 대변 합**이어야 한다.
   불균형 전표는 애플리케이션(서비스 검증)과 DB(deferred constraint trigger) 양쪽에서 거부한다.
   ([ADR-002](docs/adr/002-balance-deferred-trigger.md))
2. **Append-only**: entries는 추가만 한다. entries에 대한 `UPDATE`/`DELETE` 코드와 API를
   **절대 작성하지 않는다**. 정정은 오직 **역분개(reversal)** 로만 처리한다.
   ([ADR-001](docs/adr/001-append-only-reversal.md))
3. **금액은 양의 정수, 방향은 side**: 금액은 원 단위 `amount > 0`, 방향은 `side`(`DEBIT`/`CREDIT`)로만
   표현한다. 음수 금액 금지. `float`/`double`/`BigDecimal` 산술 금지 — Java `long`, DB `BIGINT`.
   JSON 소수 입력은 잘라내지 말고 거부한다. ([ADR-005](docs/adr/005-integer-amount.md))
4. **스키마 변경은 마이그레이션으로만**: 스키마는 Flyway SQL 파일로만 바꾼다. 이미 커밋된
   마이그레이션 파일은 수정하지 않고 새 버전을 추가한다.
   `ddl-auto`는 **영원히 `validate`**다. ([src/main/resources/application.yml](src/main/resources/application.yml))
5. **잔액은 파생 데이터**: 계정에 잔액 컬럼을 두지 않는다. 잔액은 entries 집계
   (`SUM(DEBIT) − SUM(CREDIT)`, 계정 유형별 부호)로 계산한다. 유일한 예외인 `balance_snapshots`는
   entries에서 언제든 재생성 가능한 **파생 캐시**다. ([ADR-004](docs/adr/004-balance-snapshot.md))

## 개발 환경

- **DB**: `docker compose up -d` 으로 PostgreSQL 16 기동. 접속 정보는 `.env` 참고
  (`.env.example` 복사해서 사용). 호스트 포트는 **5432**.
- **빌드**: Gradle Wrapper(`./gradlew` / `gradlew.bat`). Java 21 필요.
- **마이그레이션**: 애플리케이션 기동 시 Flyway가 자동 적용. SQL 파일 위치:
  `src/main/resources/db/migration/V{N}__{설명}.sql`
- **테스트 DB**: Testcontainers가 PostgreSQL 컨테이너를 자동 기동. Docker 필요.
- **Swagger UI**: 기동 후 `http://localhost:3000/swagger-ui.html`

## 작업 규칙

- **ADR**: 설계 결정이 생기면 [docs/adr/000-template.md](docs/adr/000-template.md) 형식으로
  ADR 작성을 **제안**한다. 번호는 Notion 05 ADR 목록과 맞춘다.
- **커밋 메시지**: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`,
  `test:`, `perf:`, `ci:`).
- **테스트**: 비즈니스 로직에는 Testcontainers 통합 테스트를 함께 작성한다. (`./gradlew test`)
  H2 등 인메모리 DB로 대체하지 않는다 — 트리거·제약 검증이 이 프로젝트의 핵심이다.
  AC 하나 = 테스트 하나, 테스트 이름에 AC 번호를 넣는다 (`ac05_unbalancedVoucher_rejectedWith422`).
- **코드 작성 전 승인**: 사용자가 명시적으로 요청한 경우, 구현 계획을 먼저 제안하고
  승인을 받은 뒤 코드를 작성한다.
- **분개 INSERT는 JdbcTemplate**: JPA `save()`는 deferred trigger와 flush 타이밍이 맞지 않아
  entries 삽입에는 `JdbcTemplate`을 사용한다. 조회는 JPA 사용 가능.
- **계층**: Controller → Service → Repository. 요청/응답 DTO는 `record`.
  도메인 예외 → 에러 코드 매핑은 `@RestControllerAdvice` 한 곳에서.
- **API 공통**: 쓰기 API는 `X-User-Id` 헤더 필수(→ `created_by`). 에러 응답은 항상
  `{ "code", "message", "detail" }`. 에러 코드 표는 Notion 04 API 명세가 정본.

## 자주 쓰는 명령

```bash
docker compose up -d        # PostgreSQL 기동
./gradlew bootRun           # 개발 서버 기동 (포트 3000)
./gradlew test              # 전체 테스트 (Testcontainers 필요)
./gradlew test --tests "*.{ClassName}" # 단일 테스트 클래스 실행
./gradlew build             # 프로덕션 빌드
./gradlew dependencies      # 의존성 트리 확인
```

Windows:
```powershell
.\gradlew.bat bootRun
.\gradlew.bat test
```

## 용어집

| 용어 | 영문 | 의미 |
|------|------|------|
| 원장 | Ledger | 계정 하나의 분개를 시간순으로 모은 것 + 누적 잔액 |
| 계정(과목) | Account | 돈이 머무는 논리적 위치. 유형: ASSET / LIABILITY / EQUITY / REVENUE / EXPENSE |
| 전표 | Voucher | 하나의 거래 단위(헤더). 2줄 이상의 분개로 구성되며 차변 합 = 대변 합 |
| 분개 | Entry | 전표의 한 줄: 계정 · side(DEBIT/CREDIT) · 양의 정수 금액 |
| 차변 / 대변 | Debit / Credit | 분개의 방향. 자산·비용은 차변이 증가, 부채·자본·수익은 대변이 증가 |
| 역분개 | Reversal | 원본은 그대로 두고, 차변↔대변을 반전한 새 전표를 추가해 정정하는 것 |
| 시산표 | Trial Balance | 기준일까지 전 계정의 차변 합·대변 합 목록. 총합이 같아야 한다 |
| 일마감 | Closing | 기준일까지 계정별 누적 합계를 스냅샷으로 확정하고, 그 이전 날짜 입력을 잠그는 것 |
| 멱등성 | Idempotency | 같은 `Idempotency-Key` 요청이 여러 번 와도 전표는 하나만 생기는 성질 |
