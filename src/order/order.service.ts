import {
  Injectable,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between } from 'typeorm';
import {
  Order,
  OrderItem,
  OrderPayment,
  OrderStatus,
  PaymentMethod,
  PaymentStatus,
} from './order.entity';
import {
  CreateOrderDto,
  QueryOrderDto,
  PayOrderDto,
  UpdateOrderDto,
} from './dto/order.dto';

@Injectable()
export class OrderService {
  constructor(
    @InjectRepository(Order)
    private orderRepository: Repository<Order>,
    @InjectRepository(OrderItem)
    private orderItemRepository: Repository<OrderItem>,
    @InjectRepository(OrderPayment)
    private paymentRepository: Repository<OrderPayment>,
  ) {}

  private generateOrderNo(): string {
    const now = new Date();
    const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
    const random = Math.floor(Math.random() * 10000)
      .toString()
      .padStart(4, '0');
    return `ORD${dateStr}${random}`;
  }

  async findAll(query?: QueryOrderDto): Promise<{
    list: Order[];
    total: number;
    page: number;
    pageSize: number;
  }> {
    const page = query?.page || 1;
    const pageSize = query?.pageSize || 20;
    const skip = (page - 1) * pageSize;

    const where: any = {};
    if (query?.customerId) {
      where.customerId = query.customerId;
    }
    if (query?.status) {
      where.status = query.status;
    }
    if (query?.paymentStatus) {
      where.paymentStatus = query.paymentStatus;
    }
    if (query?.startDate && query?.endDate) {
      where.orderAt = Between(
        new Date(query.startDate),
        new Date(query.endDate + ' 23:59:59'),
      );
    }

    const [list, total] = await this.orderRepository.findAndCount({
      where,
      relations: ['customer', 'items', 'items.product'],
      order: { orderAt: 'DESC' },
      skip,
      take: pageSize,
    });

    return { list, total, page, pageSize };
  }

  async findOne(id: number): Promise<Order> {
    const order = await this.orderRepository.findOne({
      where: { id },
      relations: ['customer', 'items', 'items.product', 'payments'],
    });
    if (!order) {
      throw new NotFoundException(`订单 #${id} 不存在`);
    }
    return order;
  }

  async findByOrderNo(orderNo: string): Promise<Order> {
    const order = await this.orderRepository.findOne({
      where: { orderNo },
      relations: ['customer', 'items', 'items.product', 'payments'],
    });
    if (!order) {
      throw new NotFoundException(`订单 ${orderNo} 不存在`);
    }
    return order;
  }

  async create(dto: CreateOrderDto, operatorId: number): Promise<Order> {
    // 计算金额
    let totalQuantity = 0;
    let totalWeight = 0;
    let totalAmount = 0;

    const items = dto.items.map((item) => {
      const amount = item.quantity * item.unitPrice;
      totalQuantity += item.quantity;
      totalWeight += item.weight || 0;
      totalAmount += amount;

      return {
        ...item,
        unit: item.unit || '只',
        amount,
      };
    });

    const discountAmount = dto.discountAmount || 0;
    const actualAmount = totalAmount - discountAmount;
    const paidAmount = dto.paidAmount || 0;

    // 确定支付状态
    let paymentStatus = PaymentStatus.UNPAID;
    if (paidAmount >= actualAmount) {
      paymentStatus = PaymentStatus.PAID;
    } else if (paidAmount > 0) {
      paymentStatus = PaymentStatus.PARTIAL;
    }

    // 确定订单状态
    const status =
      paymentStatus === PaymentStatus.PAID
        ? OrderStatus.COMPLETED
        : OrderStatus.PENDING;

    const order = this.orderRepository.create({
      orderNo: this.generateOrderNo(),
      customerId: dto.customerId,
      customerName: dto.customerName,
      totalQuantity,
      totalWeight,
      totalAmount,
      discountAmount,
      actualAmount,
      paymentMethod: dto.paymentMethod,
      paymentStatus,
      paidAmount,
      status,
      remark: dto.remark,
      operatorId,
      orderAt: new Date(),
      completedAt: status === OrderStatus.COMPLETED ? new Date() : undefined,
      items: items as OrderItem[],
    });

    const savedOrder = await this.orderRepository.save(order);

    // 创建支付记录
    if (paidAmount > 0) {
      const payment = this.paymentRepository.create({
        orderId: savedOrder.id,
        paymentMethod: dto.paymentMethod,
        amount: paidAmount,
        receivedAmount: dto.receivedAmount,
        changeAmount:
          dto.receivedAmount && dto.paymentMethod === PaymentMethod.CASH
            ? dto.receivedAmount - paidAmount
            : undefined,
        operatorId,
        paidAt: new Date(),
      });
      await this.paymentRepository.save(payment);
    }

    return this.findOne(savedOrder.id);
  }

