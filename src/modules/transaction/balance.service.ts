import { Injectable } from '@nestjs/common';
import { DataSource } from 'typeorm';
import { Entry } from './entry.entity';

/**
 * 잔액 조회. 잔액은 컬럼으로 저장하지 않고 항상 entries의 amount 집계로
 * 파생 계산한다(절대 원칙 #5).
 */
@Injectable()
export class BalanceService {
  constructor(private readonly dataSource: DataSource) {}

  /** 한 계좌의 현재 잔액 = 해당 계좌 entries의 amount 합. */
  async getBalance(accountId: string): Promise<bigint> {
    const row = await this.dataSource
      .getRepository(Entry)
      .createQueryBuilder('entry')
      .select('COALESCE(SUM(entry.amount), 0)', 'balance')
      .where('entry.account_id = :accountId', { accountId })
      .getRawOne<{ balance: string }>();

    // SUM(BIGINT)는 NUMERIC을 반환하므로 드라이버가 string으로 준다 → bigint로 환산.
    return BigInt(row?.balance ?? '0');
  }
}
