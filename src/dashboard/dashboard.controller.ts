import {
  Controller,
  Get,
  Query,
  UseGuards,
} from '@nestjs/common';
import { DashboardService } from './dashboard.service';
import { AuthGuard } from '@nestjs/passport';

@Controller('dashboard')
@UseGuards(AuthGuard('jwt'))
export class DashboardController {
  constructor(private readonly dashboardService: DashboardService) {}

  @Get('overview')
  getOverview() {
    return this.dashboardService.getOverview();
  }

  @Get('sales-trend')
  getSalesTrend(@Query('days') days?: string) {
    return this.dashboardService.getSalesTrend(days ? parseInt(days) : 30);
  }

  @Get('category-sales')
  getCategorySales(
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
  ) {
    return this.dashboardService.getCategorySales(startDate, endDate);
  }

  @Get('recent-orders')
  getRecentOrders(@Query('limit') limit?: string) {
    return this.dashboardService.getRecentOrders(limit ? parseInt(limit) : 10);
  }

  @Get('inventory-alerts')
  getInventoryAlerts() {
    return this.dashboardService.getInventoryAlerts();
  }

  @Get('top-products')
  getTopProducts(@Query('limit') limit?: string) {
    return this.dashboardService.getTopProducts(limit ? parseInt(limit) : 10);
  }

  @Get('weekly-sales')
  getWeeklySales() {
    return this.dashboardService.getWeeklySales();
  }
}

