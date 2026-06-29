import {
  Column,
  CreateDateColumn,
  Entity,
  PrimaryGeneratedColumn,
} from 'typeorm';
import { bigintTransformer } from '../../database/bigint.transformer';

/**
 * 특정 계좌의 금액 변화 한 줄(+/- 부호 있는 정수). (entries 테이블 매핑)
 *
 * - append-only: UPDATE/DELETE 코드를 작성하지 않는다(절대 원칙 #2).
 *   DB의 prevent_entry_mutation 트리거가 이를 강제한다.
 * - 관계(transaction/account)는 현재 읽기 경로에서 불필요하므로 매핑하지 않고
 *   FK 스칼라 컬럼만 둔다(YAGNI). 잔액은 amount 집계로 파생한다(절대 원칙 #5).
 * - 조회 인덱스(account_id, id)는 InitLedgerSchema 마이그레이션에서 생성한다.
 */
@Entity({ name: 'entries' })
export class Entry {
  // BIGSERIAL → string 매핑.
  @PrimaryGeneratedColumn({ type: 'bigint' })
  id!: string;

  @Column({ name: 'transaction_id', type: 'bigint' })
  transactionId!: string;

  @Column({ name: 'account_id', type: 'bigint' })
  accountId!: string;

  // 부호 있는 정수 금액. BIGINT(string) <-> bigint 변환(절대 원칙 #3).
  // 0은 DB CHECK(amount <> 0)가 거부한다.
  @Column({ type: 'bigint', transformer: bigintTransformer })
  amount!: bigint;

  @CreateDateColumn({ name: 'created_at', type: 'timestamptz' })
  createdAt!: Date;
}
