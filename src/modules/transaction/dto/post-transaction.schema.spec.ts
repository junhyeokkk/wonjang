import { postTransactionSchema } from './post-transaction.schema';

const validPayload = {
  type: 'payment',
  occurredAt: '2026-06-29T00:00:00Z',
  entries: [
    { accountId: '1', amount: '1000' },
    { accountId: '2', amount: '-1000' },
  ],
};

describe('postTransactionSchema', () => {
  it('accepts a well-formed payload', () => {
    expect(postTransactionSchema.safeParse(validPayload).success).toBe(true);
  });

  it('rejects non-integer amount strings', () => {
    const result = postTransactionSchema.safeParse({
      ...validPayload,
      entries: [
        { accountId: '1', amount: '10.5' },
        { accountId: '2', amount: '-1000' },
      ],
    });
    expect(result.success).toBe(false);
  });

  it('rejects a zero amount', () => {
    const result = postTransactionSchema.safeParse({
      ...validPayload,
      entries: [
        { accountId: '1', amount: '0' },
        { accountId: '2', amount: '-1000' },
      ],
    });
    expect(result.success).toBe(false);
  });

  it('rejects fewer than two entries', () => {
    const result = postTransactionSchema.safeParse({
      ...validPayload,
      entries: [{ accountId: '1', amount: '1000' }],
    });
    expect(result.success).toBe(false);
  });

  it('rejects an invalid occurredAt', () => {
    const result = postTransactionSchema.safeParse({
      ...validPayload,
      occurredAt: 'not-a-date',
    });
    expect(result.success).toBe(false);
  });
});
