import {
  ArgumentMetadata,
  BadRequestException,
  Injectable,
  PipeTransform,
} from '@nestjs/common';
import { ZodError, type ZodType } from 'zod';

/**
 * Zod 스키마로 요청 본문/파라미터를 검증하는 파이프.
 * 시스템 경계에서 외부 입력을 신뢰하지 않고 스키마로 검증한다(coding-style).
 * 검증 실패는 400으로 매핑하고 위반 항목을 함께 돌려준다.
 */
@Injectable()
export class ZodValidationPipe<T> implements PipeTransform {
  constructor(private readonly schema: ZodType<T>) {}

  transform(value: unknown, _metadata: ArgumentMetadata): T {
    try {
      return this.schema.parse(value);
    } catch (error) {
      if (error instanceof ZodError) {
        throw new BadRequestException({
          message: 'validation failed',
          issues: error.issues.map((issue) => ({
            path: issue.path.join('.'),
            message: issue.message,
          })),
        });
      }
      throw error;
    }
  }
}
