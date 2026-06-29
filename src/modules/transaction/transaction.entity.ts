import {
  Column,
  CreateDateColumn,
  Entity,
  PrimaryGeneratedColumn,
} from 'typeorm';

/**
 * 하나의 사건 단위. 2개 이상 entry로 구성되며 entries의 amount 합은 0이다.
 * (InitLedgerSchema 마이그레이션으로 생성된 transactions 테이블 매핑)
 *
 * 잔액은 이 엔티티에 저장하지 않는다(절대 원칙 #5). entries는 append-only이며
 * 정정은 역분개(reversal) 거래 추가로만 처리한다(절대 원칙 #2).
 */
@Entity({ name: 'transactions' })
export class Transaction {
  // BIGSERIAL → TypeORM은 bigint를 string으로 매핑한다.
  @PrimaryGeneratedColumn({ type: 'bigint' })
  id!: string;

  // 거래 종류(payment / settlement / reversal 등). 값은 도메인이 커지며 늘어나므로
  // DB는 VARCHAR(30)로 열어 둔다.
  @Column({ type: 'varchar', length: 30 })
  type!: string;

  @Column({ type: 'text', nullable: true })
  description!: string | null;

  // 사건이 실제로 발생한 시각(기록 시각 created_at과 구분한다).
  @Column({ name: 'occurred_at', type: 'timestamptz' })
  occurredAt!: Date;

  @CreateDateColumn({ name: 'created_at', type: 'timestamptz' })
  createdAt!: Date;
}
