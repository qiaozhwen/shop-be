import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
} from '@nestjs/common';
import { InventoryService } from './inventory.service';
import { Inventory } from './inventory.entity';

@Controller('inventory')
export class InventoryController {
  constructor(private readonly inventoryService: InventoryService) {}

  @Get()
  findAll(): Promise<Inventory[]> {
    return this.inventoryService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string): Promise<Inventory> {
    return this.inventoryService.findOne(+id);
  }

  // @Post()
  // create(@Body() inventory: Partial<Inventory>): Promise<Inventory> {
  //   return this.inventoryService.create(inventory);
  // }

  // @Put(':id')
  // update(
  //   @Param('id') id: string,
  //   @Body() inventory: Partial<Inventory>,
  // ): Promise<Inventory> {
  //   return this.inventoryService.update(+id, inventory);
  // }

  // @Put(':id/quantity')
  // updateQuantity(
  //   @Param('id') id: string,
  //   @Body('quantity') quantity: number,
  // ): Promise<Inventory> {
  //   return this.inventoryService.updateQuantity(+id, quantity);
  // }

  // @Delete(':id')
  // remove(@Param('id') id: string): Promise<void> {
  //   return this.inventoryService.remove(+id);
  // }
}
