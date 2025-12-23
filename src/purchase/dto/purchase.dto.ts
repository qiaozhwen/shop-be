import {
  IsString,
  IsOptional,
  IsNumber,
  IsArray,
  ValidateNested,
  IsEnum,
  IsDateString,
  Min,
} from 'class-validator';
import { Type } from 'class-transformer';
import { PurchaseStatus, PaymentStatus } from '../purchase.entity';

export class PurchaseItemDto {
  @IsNumber()
  productId: number;

  @IsString()
  productName: string;

  @IsNumber()
  @Min(1)
  quantity: number;

  @IsOptional()
  @IsNumber()
  weight?: number;

  @IsNumber()
  @Min(0)
  unitPrice: number;
}

export class CreatePurchaseDto {
  @IsNumber()
  supplierId: number;

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => PurchaseItemDto)
  items: PurchaseItemDto[];

  @IsOptional()
  @IsDateString()
  expectedAt?: string;

  @IsOptional()
  @IsString()
  remark?: string;
}

export class UpdatePurchaseDto {
  @IsOptional()
  @IsEnum(PurchaseStatus)
  status?: PurchaseStatus;

  @IsOptional()
  @IsString()
  remark?: string;
}

export class QueryPurchaseDto {
  @IsOptional()
  @IsNumber()
  supplierId?: number;

  @IsOptional()
  @IsEnum(PurchaseStatus)
  status?: PurchaseStatus;

  @IsOptional()
  @IsEnum(PaymentStatus)
  paymentStatus?: PaymentStatus;

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

export class ReceivePurchaseDto {
  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => ReceiveItemDto)
  items: ReceiveItemDto[];
}

export class ReceiveItemDto {
  @IsNumber()
  itemId: number;

  @IsNumber()
  @Min(0)
  receivedQuantity: number;

  @IsOptional()
  @IsNumber()
  receivedWeight?: number;
}

export class PayPurchaseDto {
  @IsNumber()
  @Min(0.01)
  amount: number;

  @IsOptional()
  @IsString()
  paymentMethod?: string;

  @IsOptional()
  @IsString()
  remark?: string;
}

