import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { Transaction } from './transaction.entity';
import { Entry } from './entry.entity';
import { Account } from '../account/account.entity';
import { PostingService } from './posting.service';
import { BalanceService } from './balance.service';
import { TransactionController } from './transaction.controller';
import { AccountController } from '../account/account.controller';

/**
 * 원장 코어(로드맵 1단계): 거래 기록(posting)과 잔액 파생 조회 + HTTP API.
 */
@Module({
  imports: [TypeOrmModule.forFeature([Transaction, Entry, Account])],
  controllers: [TransactionController, AccountController],
  providers: [PostingService, BalanceService],
  exports: [PostingService, BalanceService],
})
export class LedgerModule {}
