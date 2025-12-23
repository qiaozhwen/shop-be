import {
  IsString,
  IsOptional,
  IsNumber,
  IsEnum,
  IsDateString,
} from 'class-validator';
import { LogModule, LogAction } from '../system-log.entity';

export class CreateLogDto {
  @IsOptional()
  @IsNumber()
  staffId?: number;

  @IsOptional()
  @IsString()
  staffName?: string;

  @IsEnum(LogModule)
  module: LogModule;

  @IsEnum(LogAction)
  action: LogAction;

  @IsOptional()
  @IsString()
  content?: string;

  @IsOptional()
  @IsString()
  ip?: string;

  @IsOptional()
  @IsString()
  userAgent?: string;
}

export class QueryLogDto {
  @IsOptional()
  @IsNumber()
  staffId?: number;

  @IsOptional()
  @IsEnum(LogModule)
  module?: LogModule;

  @IsOptional()
  @IsEnum(LogAction)
  action?: LogAction;

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

