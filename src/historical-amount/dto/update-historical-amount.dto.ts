import { PartialType } from '@nestjs/mapped-types';
import { CreateHistoricalAmountDto } from './create-historical-amount.dto';

export class UpdateHistoricalAmountDto extends PartialType(
  CreateHistoricalAmountDto,
) {}
