import {
  assertBalanced,
  sumAmounts,
  InvalidTransactionError,
  UnbalancedTransactionError,
  type EntryDraft,
} from './balance';

describe('sumAmounts', () => {
  it('returns 0 for an empty list', () => {
    expect(sumAmounts([])).toBe(0n);
  });

  it('sums signed bigint amounts', () => {
    const entries: EntryDraft[] = [
      { accountId: '1', amount: 1000n },
      { accountId: '2', amount: -300n },
      { accountId: '3', amount: -700n },
    ];
    expect(sumAmounts(entries)).toBe(0n);
  });
});

describe('assertBalanced', () => {
  it('accepts a balanced two-entry transaction', () => {
    const entries: EntryDraft[] = [
      { accountId: '1', amount: 1000n },
      { accountId: '2', amount: -1000n },
    ];
    expect(() => assertBalanced(entries)).not.toThrow();
  });

  it('accepts a balanced multi-entry transaction', () => {
    const entries: EntryDraft[] = [
      { accountId: '1', amount: 1000n },
      { accountId: '2', amount: -970n },
      { accountId: '3', amount: -30n },
    ];
    expect(() => assertBalanced(entries)).not.toThrow();
  });

  it('rejects an unbalanced transaction with the residual sum', () => {
    const entries: EntryDraft[] = [
      { accountId: '1', amount: 1000n },
      { accountId: '2', amount: -500n },
    ];
    expect(() => assertBalanced(entries)).toThrow(UnbalancedTransactionError);
    try {
      assertBalanced(entries);
    } catch (error) {
      expect((error as UnbalancedTransactionError).sum).toBe(500n);
    }
  });

  it('rejects a transaction with fewer than two entries', () => {
    expect(() => assertBalanced([{ accountId: '1', amount: 0n }])).toThrow(
      InvalidTransactionError,
    );
    expect(() => assertBalanced([])).toThrow(InvalidTransactionError);
  });

  it('rejects a transaction containing a zero amount', () => {
    const entries: EntryDraft[] = [
      { accountId: '1', amount: 0n },
      { accountId: '2', amount: 0n },
    ];
    expect(() => assertBalanced(entries)).toThrow(InvalidTransactionError);
  });
});
