import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Order, OrderItem } from './order.entity';

@Injectable()
export class OrderService {
  constructor(
    @InjectRepository(Order)
    private orderRepository: Repository<Order>,
    @InjectRepository(OrderItem)
    private orderItemRepository: Repository<OrderItem>,
  ) {}

  async findAll(): Promise<Order[]> {
    return this.orderRepository.find();
  }

  async findOne(id: number): Promise<Order> {
    const order = await this.orderRepository.findOne({ where: { id } });
    if (!order) {
      throw new Error('订单不存在');
    }
    return order;
  }

  async create(order: Partial<Order>): Promise<Order> {
    const newOrder = this.orderRepository.create(order);
    return this.orderRepository.save(newOrder);
  }

  async update(id: number, order: Partial<Order>): Promise<Order> {
    await this.orderRepository.update(id, order);
    const updatedOrder = await this.orderRepository.findOne({ where: { id } });
    if (!updatedOrder) {
      throw new Error('订单不存在');
    }
    return updatedOrder;
  }

  async updateStatus(id: number, status: string): Promise<Order> {
    await this.orderRepository.update(id, { status });
    const updatedOrder = await this.orderRepository.findOne({ where: { id } });
    if (!updatedOrder) {
      throw new Error('订单不存在');
    }
    return updatedOrder;
  }

  async remove(id: number): Promise<void> {
    await this.orderRepository.delete(id);
  }
}
