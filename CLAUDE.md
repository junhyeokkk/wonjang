# CLAUDE.md

이 파일은 이 저장소에서 작업하는 Claude Code(및 모든 AI 에이전트)를 위한 지침이다.
여기 적힌 **절대 원칙**은 어떤 경우에도 위반하지 않는다.

## 프로젝트 개요

- **wonjang (원장)**: 복식부기(double-entry) 원장 기반 결제·정산 시스템
- **기술 스택**: Java 21 · Spring Boot 3.4 · Spring Data JPA · Flyway · PostgreSQL 16 · Testcontainers · GitHub Actions
- **핵심 불변식**: "돈은 생기거나 사라지지 않는다. 모든 거래의 합은 0이다."
- **레거시**: NestJS 뼈대는 `legacy/` 브랜치에 보존. `main`은 Java·Spring으로 재구축.

### 로드맵

| 단계 | 내용 | 상태 |
|------|------|------|
| 1 | 원장 코어 (accounts / vouchers / entries) | 진행 중 |
| 2 | 역분개 · 시산표 | 예정 |
| 3 | 일마감 잔액 스냅샷 + 성능 측정 | 예정 |
| 4 | 멱등성 키 · 동시성 테스트 | 예정 |
| 5 | CI 녹색 · README · ADR 완성 | 예정 |

## 절대 원칙 (위반 금지)

1. **합 = 0 강제**: 모든 거래(transaction)의 entries 합은 반드시 0이어야 한다.
   균형이 맞지 않는 거래는 애플리케이션과 DB 양쪽에서 거부한다.
2. **Append-only**: entries는 추가만 한다. entries에 대한 `UPDATE`/`DELETE` 코드를
   **절대 작성하지 않는다**. 정정이 필요하면 오직 **역분개(reversal)** 로만 처리한다.
3. **금액은 항상 정수**: `float`/`double` 산술 연산 금지. 애플리케이션에서는 Java
   `long`, DB에서는 `BIGINT`를 사용한다. (근거: [ADR-001](docs/adr/001-integer-amount.md))
4. **스키마 변경은 마이그레이션으로만**: 스키마는 Flyway SQL 파일로만 바꾼다.
   `ddl-auto`는 **영원히 `validate`**다. ([src/main/resources/application.yml](src/main/resources/application.yml))
5. **잔액은 파생 데이터**: 잔액은 컬럼으로 저장하지 않는다. 항상 entries 집계로
   파생 계산한다 (`SUM(amount)`).

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
  ADR 작성을 **제안**한다. (예: [ADR-001](docs/adr/001-integer-amount.md))
- **커밋 메시지**: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`, `refactor:`,
  `test:`, `perf:`, `ci:`).
- **테스트**: 비즈니스 로직에는 Testcontainers 통합 테스트를 함께 작성한다. (`./gradlew test`)
- **코드 작성 전 승인**: 사용자가 명시적으로 요청한 경우, 구현 계획을 먼저 제안하고
  승인을 받은 뒤 코드를 작성한다.
- **분개 INSERT는 JdbcTemplate**: JPA `save()`는 deferred trigger와 flush 타이밍이 맞지 않아
  entries 삽입에는 `JdbcTemplate`을 사용한다. 조회는 JPA 사용 가능.

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
| 원장 | Ledger | 모든 돈의 이동이 기록되는 원본 장부 |
| 분개 / 거래 | Transaction | 하나의 사건 단위. 2개 이상 entry로 구성되며 합은 0 |
| 기입 | Entry | 특정 계좌의 금액 변화 한 줄 (+/- 부호 있는 정수) |
| 계좌 | Account | 돈이 머무는 논리적 위치 |
| 역분개 | Reversal | 잘못된 거래를 반대 방향 거래 추가로 정정하는 것 |
| 정산 | Settlement | 기간 거래를 집계해 가맹점별 지급액을 확정하는 것 |
| 대사 | Reconciliation | 외부 기관(PG) 기록과 내부 원장의 일치 여부 대조 |
| 멱등성 | Idempotency | 같은 요청이 여러 번 와도 한 번 처리한 것과 같음을 보장 |
