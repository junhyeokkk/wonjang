import { Injectable } from '@nestjs/common';
import { DataSource } from 'typeorm';
import { Transaction } from './transaction.entity';
import { Entry } from './entry.entity';
import { assertBalanced, type EntryDraft } from './balance';

/** 하나의 균형 거래를 기록하기 위한 입력. */
export interface PostTransactionInput {
  type: string;
  description?: string;
  occurredAt: Date;
  entries: readonly EntryDraft[];
}

/**
 * 분개(posting): 균형 잡힌 거래 하나를 원장에 기록한다.
 *
 * 절대 원칙 #1을 2중으로 강제한다:
 *  1) 앱 레벨 — assertBalanced로 합=0/기입수/0원 검증 후에만 INSERT
 *  2) DB 레벨 — 단일 DB 트랜잭션으로 커밋, 커밋 시 deferred trigger가 합=0 재강제
 */
@Injectable()
export class PostingService {
  constructor(private readonly dataSource: DataSource) {}

  async post(input: PostTransactionInput): Promise<Transaction> {
    // 1차 방어선: 잘못된 거래는 DB에 닿기 전에 거부한다.
    assertBalanced(input.entries);

    return this.dataSource.transaction(async (manager) => {
      const txRepo = manager.getRepository(Transaction);
      const entryRepo = manager.getRepository(Entry);

      const transaction = await txRepo.save(
        txRepo.create({
          type: input.type,
          description: input.description ?? null,
          occurredAt: input.occurredAt,
        }),
      );

      const entries = input.entries.map((draft) =>
        entryRepo.create({
          transactionId: transaction.id,
          accountId: draft.accountId,
          amount: draft.amount,
        }),
      );
      await entryRepo.save(entries);

      // 트랜잭션 콜백이 정상 반환하면 커밋 → 이 시점에 deferred trigger가
      // 합=0을 최종 검증한다(2차 방어선). 위반 시 여기서 예외가 발생한다.
      return transaction;
    });
  }
}
