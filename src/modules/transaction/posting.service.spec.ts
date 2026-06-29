import dataSource from '../../database/data-source';
import { Account } from '../account/account.entity';
import { Transaction } from './transaction.entity';
import { Entry } from './entry.entity';
import { PostingService } from './posting.service';
import { BalanceService } from './balance.service';
import { UnbalancedTransactionError } from './balance';

/**
 * posting 서비스 통합 테스트. 로컬 PostgreSQL이 떠 있어야 한다(docker compose up).
 * DATABASE_URL이 없으면(예: DB 없는 CI) 스킵한다.
 */
const describeDb = process.env.DATABASE_URL ? describe : describe.skip;

describeDb('PostingService (integration)', () => {
  let posting: PostingService;
  let balances: BalanceService;
  let accountA: string;
  let accountB: string;

  beforeAll(async () => {
    await dataSource.initialize();
    posting = new PostingService(dataSource);
    balances = new BalanceService(dataSource);

    const accountRepo = dataSource.getRepository(Account);
    await accountRepo.upsert(
      [
        { code: 'itest_a', name: '통합테스트 A', type: 'ASSET' },
        { code: 'itest_b', name: '통합테스트 B', type: 'REVENUE' },
      ],
      ['code'],
    );
    const rows = await accountRepo.find({
      where: [{ code: 'itest_a' }, { code: 'itest_b' }],
    });
    accountA = rows.find((r) => r.code === 'itest_a')!.id;
    accountB = rows.find((r) => r.code === 'itest_b')!.id;
  });

  afterEach(async () => {
    // entries는 append-only(DELETE 불가)이므로 TRUNCATE로 비운다.
    await dataSource.query('TRUNCATE entries, transactions RESTART IDENTITY');
  });

  afterAll(async () => {
    await dataSource.query(
      "DELETE FROM accounts WHERE code IN ('itest_a','itest_b')",
    );
    await dataSource.destroy();
  });

  it('persists a balanced transaction with its entries', async () => {
    const tx = await posting.post({
      type: 'test',
      description: 'balanced',
      occurredAt: new Date('2026-06-29T00:00:00Z'),
      entries: [
        { accountId: accountA, amount: 1000n },
        { accountId: accountB, amount: -1000n },
      ],
    });

    const persisted = await dataSource
      .getRepository(Transaction)
      .findOneBy({ id: tx.id });
    expect(persisted).not.toBeNull();

    const entries = await dataSource
      .getRepository(Entry)
      .findBy({ transactionId: tx.id });
    expect(entries).toHaveLength(2);
    expect(entries.map((e) => e.amount).sort()).toEqual([-1000n, 1000n]);
  });

  it('derives account balances from entries', async () => {
    await posting.post({
      type: 'test',
      occurredAt: new Date('2026-06-29T00:00:00Z'),
      entries: [
        { accountId: accountA, amount: 1000n },
        { accountId: accountB, amount: -1000n },
      ],
    });

    expect(await balances.getBalance(accountA)).toBe(1000n);
    expect(await balances.getBalance(accountB)).toBe(-1000n);
  });

  it('rejects an unbalanced transaction before touching the DB', async () => {
    await expect(
      posting.post({
        type: 'test',
        occurredAt: new Date('2026-06-29T00:00:00Z'),
        entries: [
          { accountId: accountA, amount: 1000n },
          { accountId: accountB, amount: -500n },
        ],
      }),
    ).rejects.toBeInstanceOf(UnbalancedTransactionError);

    // 아무것도 기록되지 않아야 한다.
    const count = await dataSource.getRepository(Transaction).count();
    expect(count).toBe(0);
  });

  it('returns 0 balance for an account with no entries', async () => {
    expect(await balances.getBalance(accountA)).toBe(0n);
  });
});
