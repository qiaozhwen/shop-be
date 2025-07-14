import { IsDateString, IsNotEmpty, IsNumber, IsOptional } from 'class-validator';

export class CreateHistoricalAmountDto {
  @IsOptional()
  @IsDateString()
  date?: string;

  @IsNotEmpty()
  @IsNumber()
  amount: number;
}