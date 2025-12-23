import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between } from 'typeorm';
import {
  FinanceRecord,
  DailySettlement,
  FinanceType,
  FinanceCategory,
} from './finance.entity';
import { CreateFinanceDto, QueryFinanceDto } from './dto/finance.dto';

@Injectable()
export class FinanceService {
  constructor(
    @InjectRepository(FinanceRecord)
    private financeRepository: Repository<FinanceRecord>,
    @InjectRepository(DailySettlement)
    private settlementRepository: Repository<DailySettlement>,
  ) {}

  private generateNo(): string {
    const now = new Date();
    const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
    const random = Math.floor(Math.random() * 10000)
      .toString()
      .padStart(4, '0');
    return `FIN${dateStr}${random}`;
  }

  async findAll(query?: QueryFinanceDto): Promise<{
    list: FinanceRecord[];
    total: number;
    page: number;
    pageSize: number;
  }> {
    const page = query?.page || 1;
    const pageSize = query?.pageSize || 20;
    const skip = (page - 1) * pageSize;

    const where: any = {};
    if (query?.type) {
      where.type = query.type;
    }
    if (query?.category) {
      where.category = query.category;
    }
    if (query?.startDate && query?.endDate) {
      where.recordAt = Between(
        new Date(query.startDate),
        new Date(query.endDate),
      );
    }

    const [list, total] = await this.financeRepository.findAndCount({
      where,
      order: { recordAt: 'DESC', createdAt: 'DESC' },
      skip,
      take: pageSize,
    });

    return { list, total, page, pageSize };
  }

  async findOne(id: number): Promise<FinanceRecord> {
    const record = await this.financeRepository.findOne({
      where: { id },
    });
    if (!record) {
      throw new NotFoundException(`财务记录 #${id} 不存在`);
    }
    return record;
  }

  async create(
    dto: CreateFinanceDto,
    operatorId: number,
  ): Promise<FinanceRecord> {
    const record = this.financeRepository.create({
      recordNo: this.generateNo(),
      type: dto.type,
      category: dto.category,
      amount: dto.amount,
      paymentMethod: dto.paymentMethod,
      relatedType: dto.relatedType,
      relatedId: dto.relatedId,
      description: dto.description,
      remark: dto.remark,
      operatorId,
      recordAt: dto.recordAt ? new Date(dto.recordAt) : new Date(),
    });
    return this.financeRepository.save(record);
  }

  // 自动创建销售收入记录
  async createSaleIncome(
    orderId: number,
    amount: number,
    paymentMethod: string,
    operatorId: number,
  ): Promise<void> {
    await this.create(
      {
        type: FinanceType.INCOME,
        category: FinanceCategory.SALE,
        amount,
        paymentMethod,
        relatedType: 'order',
        relatedId: orderId,
        description: `订单收入`,
      },
      operatorId,
    );
  }

  // 自动创建采购支出记录
  async createPurchaseExpense(
    purchaseId: number,
    amount: number,
    operatorId: number,
  ): Promise<void> {
    await this.create(
      {
        type: FinanceType.EXPENSE,
        category: FinanceCategory.PURCHASE,
        amount,
        relatedType: 'purchase',
        relatedId: purchaseId,
        description: `采购支出`,
      },
      operatorId,
    );
  }

  // 获取收支统计
  async getSummary(startDate?: string, endDate?: string): Promise<any> {
    const where: any = {};
    if (startDate && endDate) {
      where.recordAt = Between(new Date(startDate), new Date(endDate));
    }

    // 收入统计
    const incomeResult = await this.financeRepository
      .createQueryBuilder('record')
      .select('SUM(record.amount)', 'total')
      .where({ ...where, type: FinanceType.INCOME })
      .getRawOne();

    // 支出统计
    const expenseResult = await this.financeRepository
      .createQueryBuilder('record')
      .select('SUM(record.amount)', 'total')
      .where({ ...where, type: FinanceType.EXPENSE })
      .getRawOne();

    // 按分类统计
    const categoryStats = await this.financeRepository
      .createQueryBuilder('record')
      .select('record.type', 'type')
      .addSelect('record.category', 'category')
      .addSelect('SUM(record.amount)', 'amount')
      .where(where)
      .groupBy('record.type')
      .addGroupBy('record.category')
      .getRawMany();

    const totalIncome = Number(incomeResult?.total || 0);
    const totalExpense = Number(expenseResult?.total || 0);

    return {
      totalIncome,
      totalExpense,
      profit: totalIncome - totalExpense,
      categoryStats,
    };
  }

  // 日结
  async createDailySettlement(
    date: Date,
    operatorId: number,
  ): Promise<DailySettlement> {
    const dateStr = date.toISOString().slice(0, 10);
    const nextDate = new Date(date);
    nextDate.setDate(nextDate.getDate() + 1);

    // 检查是否已结算
    const existing = await this.settlementRepository.findOne({
      where: { settleDate: date },
    });
    if (existing) {
      return existing;
    }

    // 获取当日财务数据
    const summary = await this.getSummary(dateStr, dateStr);

    // TODO: 从订单获取更详细的支付方式统计

    const settlement = this.settlementRepository.create({
      settleDate: date,
      totalSales: summary.totalIncome,
      totalExpense: summary.totalExpense,
      profit: summary.profit,
      operatorId,
      settledAt: new Date(),
    });

    return this.settlementRepository.save(settlement);
  }

  async getSettlements(
    startDate?: string,
    endDate?: string,
  ): Promise<DailySettlement[]> {
    const where: any = {};
    if (startDate && endDate) {
      where.settleDate = Between(new Date(startDate), new Date(endDate));
    }
    return this.settlementRepository.find({
      where,
      order: { settleDate: 'DESC' },
    });
  }
}

