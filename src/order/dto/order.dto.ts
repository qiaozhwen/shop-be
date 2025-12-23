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
import { OrderStatus, PaymentMethod, PaymentStatus } from '../order.entity';

export class OrderItemDto {
  @IsNumber()
  productId: number;

  @IsString()
  productName: string;

  @IsOptional()
  @IsString()
  unit?: string;

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

export class CreateOrderDto {
  @IsOptional()
  @IsNumber()
  customerId?: number;

  @IsOptional()
  @IsString()
  customerName?: string;

  @IsArray()
  @ValidateNested({ each: true })
  @Type(() => OrderItemDto)
  items: OrderItemDto[];

  @IsOptional()
  @IsNumber()
  discountAmount?: number;

  @IsEnum(PaymentMethod)
  paymentMethod: PaymentMethod;

  @IsOptional()
  @IsNumber()
  paidAmount?: number;

  @IsOptional()
  @IsNumber()
  receivedAmount?: number;

  @IsOptional()
  @IsString()
  remark?: string;
}

export class QueryOrderDto {
  @IsOptional()
  @IsNumber()
  customerId?: number;

  @IsOptional()
  @IsEnum(OrderStatus)
  status?: OrderStatus;

  @IsOptional()
  @IsEnum(PaymentStatus)
  paymentStatus?: PaymentStatus;

  @IsOptional()
  @IsString()
  orderNo?: string;

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

export class PayOrderDto {
  @IsEnum(PaymentMethod)
  paymentMethod: PaymentMethod;

  @IsNumber()
  @Min(0.01)
  amount: number;

  @IsOptional()
  @IsNumber()
  receivedAmount?: number;

  @IsOptional()
  @IsString()
  transactionNo?: string;
}

export class UpdateOrderDto {
  @IsOptional()
  @IsEnum(OrderStatus)
  status?: OrderStatus;

  @IsOptional()
  @IsString()
  remark?: string;
}

