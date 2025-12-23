import {
  IsString,
  IsOptional,
  IsNumber,
  IsEnum,
  IsDateString,
  Min,
} from 'class-validator';
import { InboundType, OutboundType } from '../inventory.entity';

export class CreateInboundDto {
  @IsNumber()
  productId: number;

  @IsNumber()
  @Min(1)
  quantity: number;

  @IsOptional()
  @IsNumber()
  weight?: number;

  @IsOptional()
  @IsNumber()
  unitPrice?: number;

  @IsOptional()
  @IsNumber()
  supplierId?: number;

  @IsOptional()
  @IsString()
  batchNo?: string;

  @IsOptional()
  @IsEnum(InboundType)
  type?: InboundType;

  @IsOptional()
  @IsString()
  remark?: string;
}

export class CreateOutboundDto {
  @IsNumber()
  productId: number;

  @IsNumber()
  @Min(1)
  quantity: number;

  @IsOptional()
  @IsNumber()
  weight?: number;

  @IsEnum(OutboundType)
  type: OutboundType;

  @IsOptional()
  @IsNumber()
  orderId?: number;

  @IsOptional()
  @IsString()
  reason?: string;
}

export class QueryInboundDto {
  @IsOptional()
  @IsNumber()
  productId?: number;

  @IsOptional()
  @IsNumber()
  supplierId?: number;

  @IsOptional()
  @IsEnum(InboundType)
  type?: InboundType;

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

export class QueryOutboundDto {
  @IsOptional()
  @IsNumber()
  productId?: number;

  @IsOptional()
  @IsEnum(OutboundType)
  type?: OutboundType;

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

export class UpdateInventoryDto {
  @IsOptional()
  @IsNumber()
  quantity?: number;

  @IsOptional()
  @IsNumber()
  minQuantity?: number;

  @IsOptional()
  @IsNumber()
  maxQuantity?: number;

  @IsOptional()
  @IsString()
  notes?: string;
}

