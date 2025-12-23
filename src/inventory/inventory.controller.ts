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
import { InventoryService } from './inventory.service';
import {
  CreateInboundDto,
  CreateOutboundDto,
  QueryInboundDto,
  QueryOutboundDto,
  UpdateInventoryDto,
} from './dto/inventory.dto';
import { AuthGuard } from '@nestjs/passport';

@Controller('inventory')
@UseGuards(AuthGuard('jwt'))
export class InventoryController {
  constructor(private readonly inventoryService: InventoryService) {}

  // ========== 库存 ==========

  @Get()
  findAll() {
    return this.inventoryService.findAll();
  }

  @Get('overview')
  getOverview() {
    return this.inventoryService.getOverview();
  }

  @Get('product/:productId')
  findByProduct(@Param('productId', ParseIntPipe) productId: number) {
    return this.inventoryService.findByProduct(productId);
  }

  @Put('product/:productId')
  updateInventory(
    @Param('productId', ParseIntPipe) productId: number,
    @Body() dto: UpdateInventoryDto,
  ) {
    return this.inventoryService.updateInventory(productId, dto);
  }

  // ========== 入库 ==========

  @Get('inbound')
  findInbounds(@Query() query: QueryInboundDto) {
    return this.inventoryService.findInbounds(query);
  }

  @Post('inbound')
  createInbound(@Body() dto: CreateInboundDto, @Request() req: any) {
    return this.inventoryService.createInbound(dto, req.user?.id || 1);
  }

  // ========== 出库 ==========

  @Get('outbound')
  findOutbounds(@Query() query: QueryOutboundDto) {
    return this.inventoryService.findOutbounds(query);
  }

  @Post('outbound')
  createOutbound(@Body() dto: CreateOutboundDto, @Request() req: any) {
    return this.inventoryService.createOutbound(dto, req.user?.id || 1);
  }

  // ========== 预警 ==========

  @Get('alert')
  findAlerts(@Query('handled') handled?: string) {
    const handledBool =
      handled === 'true' ? true : handled === 'false' ? false : undefined;
    return this.inventoryService.findAlerts(handledBool);
  }

  @Post('alert/:id/handle')
  handleAlert(@Param('id', ParseIntPipe) id: number, @Request() req: any) {
    return this.inventoryService.handleAlert(id, req.user?.id || 1);
  }
}
