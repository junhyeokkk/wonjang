import { Controller, Get, Param } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Account } from './account.entity';
import { BalanceService } from '../transaction/balance.service';

@Controller('accounts')
export class AccountController {
  constructor(
    @InjectRepository(Account)
    private readonly accounts: Repository<Account>,
    private readonly balances: BalanceService,
  ) {}

  /** 계좌 목록. id로 거래를 기입하므로 식별자 조회용으로도 쓰인다. */
  @Get()
  async list() {
    const rows = await this.accounts.find({ order: { id: 'ASC' } });
    return rows.map((account) => ({
      id: account.id,
      code: account.code,
      name: account.name,
      type: account.type,
      currency: account.currency,
    }));
  }

  /** 파생 잔액. entries의 amount 집계이며 bigint는 JSON 안전성을 위해 문자열로 돌려준다. */
  @Get(':id/balance')
  async balance(@Param('id') id: string) {
    const balance = await this.balances.getBalance(id);
    return { accountId: id, balance: balance.toString() };
  }
}
