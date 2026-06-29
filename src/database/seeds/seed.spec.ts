import { SEED_ACCOUNTS } from './seed';
import { AccountType } from '../../modules/account/account.entity';

const VALID_TYPES: readonly AccountType[] = [
  'ASSET',
  'LIABILITY',
  'REVENUE',
  'EXPENSE',
  'EQUITY',
];

describe('SEED_ACCOUNTS', () => {
  it('defines exactly the three required base accounts', () => {
    expect(SEED_ACCOUNTS).toHaveLength(3);
    expect(SEED_ACCOUNTS.map((a) => a.code).sort()).toEqual(
      ['fee_revenue', 'merchant_payable:M001', 'pg_receivable'].sort(),
    );
  });

  it('maps each account to the expected type', () => {
    const byCode = new Map(SEED_ACCOUNTS.map((a) => [a.code, a.type]));
    expect(byCode.get('pg_receivable')).toBe('ASSET');
    expect(byCode.get('fee_revenue')).toBe('REVENUE');
    expect(byCode.get('merchant_payable:M001')).toBe('LIABILITY');
  });

  it('uses only valid account types', () => {
    for (const account of SEED_ACCOUNTS) {
      expect(VALID_TYPES).toContain(account.type);
    }
  });

  it('has unique codes (idempotent upsert key)', () => {
    const codes = SEED_ACCOUNTS.map((a) => a.code);
    expect(new Set(codes).size).toBe(codes.length);
  });

  it('has a non-empty display name for every account', () => {
    for (const account of SEED_ACCOUNTS) {
      expect(account.name.trim().length).toBeGreaterThan(0);
    }
  });
});
