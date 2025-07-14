import { IsDateString, IsNumber } from 'class-validator';

export class CreateHistoricalAmountDto {
  @IsDateString()
  date: string;

  @IsNumber({}, { each: true })
  amounts: number[];
}