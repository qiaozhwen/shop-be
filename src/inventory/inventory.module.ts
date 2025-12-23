import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import {
  Inventory,
  InventoryInbound,
  InventoryOutbound,
  InventoryAlert,
} from './inventory.entity';
import { InventoryService } from './inventory.service';
import { InventoryController } from './inventory.controller';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      Inventory,
      InventoryInbound,
      InventoryOutbound,
      InventoryAlert,
    ]),
  ],
  controllers: [InventoryController],
  providers: [InventoryService],
  exports: [InventoryService],
})
export class InventoryModule {}
