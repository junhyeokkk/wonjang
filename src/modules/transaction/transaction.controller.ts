import {
  BadRequestException,
  Body,
  Controller,
  Post,
  UsePipes,
} from '@nestjs/common';
import { PostingService } from './posting.service';
import { ZodValidationPipe } from '../../common/zod-validation.pipe';
import {
  postTransactionSchema,
  type PostTransactionDto,
} from './dto/post-transaction.schema';
import { InvalidTransactionError, UnbalancedTransactionError } from './balance';

@Controller('transactions')
export class TransactionController {
  constructor(private readonly posting: PostingService) {}

  /** 균형 거래 하나를 기록한다. 불균형/형식 오류는 400으로 응답한다. */
  @Post()
  @UsePipes(new ZodValidationPipe(postTransactionSchema))
  async create(@Body() dto: PostTransactionDto) {
    try {
      const transaction = await this.posting.post({
        type: dto.type,
        description: dto.description,
        occurredAt: new Date(dto.occurredAt),
        entries: dto.entries.map((entry) => ({
          accountId: entry.accountId,
          amount: BigInt(entry.amount),
        })),
      });

      return {
        id: transaction.id,
        type: transaction.type,
        occurredAt: transaction.occurredAt,
      };
    } catch (error) {
      // 도메인 규칙 위반(균형/기입수/0원)은 클라이언트 잘못이므로 400으로 매핑한다.
      if (
        error instanceof UnbalancedTransactionError ||
        error instanceof InvalidTransactionError
      ) {
        throw new BadRequestException(error.message);
      }
      throw error;
    }
  }
}
