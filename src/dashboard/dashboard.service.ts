import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, MoreThanOrEqual, Between } from 'typeorm';
import { Order, OrderStatus } from '../order/order.entity';
import { Customer } from '../customer/customer.entity';
import { Inventory, InventoryAlert } from '../inventory/inventory.entity';
import { Product } from '../product/product.entity';

@Injectable()
export class DashboardService {
  constructor(
    @InjectRepository(Order)
    private orderRepository: Repository<Order>,
    @InjectRepository(Customer)
    private customerRepository: Repository<Customer>,
    @InjectRepository(Inventory)
    private inventoryRepository: Repository<Inventory>,
    @InjectRepository(InventoryAlert)
    private alertRepository: Repository<InventoryAlert>,
    @InjectRepository(Product)
    private productRepository: Repository<Product>,
  ) {}

  async getOverview(): Promise<any> {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);

    // 今日销售额
    const todaySales = await this.orderRepository
      .createQueryBuilder('order')
      .select('SUM(order.actualAmount)', 'amount')
      .addSelect('COUNT(*)', 'orders')
      .where('order.orderAt >= :today', { today })
      .andWhere('order.status = :status', { status: OrderStatus.COMPLETED })
      .getRawOne();

    // 昨日销售额
    const yesterdaySales = await this.orderRepository
      .createQueryBuilder('order')
      .select('SUM(order.actualAmount)', 'amount')
      .addSelect('COUNT(*)', 'orders')
      .where('order.orderAt >= :yesterday', { yesterday })
      .andWhere('order.orderAt < :today', { today })
      .andWhere('order.status = :status', { status: OrderStatus.COMPLETED })
      .getRawOne();

    // 库存统计
    const inventoryStats = await this.inventoryRepository
      .createQueryBuilder('inv')
      .select('SUM(inv.quantity)', 'totalQuantity')
      .getRawOne();

    // 库存预警数
    const alertCount = await this.alertRepository.count({
      where: { handled: false },
    });

    // 活跃客户数
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    const activeCustomers = await this.customerRepository.count({
      where: {
        status: true,
        lastOrderAt: MoreThanOrEqual(thirtyDaysAgo),
      },
    });

    // 计算同比增长
    const todayAmount = Number(todaySales?.amount || 0);
    const yesterdayAmount = Number(yesterdaySales?.amount || 0);
    const salesGrowth =
      yesterdayAmount > 0
        ? ((todayAmount - yesterdayAmount) / yesterdayAmount) * 100
        : 0;

    return {
      todaySales: todayAmount,
      todayOrders: Number(todaySales?.orders || 0),
      salesGrowth: Math.round(salesGrowth * 10) / 10,
      totalInventory: Number(inventoryStats?.totalQuantity || 0),
      alertCount,
      activeCustomers,
    };
  }

  async getSalesTrend(days = 30): Promise<any[]> {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - days);
    startDate.setHours(0, 0, 0, 0);

    const result = await this.orderRepository
      .createQueryBuilder('order')
      .select("DATE_FORMAT(order.orderAt, '%Y-%m-%d')", 'date')
      .addSelect('SUM(order.actualAmount)', 'sales')
      .addSelect('COUNT(*)', 'orders')
      .where('order.orderAt >= :startDate', { startDate })
      .andWhere('order.status = :status', { status: OrderStatus.COMPLETED })
      .groupBy('date')
      .orderBy('date', 'ASC')
      .getRawMany();

    return result;
  }

  async getCategorySales(startDate?: string, endDate?: string): Promise<any[]> {
    const qb = this.orderRepository
      .createQueryBuilder('order')
      .leftJoin('order.items', 'item')
      .leftJoin('item.product', 'product')
      .leftJoin('product.category', 'category')
      .select('category.id', 'categoryId')
      .addSelect('category.name', 'categoryName')
      .addSelect('SUM(item.amount)', 'sales')
      .addSelect('SUM(item.quantity)', 'quantity')
      .where('order.status = :status', { status: OrderStatus.COMPLETED });

    if (startDate && endDate) {
      qb.andWhere('order.orderAt BETWEEN :startDate AND :endDate', {
        startDate: new Date(startDate),
        endDate: new Date(endDate + ' 23:59:59'),
      });
    }

    return qb
      .groupBy('category.id')
      .addGroupBy('category.name')
      .orderBy('sales', 'DESC')
      .getRawMany();
  }

  async getRecentOrders(limit = 10): Promise<Order[]> {
    return this.orderRepository.find({
      relations: ['customer'],
      order: { orderAt: 'DESC' },
      take: limit,
    });
  }

  async getInventoryAlerts(): Promise<InventoryAlert[]> {
    return this.alertRepository.find({
      where: { handled: false },
      relations: ['product'],
      order: { createdAt: 'DESC' },
      take: 10,
    });
  }

  async getTopProducts(limit = 10): Promise<any[]> {
    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);

    return this.orderRepository
      .createQueryBuilder('order')
      .leftJoin('order.items', 'item')
      .select('item.productId', 'productId')
      .addSelect('item.productName', 'productName')
      .addSelect('SUM(item.quantity)', 'quantity')
      .addSelect('SUM(item.amount)', 'amount')
      .where('order.orderAt >= :startDate', { startDate: thirtyDaysAgo })
      .andWhere('order.status = :status', { status: OrderStatus.COMPLETED })
      .groupBy('item.productId')
      .addGroupBy('item.productName')
      .orderBy('amount', 'DESC')
      .limit(limit)
      .getRawMany();
  }

  async getWeeklySales(): Promise<any[]> {
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - 7);
    startDate.setHours(0, 0, 0, 0);

    return this.orderRepository
      .createQueryBuilder('order')
      .select("DAYNAME(order.orderAt)", 'day')
      .addSelect('SUM(order.actualAmount)', 'sales')
      .addSelect('COUNT(*)', 'orders')
      .where('order.orderAt >= :startDate', { startDate })
      .andWhere('order.status = :status', { status: OrderStatus.COMPLETED })
      .groupBy('day')
      .orderBy("DAYOFWEEK(order.orderAt)", 'ASC')
      .getRawMany();
  }
}

