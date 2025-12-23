import { Module } from '@nestjs/common';
import { AppController } from './app.controller';
import { AppService } from './app.service';
import { TypeOrmModule } from '@nestjs/typeorm';
import { ConfigModule } from '@nestjs/config';

// 已有模块
import { StoreModule } from './store/store.module';
import { EmployeeModule } from './employee/employee.module';
import { RegionModule } from './region/region.module';
import { UserModule } from './user/user.module';
import { RoleModule } from './role/role.module';
import { AuthModule } from './auth/auth.module';
import { HistoricalAmountModule } from './historical-amount/historical-amount.module';

// 新模块
import { CategoryModule } from './category/category.module';
import { ProductModule } from './product/product.module';
import { CustomerModule } from './customer/customer.module';
import { SupplierModule } from './supplier/supplier.module';
import { PurchaseModule } from './purchase/purchase.module';
import { InventoryModule } from './inventory/inventory.module';
import { OrderModule } from './order/order.module';
import { FinanceModule } from './finance/finance.module';
import { DashboardModule } from './dashboard/dashboard.module';
import { SystemLogModule } from './system-log/system-log.module';

@Module({
  imports: [
    ConfigModule.forRoot({
      isGlobal: true,
    }),
    TypeOrmModule.forRoot({
      type: 'mysql',
      host: process.env.DB_HOST || '106.14.227.122',
      port: parseInt(process.env.DB_PORT || '3306'),
      username: process.env.DB_USERNAME || 'root',
      password: process.env.DB_PASSWORD || '13524155957Qz@1',
      database: process.env.DB_DATABASE || 'freshbird',
      autoLoadEntities: true,
      synchronize: false, // 关闭自动同步，使用 SQL 脚本管理表结构
    }),
    // 系统基础模块
    AuthModule,
    UserModule,
    RoleModule,
    SystemLogModule,

    // 业务模块
    CategoryModule,
    ProductModule,
    InventoryModule,
    CustomerModule,
    SupplierModule,
    PurchaseModule,
    OrderModule,
    FinanceModule,
    DashboardModule,

    // 其他模块
    StoreModule,
    EmployeeModule,
    RegionModule,
    HistoricalAmountModule,
  ],
  controllers: [AppController],
  providers: [AppService],
})
export class AppModule {}
