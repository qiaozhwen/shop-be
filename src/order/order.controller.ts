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
import { OrderService } from './order.service';
import {
  CreateOrderDto,
  QueryOrderDto,
  PayOrderDto,
  UpdateOrderDto,
} from './dto/order.dto';
import { AuthGuard } from '@nestjs/passport';

@Controller('order')
@UseGuards(AuthGuard('jwt'))
export class OrderController {
  constructor(private readonly orderService: OrderService) {}

  @Get()
  findAll(@Query() query: QueryOrderDto) {
    return this.orderService.findAll(query);
  }

  @Get('statistics')
  getStatistics(
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
  ) {
    return this.orderService.getStatistics(startDate, endDate);
  }

  @Get('top-products')
  getTopProducts(
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
    @Query('limit') limit?: string,
  ) {
    return this.orderService.getTopProducts(
      startDate,
      endDate,
      limit ? parseInt(limit) : 10,
    );
  }

  @Get(':id')
  findOne(@Param('id', ParseIntPipe) id: number) {
    return this.orderService.findOne(id);
  }

  @Get('no/:orderNo')
  findByOrderNo(@Param('orderNo') orderNo: string) {
    return this.orderService.findByOrderNo(orderNo);
  }

  @Post()
  create(@Body() createOrderDto: CreateOrderDto, @Request() req: any) {
    return this.orderService.create(createOrderDto, req.user?.id || 1);
  }

  @Put(':id')
  update(
    @Param('id', ParseIntPipe) id: number,
    @Body() updateOrderDto: UpdateOrderDto,
  ) {
    return this.orderService.update(id, updateOrderDto);
  }

  @Post(':id/pay')
  pay(
    @Param('id', ParseIntPipe) id: number,
    @Body() payDto: PayOrderDto,
    @Request() req: any,
  ) {
    return this.orderService.pay(id, payDto, req.user?.id || 1);
  }

  @Post(':id/cancel')
  cancel(@Param('id', ParseIntPipe) id: number) {
    return this.orderService.cancel(id);
  }
}
