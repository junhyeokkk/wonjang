/**
 * 복식부기 균형 규칙의 순수(부수효과 없는) 검증 로직.
 *
 * 절대 원칙 #1 "합 = 0 강제"의 **애플리케이션 레벨** 1차 방어선이다.
 * DB 레벨 2차 방어선은 EnforceBalancedTransaction 마이그레이션의
 * deferred constraint trigger가 담당한다. 두 곳 모두에서 강제한다.
 */

/** 거래에 들어갈 기입 한 줄의 입력값. 금액은 부호 있는 정수(bigint). */
export interface EntryDraft {
  accountId: string;
  amount: bigint;
}

/** 균형은 맞지만 거래 자체가 성립할 수 없는 경우(기입 부족, 0원 기입 등). */
export class InvalidTransactionError extends Error {
  constructor(message: string) {
    super(message);
    this.name = 'InvalidTransactionError';
  }
}

/** 기입 합이 0이 아니라 균형이 깨진 경우. 잔여 합(sum)을 함께 보고한다. */
export class UnbalancedTransactionError extends Error {
  constructor(public readonly sum: bigint) {
    super(`transaction is not balanced: sum of entries = ${sum}`);
    this.name = 'UnbalancedTransactionError';
  }
}

/** 복식부기는 최소 2줄(차변/대변)이 있어야 한다. */
const MIN_ENTRIES = 2;

/** 기입들의 부호 있는 합을 구한다. */
export function sumAmounts(entries: readonly EntryDraft[]): bigint {
  return entries.reduce((acc, entry) => acc + entry.amount, 0n);
}

/**
 * 거래가 균형 규칙을 만족하는지 검증한다. 위반 시 예외를 던진다.
 * - 기입은 2줄 이상
 * - 모든 기입 금액은 0이 아님 (DB의 `amount <> 0` CHECK와 일치)
 * - 전체 합은 정확히 0
 */
export function assertBalanced(entries: readonly EntryDraft[]): void {
  if (entries.length < MIN_ENTRIES) {
    throw new InvalidTransactionError(
      `a transaction needs at least ${MIN_ENTRIES} entries, got ${entries.length}`,
    );
  }

  for (const entry of entries) {
    if (entry.amount === 0n) {
      throw new InvalidTransactionError('entry amount must not be zero');
    }
  }

  const sum = sumAmounts(entries);
  if (sum !== 0n) {
    throw new UnbalancedTransactionError(sum);
  }
}
