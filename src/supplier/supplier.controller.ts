import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
  Query,
  ParseIntPipe,
  UseGuards,
} from '@nestjs/common';
import { SupplierService } from './supplier.service';
import {
  CreateSupplierDto,
  UpdateSupplierDto,
  QuerySupplierDto,
} from './dto/supplier.dto';
import { AuthGuard } from '@nestjs/passport';

@Controller('supplier')
@UseGuards(AuthGuard('jwt'))
export class SupplierController {
  constructor(private readonly supplierService: SupplierService) {}

  @Get()
  findAll(@Query() query: QuerySupplierDto) {
    return this.supplierService.findAll(query);
  }

  @Get('active')
  findActive() {
    return this.supplierService.findActive();
  }

  @Get(':id')
  findOne(@Param('id', ParseIntPipe) id: number) {
    return this.supplierService.findOne(id);
  }

  @Post()
  create(@Body() createSupplierDto: CreateSupplierDto) {
    return this.supplierService.create(createSupplierDto);
  }

  @Put(':id')
  update(
    @Param('id', ParseIntPipe) id: number,
    @Body() updateSupplierDto: UpdateSupplierDto,
  ) {
    return this.supplierService.update(id, updateSupplierDto);
  }

  @Post(':id/pay')
  pay(
    @Param('id', ParseIntPipe) id: number,
    @Body('amount') amount: number,
  ) {
    return this.supplierService.pay(id, amount);
  }

  @Delete(':id')
  remove(@Param('id', ParseIntPipe) id: number) {
    return this.supplierService.remove(id);
  }
}

