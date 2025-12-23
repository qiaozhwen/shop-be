import { Module } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { DashboardService } from './dashboard.service';
import { DashboardController } from './dashboard.controller';
import { Order } from '../order/order.entity';
import { Customer } from '../customer/customer.entity';
import { Inventory, InventoryAlert } from '../inventory/inventory.entity';
import { Product } from '../product/product.entity';

@Module({
  imports: [
    TypeOrmModule.forFeature([
      Order,
      Customer,
      Inventory,
      InventoryAlert,
      Product,
    ]),
  ],
  controllers: [DashboardController],
  providers: [DashboardService],
})
export class DashboardModule {}

