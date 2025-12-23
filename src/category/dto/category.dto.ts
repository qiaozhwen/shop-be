import { IsString, IsOptional, IsNumber, IsBoolean } from 'class-validator';

export class CreateCategoryDto {
  @IsString()
  name: string;

  @IsOptional()
  @IsString()
  icon?: string;

  @IsOptional()
  @IsNumber()
  sort?: number;

  @IsOptional()
  @IsBoolean()
  status?: boolean;
}

export class UpdateCategoryDto {
  @IsOptional()
  @IsString()
  name?: string;

  @IsOptional()
  @IsString()
  icon?: string;

  @IsOptional()
  @IsNumber()
  sort?: number;

  @IsOptional()
  @IsBoolean()
  status?: boolean;
}

