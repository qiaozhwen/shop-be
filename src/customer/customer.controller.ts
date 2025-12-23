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
  Request,
} from '@nestjs/common';
import { CustomerService } from './customer.service';
import {
  CreateCustomerDto,
  UpdateCustomerDto,
  QueryCustomerDto,
  CustomerRepayDto,
} from './dto/customer.dto';
import { AuthGuard } from '@nestjs/passport';

@Controller('customer')
@UseGuards(AuthGuard('jwt'))
export class CustomerController {
  constructor(private readonly customerService: CustomerService) {}

  @Get()
  findAll(@Query() query: QueryCustomerDto) {
    return this.customerService.findAll(query);
  }

  @Get('analysis')
  getAnalysis() {
    return this.customerService.getAnalysis();
  }

  @Get(':id')
  findOne(@Param('id', ParseIntPipe) id: number) {
    return this.customerService.findOne(id);
  }

  @Get(':id/credit-logs')
  getCreditLogs(@Param('id', ParseIntPipe) id: number) {
    return this.customerService.getCreditLogs(id);
  }

  @Post()
  create(@Body() createCustomerDto: CreateCustomerDto) {
    return this.customerService.create(createCustomerDto);
  }

  @Put(':id')
  update(
    @Param('id', ParseIntPipe) id: number,
    @Body() updateCustomerDto: UpdateCustomerDto,
  ) {
    return this.customerService.update(id, updateCustomerDto);
  }

  @Post(':id/repay')
  repay(
    @Param('id', ParseIntPipe) id: number,
    @Body() repayDto: CustomerRepayDto,
    @Request() req: any,
  ) {
    return this.customerService.repay(id, repayDto, req.user?.id || 1);
  }

  @Delete(':id')
  remove(@Param('id', ParseIntPipe) id: number) {
    return this.customerService.remove(id);
  }
}

