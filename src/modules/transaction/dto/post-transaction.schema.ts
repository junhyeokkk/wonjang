import { z } from 'zod';

/** BIGINT 식별자/금액은 JSON number 정밀도를 신뢰할 수 없어 문자열로 받는다. */
const integerString = z
  .string()
  .regex(/^-?\d+$/, 'must be an integer string');

// refine은 regex 실패 입력에도 실행되므로 BigInt 파싱(예외 위험) 대신
// "부호 포함 전부 0"인지 문자열로 판정한다. 예: '0', '-0', '000'.
const nonZeroAmount = integerString.refine(
  (value) => !/^-?0+$/.test(value),
  'amount must not be zero',
);

export const entryDraftSchema = z.object({
  accountId: integerString,
  amount: nonZeroAmount,
});

/**
 * 거래 생성 요청 스키마. 균형(합=0)은 도메인 규칙이라 여기서 검사하지 않고
 * PostingService의 assertBalanced + DB 트리거가 강제한다. 여기서는 형태만 검증한다.
 */
export const postTransactionSchema = z.object({
  type: z.string().min(1).max(30),
  description: z.string().max(1000).optional(),
  occurredAt: z
    .string()
    .refine(
      (value) => !Number.isNaN(Date.parse(value)),
      'occurredAt must be a valid ISO date-time string',
    ),
  entries: z
    .array(entryDraftSchema)
    .min(2, 'a transaction needs at least 2 entries'),
});

export type PostTransactionDto = z.infer<typeof postTransactionSchema>;
