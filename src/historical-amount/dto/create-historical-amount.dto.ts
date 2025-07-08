import { IsDateString, IsNotEmpty, IsNumber } from 'class-validator';

export class CreateHistoricalAmountDto {
  @IsNotEmpty()
  @IsDateString()
  date: string;

  @IsNotEmpty()
  @IsNumber()
  amount: number;
}