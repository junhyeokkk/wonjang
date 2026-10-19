# ADR-004: 잔액은 일마감 스냅샷 + 이후 분개 증분 합산으로 계산한다

- 날짜: 2026-10-19
- 상태: 제안됨 (5주차 측정 후 승인)

## 맥락 (Context)

append-only([ADR-001](001-append-only-reversal.md))라 분개가 계속 쌓인다. 잔액 조회마다
전체 `SUM`을 하면 분개 수에 비례해 느려진다.

## 검토한 선택지 (Options)

1. **매번 전체 SUM** — 단순하고 항상 정확하지만 데이터가 늘면 느려진다.
2. **accounts에 현재 잔액 컬럼** — 조회는 O(1)이지만 입력마다 같은 행을 UPDATE해 쓰기 경합이
   생긴다([ADR-003](003-concurrency-lock-strategy.md)과 충돌). 절대 원칙 #5 위반.
3. **일마감 스냅샷 + 이후 분개만 SUM** — 쓰기는 그대로 두고 읽기만 빨라진다.

## 결정 (Decision)

**3번.** `balance_snapshots(account_id, as_of_date, debit_total, credit_total)`에 마감일까지의
누적 차변·대변을 저장한다 ([V4](../../src/main/resources/db/migration/V4__snapshots.sql)).

```
잔액(자산·비용)       = (snap.debit_total − snap.credit_total)
                      + Σ(snap 이후 ~ asOf DEBIT) − Σ(snap 이후 ~ asOf CREDIT)
잔액(부채·자본·수익)  = 부호 반대
```

스냅샷은 잔액 "컬럼"이 아니라 entries에서 언제든 다시 만들 수 있는 **파생 캐시**다.
원본은 여전히 entries다.

## 결과 (Consequences)

- 얻는 것: 조회 비용이 전체 분개 수가 아니라 "마지막 마감 이후 분개 수"에 비례.
- 트레이드오프: 마감일 이전 날짜로 전표가 들어오면 스냅샷이 틀린다 → 마감 잠금으로
  마감일 이하 날짜 입력을 거부한다 (AC-21, `PERIOD_CLOSED`).
- 검증: 스냅샷+증분 잔액 == 전체 SUM 잔액 동등성 테스트 (AC-20, 계정 유형 5개 전부).
  10만 분개 기준 도입 전/후 응답시간을 측정해 README에 기록한다.
- 재검토 조건: 측정 결과 개선 폭이 미미하면 인덱스(`vouchers(voucher_date)` 등)만으로 충분한지 비교한다.