  async update(id: number, dto: UpdateOrderDto): Promise<Order> {
    const order = await this.findOne(id);
    if (order.status === OrderStatus.CANCELLED) {
      throw new BadRequestException('已取消的订单不能修改');
    }
    await this.orderRepository.update(id, dto);
    return this.findOne(id);
  }

  async cancel(id: number): Promise<Order> {
    const order = await this.findOne(id);
    if (order.status === OrderStatus.COMPLETED) {
      throw new BadRequestException('已完成的订单不能取消');
    }
    await this.orderRepository.update(id, {
      status: OrderStatus.CANCELLED,
    });
    return this.findOne(id);
  }

  async pay(id: number, dto: PayOrderDto, operatorId: number): Promise<Order> {
    const order = await this.findOne(id);

    if (order.paymentStatus === PaymentStatus.PAID) {
      throw new BadRequestException('订单已付清');
    }

    const newPaidAmount = Number(order.paidAmount) + dto.amount;
    const actualAmount = Number(order.actualAmount);

    let paymentStatus = PaymentStatus.PARTIAL;
    let status = order.status;

    if (newPaidAmount >= actualAmount) {
      paymentStatus = PaymentStatus.PAID;
      status = OrderStatus.COMPLETED;
    }

    // 创建支付记录
    const payment = this.paymentRepository.create({
      orderId: id,
      paymentMethod: dto.paymentMethod,
      amount: dto.amount,
      receivedAmount: dto.receivedAmount,
      changeAmount:
        dto.receivedAmount && dto.paymentMethod === PaymentMethod.CASH
          ? dto.receivedAmount - dto.amount
          : undefined,
      transactionNo: dto.transactionNo,
      operatorId,
      paidAt: new Date(),
    });
    await this.paymentRepository.save(payment);

    // 更新订单
    await this.orderRepository.update(id, {
      paidAmount: newPaidAmount,
      paymentStatus,
      status,
      completedAt:
        status === OrderStatus.COMPLETED ? new Date() : undefined,
    });

    return this.findOne(id);
  }

  async getStatistics(startDate?: string, endDate?: string): Promise<any> {
    const where: any = { status: OrderStatus.COMPLETED };
    if (startDate && endDate) {
      where.orderAt = Between(
        new Date(startDate),
        new Date(endDate + ' 23:59:59'),
      );
    }

    const result = await this.orderRepository
      .createQueryBuilder('order')
      .select('COUNT(*)', 'totalOrders')
      .addSelect('SUM(order.actualAmount)', 'totalSales')
      .addSelect('SUM(order.totalQuantity)', 'totalQuantity')
      .addSelect('AVG(order.actualAmount)', 'avgOrderAmount')
      .where(where)
      .getRawOne();

    // 今日统计
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayStats = await this.orderRepository
      .createQueryBuilder('order')
      .select('COUNT(*)', 'orders')
      .addSelect('SUM(order.actualAmount)', 'sales')
      .where('order.orderAt >= :today', { today })
      .andWhere('order.status = :status', { status: OrderStatus.COMPLETED })
      .getRawOne();

    return {
      ...result,
      todayOrders: todayStats?.orders || 0,
      todaySales: todayStats?.sales || 0,
    };
  }

  async getTopProducts(
    startDate?: string,
    endDate?: string,
    limit = 10,
  ): Promise<any[]> {
    const qb = this.orderItemRepository
      .createQueryBuilder('item')
      .leftJoin('item.order', 'order')
      .select('item.productId', 'productId')
      .addSelect('item.productName', 'productName')
      .addSelect('SUM(item.quantity)', 'totalQuantity')
      .addSelect('SUM(item.amount)', 'totalAmount')
      .where('order.status = :status', { status: OrderStatus.COMPLETED });

    if (startDate && endDate) {
      qb.andWhere('order.orderAt BETWEEN :startDate AND :endDate', {
        startDate: new Date(startDate),
        endDate: new Date(endDate + ' 23:59:59'),
      });
    }

    return qb
      .groupBy('item.productId')
      .addGroupBy('item.productName')
      .orderBy('totalAmount', 'DESC')
      .limit(limit)
      .getRawMany();
  }
}
