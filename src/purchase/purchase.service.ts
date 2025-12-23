import {
  Injectable,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between } from 'typeorm';
import {
  PurchaseOrder,
  PurchaseOrderItem,
  PurchaseStatus,
  PaymentStatus,
} from './purchase.entity';
import {
  CreatePurchaseDto,
  UpdatePurchaseDto,
  QueryPurchaseDto,
  ReceivePurchaseDto,
  PayPurchaseDto,
} from './dto/purchase.dto';

@Injectable()
export class PurchaseService {
  constructor(
    @InjectRepository(PurchaseOrder)
    private purchaseRepository: Repository<PurchaseOrder>,
    @InjectRepository(PurchaseOrderItem)
    private purchaseItemRepository: Repository<PurchaseOrderItem>,
  ) {}

  private generatePurchaseNo(): string {
    const now = new Date();
    const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
    const random = Math.floor(Math.random() * 10000)
      .toString()
      .padStart(4, '0');
    return `PO${dateStr}${random}`;
  }

  async findAll(query?: QueryPurchaseDto): Promise<{
    list: PurchaseOrder[];
    total: number;
    page: number;
    pageSize: number;
  }> {
    const page = query?.page || 1;
    const pageSize = query?.pageSize || 20;
    const skip = (page - 1) * pageSize;

    const where: any = {};
    if (query?.supplierId) {
      where.supplierId = query.supplierId;
    }
    if (query?.status) {
      where.status = query.status;
    }
    if (query?.paymentStatus) {
      where.paymentStatus = query.paymentStatus;
    }
    if (query?.startDate && query?.endDate) {
      where.createdAt = Between(
        new Date(query.startDate),
        new Date(query.endDate + ' 23:59:59'),
      );
    }

    const [list, total] = await this.purchaseRepository.findAndCount({
      where,
      relations: ['supplier', 'items', 'items.product'],
      order: { createdAt: 'DESC' },
      skip,
      take: pageSize,
    });

    return { list, total, page, pageSize };
  }

  async findOne(id: number): Promise<PurchaseOrder> {
    const purchase = await this.purchaseRepository.findOne({
      where: { id },
      relations: ['supplier', 'items', 'items.product'],
    });
    if (!purchase) {
      throw new NotFoundException(`采购单 #${id} 不存在`);
    }
    return purchase;
  }

  async create(
    createPurchaseDto: CreatePurchaseDto,
    operatorId: number,
  ): Promise<PurchaseOrder> {
    // 计算总金额
    let totalQuantity = 0;
    let totalWeight = 0;
    let totalAmount = 0;

    const items = createPurchaseDto.items.map((item) => {
      const amount = item.quantity * item.unitPrice;
      totalQuantity += item.quantity;
      totalWeight += item.weight || 0;
      totalAmount += amount;

      return {
        ...item,
        amount,
      };
    });

    const purchase = this.purchaseRepository.create({
      purchaseNo: this.generatePurchaseNo(),
      supplierId: createPurchaseDto.supplierId,
      totalQuantity,
      totalWeight,
      totalAmount,
      expectedAt: createPurchaseDto.expectedAt
        ? new Date(createPurchaseDto.expectedAt)
        : undefined,
      remark: createPurchaseDto.remark,
      operatorId,
      items: items as PurchaseOrderItem[],
    });

    return this.purchaseRepository.save(purchase);
  }

  async update(
    id: number,
    updatePurchaseDto: UpdatePurchaseDto,
  ): Promise<PurchaseOrder> {
    const purchase = await this.findOne(id);

    if (
      purchase.status === PurchaseStatus.RECEIVED ||
      purchase.status === PurchaseStatus.CANCELLED
    ) {
      throw new BadRequestException('该采购单状态不可修改');
    }

    await this.purchaseRepository.update(id, updatePurchaseDto);
    return this.findOne(id);
  }

  // 确认采购单
  async confirm(id: number): Promise<PurchaseOrder> {
    const purchase = await this.findOne(id);
    if (purchase.status !== PurchaseStatus.PENDING) {
      throw new BadRequestException('只有待确认的采购单可以确认');
    }
    await this.purchaseRepository.update(id, {
      status: PurchaseStatus.CONFIRMED,
    });
    return this.findOne(id);
  }

  // 收货
  async receive(
    id: number,
    receiveDto: ReceivePurchaseDto,
  ): Promise<PurchaseOrder> {
    const purchase = await this.findOne(id);
    if (purchase.status !== PurchaseStatus.CONFIRMED) {
      throw new BadRequestException('只有已确认的采购单可以收货');
    }

    // 更新各项收货数量
    for (const item of receiveDto.items) {
      await this.purchaseItemRepository.update(item.itemId, {
        receivedQuantity: item.receivedQuantity,
      });
    }

    await this.purchaseRepository.update(id, {
      status: PurchaseStatus.RECEIVED,
      receivedAt: new Date(),
    });

    return this.findOne(id);
  }

  // 取消采购单
  async cancel(id: number): Promise<PurchaseOrder> {
    const purchase = await this.findOne(id);
    if (purchase.status === PurchaseStatus.RECEIVED) {
      throw new BadRequestException('已收货的采购单不可取消');
    }
    await this.purchaseRepository.update(id, {
      status: PurchaseStatus.CANCELLED,
    });
    return this.findOne(id);
  }

  // 付款
  async pay(id: number, payDto: PayPurchaseDto): Promise<PurchaseOrder> {
    const purchase = await this.findOne(id);

    const newPaidAmount = Number(purchase.paidAmount) + payDto.amount;
    const totalAmount = Number(purchase.totalAmount);

    if (newPaidAmount > totalAmount) {
      throw new BadRequestException('付款金额不能超过采购总金额');
    }

    let paymentStatus = PaymentStatus.PARTIAL;
    if (newPaidAmount >= totalAmount) {
      paymentStatus = PaymentStatus.PAID;
    }

    await this.purchaseRepository.update(id, {
      paidAmount: newPaidAmount,
      paymentStatus,
    });

    return this.findOne(id);
  }

  // 获取采购统计
  async getStatistics(startDate?: string, endDate?: string): Promise<any> {
    const where: any = {};
    if (startDate && endDate) {
      where.createdAt = Between(
        new Date(startDate),
        new Date(endDate + ' 23:59:59'),
      );
    }

    const result = await this.purchaseRepository
      .createQueryBuilder('po')
      .select('COUNT(*)', 'totalOrders')
      .addSelect('SUM(po.totalAmount)', 'totalAmount')
      .addSelect('SUM(po.paidAmount)', 'paidAmount')
      .addSelect(
        'SUM(po.totalAmount) - SUM(po.paidAmount)',
        'unpaidAmount',
      )
      .where(where)
      .getRawOne();

    const pendingCount = await this.purchaseRepository.count({
      where: { ...where, status: PurchaseStatus.PENDING },
    });

    const confirmedCount = await this.purchaseRepository.count({
      where: { ...where, status: PurchaseStatus.CONFIRMED },
    });

    return {
      ...result,
      pendingCount,
      confirmedCount,
    };
  }
}

