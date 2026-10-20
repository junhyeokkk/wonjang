# 로드맵

**정본은 Notion「07 주차별 계획」.** 이 문서는 로컬 작업용 요약·미러.
AC 번호의 정본은 Notion「02 요구사항(AC)」.

---

## 주차별 계획 (2026.11 ~ 2026.12)

| 주차 | 범위 | AC | 상태 |
|------|------|----|------|
| 0 | 문서 정리 · CI · 테스트 베이스 | — | 완료 |
| 1 | 공통(에러 형식·X-User-Id) + **F1 계정과목** | 01~03, 25, 26 | **진행 중** |
| 2 | **F2 전표 입력** (서비스 + DB 균형 검증) | 04~09 | 예정 |
| 3 | **F3 원장·잔액** · **F4 역분개** · **F5 시산표** (MVP) | 10~18 | 예정 |
| 4 | 동시성 테스트 · **F7 멱등성 키** | 22~24 | 예정 |
| 5 | **F6 일마감 스냅샷** · 성능 측정 | 19~21 | 예정 |
| 6 | README · ADR 확정 · 완료 기준 체크 | — | 예정 |

---

## 피처 정의

### F1 — 계정과목 (Account)
돈이 머무는 논리적 위치. 유형: `ASSET` / `LIABILITY` / `EQUITY` / `REVENUE` / `EXPENSE`.
- `POST /api/accounts` — 생성 (코드 중복 → 409)
- `GET /api/accounts` — 전체 목록
- `PATCH /api/accounts/{id}/deactivate` — 비활성화 (삭제 금지, append-only)

### F2 — 전표 입력 (Voucher Posting)
하나의 거래 = 전표(헤더) + 분개(2줄 이상). **차변 합 = 대변 합** 불변식.
- 서비스 레벨 검증 + DB deferred trigger 양쪽에서 균형 강제 ([ADR-002](adr/002-balance-deferred-trigger.md))
- 분개 INSERT는 `JdbcTemplate` (JPA save와 deferred trigger 타이밍 안 맞음 — CLAUDE.md 규칙)
- 금액은 양의 정수(`long`), 방향은 `side`(DEBIT/CREDIT) ([ADR-005](adr/005-integer-amount.md))

### F3 — 원장·잔액 (Ledger / Balance)
계정 하나의 분개를 시간순으로 모은 것 + 누적 잔액.
- `GET /api/accounts/{id}/ledger?from=&to=` — 분개 목록 + 러닝 밸런스
- `GET /api/accounts/{id}/balance?asOf=` — 특정 시점 잔액
- 잔액은 **파생 데이터**. accounts에 잔액 컬럼 두지 않음 ([ADR-004](adr/004-balance-snapshot.md))

### F4 — 역분개 (Reversal)
원본은 그대로, 차변↔대변 반전한 새 전표를 추가해 정정.
- `POST /api/vouchers/{id}/reverse`
- 원본 수정·삭제 금지 ([ADR-001](adr/001-append-only-reversal.md))

### F5 — 시산표 (Trial Balance)
기준일까지 전 계정의 차변 합·대변 합 목록. 총합이 같아야 함.
- `GET /api/trial-balance?asOf=`

### F6 — 일마감 (Daily Closing)
기준일까지 계정별 누적 합계를 스냅샷으로 확정 + 그 이전 날짜 입력 잠금.
- `POST /api/closings` — 일마감 실행
- `balance_snapshots` 테이블에 저장. entries에서 언제든 재생성 가능한 캐시 ([ADR-004](adr/004-balance-snapshot.md))

### F7 — 멱등성 키 (Idempotency Key)
같은 `Idempotency-Key` 헤더의 요청이 여러 번 와도 전표는 하나만 생성.
- `POST /api/vouchers` 요청에 `Idempotency-Key: <uuid>` 필수
- 동시 요청 100개 → 전표 1개만 생성되는 동시성 테스트 필수

---

## 작업 순서 원칙

- **AC 하나 = 테스트 하나**, 테스트 이름에 AC 번호 포함 (`ac05_unbalancedVoucher_rejectedWith422`)
- 각 피처는 **ADR → 마이그레이션 → 엔티티 → 서비스 → 컨트롤러 → 테스트** 순서
- 균형·append-only·금액 정수화 등 CLAUDE.md 절대 원칙은 모든 피처에 적용

---

## 완료 기준 (6주차 체크리스트)

- [ ] 모든 AC에 대응하는 통합 테스트 통과
- [ ] 동시성 테스트: 멱등성·균형 trigger 멀티스레드 환경에서 검증
- [ ] 성능 측정: `POST /api/vouchers` p95 지연 측정 (목표는 Notion 06 테스트 전략)
- [ ] ADR 전부 Accepted 상태
- [ ] README에 아키텍처 다이어그램·실행법·API 사용 예시
- [ ] CI(GitHub Actions)에서 `./gradlew test` 녹색
