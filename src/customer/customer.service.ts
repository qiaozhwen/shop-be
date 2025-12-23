import {
  Injectable,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like, MoreThan } from 'typeorm';
import { Customer, CustomerCreditLog } from './customer.entity';
import {
  CreateCustomerDto,
  UpdateCustomerDto,
  QueryCustomerDto,
  CustomerRepayDto,
} from './dto/customer.dto';

@Injectable()
export class CustomerService {
  constructor(
    @InjectRepository(Customer)
    private customerRepository: Repository<Customer>,
    @InjectRepository(CustomerCreditLog)
    private creditLogRepository: Repository<CustomerCreditLog>,
  ) {}

  async findAll(query?: QueryCustomerDto): Promise<{
    list: Customer[];
    total: number;
    page: number;
    pageSize: number;
  }> {
    const page = query?.page || 1;
    const pageSize = query?.pageSize || 20;
    const skip = (page - 1) * pageSize;

    const where: any = { status: true };
    if (query?.keyword) {
      where.name = Like(`%${query.keyword}%`);
    }
    if (query?.type) {
      where.type = query.type;
    }
    if (query?.level) {
      where.level = query.level;
    }
    if (query?.hasCredit) {
      where.creditBalance = MoreThan(0);
    }

    const [list, total] = await this.customerRepository.findAndCount({
      where,
      order: { lastOrderAt: 'DESC', id: 'DESC' },
      skip,
      take: pageSize,
    });

    return { list, total, page, pageSize };
  }

  async findOne(id: number): Promise<Customer> {
    const customer = await this.customerRepository.findOne({
      where: { id },
    });
    if (!customer) {
      throw new NotFoundException(`客户 #${id} 不存在`);
    }
    return customer;
  }

  async findByPhone(phone: string): Promise<Customer | null> {
    return this.customerRepository.findOne({ where: { phone } });
  }

  async create(createCustomerDto: CreateCustomerDto): Promise<Customer> {
    const existing = await this.findByPhone(createCustomerDto.phone);
    if (existing) {
      throw new BadRequestException('该手机号已被注册');
    }
    const customer = this.customerRepository.create(createCustomerDto);
    return this.customerRepository.save(customer);
  }

  async update(id: number, updateCustomerDto: UpdateCustomerDto): Promise<Customer> {
    await this.customerRepository.update(id, updateCustomerDto);
    return this.findOne(id);
  }

  async remove(id: number): Promise<void> {
    await this.customerRepository.update(id, { status: false });
  }

  // 客户还款
  async repay(
    id: number,
    repayDto: CustomerRepayDto,
    operatorId: number,
  ): Promise<Customer> {
    const customer = await this.findOne(id);

    if (repayDto.amount > Number(customer.creditBalance)) {
      throw new BadRequestException('还款金额不能大于欠款金额');
    }

    const balanceBefore = Number(customer.creditBalance);
    const balanceAfter = balanceBefore - repayDto.amount;

    // 创建还款记录
    const creditLog = this.creditLogRepository.create({
      customerId: id,
      type: 'repay',
      amount: repayDto.amount,
      balanceBefore,
      balanceAfter,
      remark: repayDto.remark,
      operatorId,
    });
    await this.creditLogRepository.save(creditLog);

    // 更新客户欠款
    await this.customerRepository.update(id, {
      creditBalance: balanceAfter,
    });

    return this.findOne(id);
  }

  // 增加欠款(赊账)
  async addCredit(
    id: number,
    amount: number,
    orderId: number,
    operatorId: number,
  ): Promise<void> {
    const customer = await this.findOne(id);

    const balanceBefore = Number(customer.creditBalance);
    const balanceAfter = balanceBefore + amount;

    if (balanceAfter > Number(customer.creditLimit)) {
      throw new BadRequestException('超过客户赊账额度');
    }

    // 创建赊账记录
    const creditLog = this.creditLogRepository.create({
      customerId: id,
      type: 'credit',
      amount,
      orderId,
      balanceBefore,
      balanceAfter,
      operatorId,
    });
    await this.creditLogRepository.save(creditLog);

    // 更新客户欠款
    await this.customerRepository.update(id, {
      creditBalance: balanceAfter,
    });
  }

  // 获取客户欠款记录
  async getCreditLogs(customerId: number): Promise<CustomerCreditLog[]> {
    return this.creditLogRepository.find({
      where: { customerId },
      order: { createdAt: 'DESC' },
    });
  }

  // 更新客户订单统计
  async updateOrderStats(
    id: number,
    orderAmount: number,
  ): Promise<void> {
    const customer = await this.findOne(id);
    await this.customerRepository.update(id, {
      totalOrders: customer.totalOrders + 1,
      totalAmount: Number(customer.totalAmount) + orderAmount,
      lastOrderAt: new Date(),
    });
  }

  // 获取客户分析数据
  async getAnalysis(): Promise<any> {
    const totalCustomers = await this.customerRepository.count({
      where: { status: true },
    });

    const vipCustomers = await this.customerRepository.count({
      where: { status: true, level: 'vip' as any },
    });

    const svipCustomers = await this.customerRepository.count({
      where: { status: true, level: 'svip' as any },
    });

    const totalCredit = await this.customerRepository
      .createQueryBuilder('customer')
      .select('SUM(customer.creditBalance)', 'total')
      .getRawOne();

    const topCustomers = await this.customerRepository.find({
      where: { status: true },
      order: { totalAmount: 'DESC' },
      take: 10,
    });

    return {
      totalCustomers,
      vipCustomers,
      svipCustomers,
      totalCredit: totalCredit?.total || 0,
      topCustomers,
    };
  }
}

