import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { TypeOrmModule } from '@nestjs/typeorm';
import { StoreModule } from './store/store.module';
import { EmployeeModule } from './employee/employee.module';
import { RegionModule } from './region/region.module';
import { ProductModule } from './product/product.module';
import { OrderModule } from './order/order.module';
import { InventoryModule } from './inventory/inventory.module';

@Module({
  imports: [
    TypeOrmModule.forRoot({
      type: 'mysql',
      host: '106.14.227.122',
      port: 3306,
      username: 'root',
      password: '13524155957Qz@1',
      database: 'shop',
      autoLoadEntities: true,
      synchronize: true,
    }),
    StoreModule,
    EmployeeModule,
    RegionModule,
    ProductModule,
    OrderModule,
    InventoryModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
