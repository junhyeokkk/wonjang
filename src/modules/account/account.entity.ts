import {
  Column,
  CreateDateColumn,
  Entity,
  PrimaryGeneratedColumn,
} from 'typeorm';

/**
 * 계정의 회계적 성격. DB는 VARCHAR(20)이며, 허용값은 이 union 타입으로
 * 애플리케이션 레벨에서 강제한다. (근거: docs/adr/002-account-type-representation.md)
 */
export type AccountType =
  | 'ASSET'
  | 'LIABILITY'
  | 'REVENUE'
  | 'EXPENSE'
  | 'EQUITY';

/**
 * 돈이 머무는 논리적 위치. (InitLedgerSchema 마이그레이션으로 생성된 accounts 테이블 매핑)
 * - `code`: 기계용 업무 식별자 (UNIQUE) 예) pg_receivable, merchant_payable:M001
 * - `name`: 표시용 이름 (UNIQUE)
 * - 금액은 이 엔티티에 저장하지 않는다. 잔액은 entries 집계로 파생 계산한다.
 */
@Entity({ name: 'accounts' })
export class Account {
  // BIGSERIAL → TypeORM은 bigint를 string으로 매핑한다.
  @PrimaryGeneratedColumn({ type: 'bigint' })
  id!: string;

  @Column({ type: 'varchar', length: 50, unique: true })
  code!: string;

  @Column({ type: 'varchar', length: 100, unique: true })
  name!: string;

  @Column({ type: 'varchar', length: 20 })
  type!: AccountType;

  @Column({ type: 'char', length: 3, default: 'KRW' })
  currency!: string;

  @CreateDateColumn({ name: 'created_at', type: 'timestamptz' })
  createdAt!: Date;
}
