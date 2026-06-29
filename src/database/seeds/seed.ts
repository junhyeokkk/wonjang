import 'reflect-metadata';
import dataSource from '../data-source';
import { Account, AccountType } from '../../modules/account/account.entity';

interface SeedAccount {
  code: string;
  name: string;
  type: AccountType;
}

/**
 * 시스템 기본 계좌. code 기준으로 멱등하게 upsert 한다.
 * 여기 정의를 바꾸면 seed 재실행 시 name/type이 갱신된다(추가 INSERT 아님).
 */
export const SEED_ACCOUNTS: readonly SeedAccount[] = [
  { code: 'pg_receivable', name: 'PG 미수금', type: 'ASSET' },
  { code: 'fee_revenue', name: '수수료 수익', type: 'REVENUE' },
  {
    code: 'merchant_payable:M001',
    name: '가맹점 미지급금 (M001)',
    type: 'LIABILITY',
  },
];

/**
 * 주어진 DataSource에 기본 계좌를 멱등하게 적재한다
 * code UNIQUE 제약을 충돌 키로 사용하므로, 여러 번 실행해도 행이 늘지 않는다
 */
export async function seedAccounts(ds = dataSource): Promise<void> {
  const repo = ds.getRepository(Account);
  await repo.upsert([...SEED_ACCOUNTS], ['code']);
}

async function run(): Promise<void> {
  await dataSource.initialize();
  try {
    await seedAccounts();
    const count = await dataSource.getRepository(Account).count();
    // eslint-disable-next-line no-console
    console.log(`[seed] accounts upserted. total accounts = ${count}`);
  } finally {
    await dataSource.destroy();
  }
}

// 스크립트로 직접 실행될 때만 동작 (import 시에는 실행하지 않음)
if (require.main === module) {
  run().catch((error: unknown) => {
    // eslint-disable-next-line no-console
    console.error('[seed] failed:', error);
    process.exit(1);
  });
}
