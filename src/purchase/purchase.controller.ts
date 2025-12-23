import {
  Controller,
  Get,
  Post,
  Put,
  Body,
  Param,
  Query,
  ParseIntPipe,
  UseGuards,
  Request,
} from '@nestjs/common';
import { PurchaseService } from './purchase.service';
import {
  CreatePurchaseDto,
  UpdatePurchaseDto,
  QueryPurchaseDto,
  ReceivePurchaseDto,
  PayPurchaseDto,
} from './dto/purchase.dto';
import { AuthGuard } from '@nestjs/passport';

@Controller('purchase')
@UseGuards(AuthGuard('jwt'))
export class PurchaseController {
  constructor(private readonly purchaseService: PurchaseService) {}

  @Get()
  findAll(@Query() query: QueryPurchaseDto) {
    return this.purchaseService.findAll(query);
  }

  @Get('statistics')
  getStatistics(
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
  ) {
    return this.purchaseService.getStatistics(startDate, endDate);
  }

  @Get(':id')
  findOne(@Param('id', ParseIntPipe) id: number) {
    return this.purchaseService.findOne(id);
  }

  @Post()
  create(@Body() createPurchaseDto: CreatePurchaseDto, @Request() req: any) {
    return this.purchaseService.create(
      createPurchaseDto,
      req.user?.id || 1,
    );
  }

  @Put(':id')
  update(
    @Param('id', ParseIntPipe) id: number,
    @Body() updatePurchaseDto: UpdatePurchaseDto,
  ) {
    return this.purchaseService.update(id, updatePurchaseDto);
  }

  @Post(':id/confirm')
  confirm(@Param('id', ParseIntPipe) id: number) {
    return this.purchaseService.confirm(id);
  }

  @Post(':id/receive')
  receive(
    @Param('id', ParseIntPipe) id: number,
    @Body() receiveDto: ReceivePurchaseDto,
  ) {
    return this.purchaseService.receive(id, receiveDto);
  }

  @Post(':id/cancel')
  cancel(@Param('id', ParseIntPipe) id: number) {
    return this.purchaseService.cancel(id);
  }

  @Post(':id/pay')
  pay(
    @Param('id', ParseIntPipe) id: number,
    @Body() payDto: PayPurchaseDto,
  ) {
    return this.purchaseService.pay(id, payDto);
  }
}

