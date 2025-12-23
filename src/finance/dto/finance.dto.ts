import {
  IsString,
  IsOptional,
  IsNumber,
  IsEnum,
  IsDateString,
  Min,
} from 'class-validator';
import { FinanceType, FinanceCategory } from '../finance.entity';

export class CreateFinanceDto {
  @IsEnum(FinanceType)
  type: FinanceType;

  @IsEnum(FinanceCategory)
  category: FinanceCategory;

  @IsNumber()
  @Min(0.01)
  amount: number;

  @IsOptional()
  @IsString()
  paymentMethod?: string;

  @IsOptional()
  @IsString()
  relatedType?: string;

  @IsOptional()
  @IsNumber()
  relatedId?: number;

  @IsOptional()
  @IsString()
  description?: string;

  @IsOptional()
  @IsString()
  remark?: string;

  @IsOptional()
  @IsDateString()
  recordAt?: string;
}

export class QueryFinanceDto {
  @IsOptional()
  @IsEnum(FinanceType)
  type?: FinanceType;

  @IsOptional()
  @IsEnum(FinanceCategory)
  category?: FinanceCategory;

  @IsOptional()
  @IsDateString()
  startDate?: string;

  @IsOptional()
  @IsDateString()
  endDate?: string;

  @IsOptional()
  @IsNumber()
  page?: number;

  @IsOptional()
  @IsNumber()
  pageSize?: number;
}

