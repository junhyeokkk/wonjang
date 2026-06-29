import { ValueTransformer } from 'typeorm';

/**
 * DB의 BIGINT(드라이버가 string으로 반환) <-> 애플리케이션의 bigint 변환.
 *
 * 절대 원칙 #3 "금액은 항상 정수": 애플리케이션에서는 TypeScript bigint로만
 * 금액을 다루고, number 변환을 거치지 않는다(정밀도 손실 방지).
 */
export const bigintTransformer: ValueTransformer = {
  to: (value: bigint): string => value.toString(),
  from: (value: string): bigint => BigInt(value),
};
