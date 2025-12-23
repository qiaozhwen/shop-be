import {
  Injectable,
  NotFoundException,
  BadRequestException,
} from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between, LessThanOrEqual } from 'typeorm';
import {
  Inventory,
  InventoryInbound,
  InventoryOutbound,
  InventoryAlert,
  InboundType,
  OutboundType,
  AlertLevel,
} from './inventory.entity';
import {
  CreateInboundDto,
  CreateOutboundDto,
  QueryInboundDto,
  QueryOutboundDto,
  UpdateInventoryDto,
} from './dto/inventory.dto';

@Injectable()
export class InventoryService {
  constructor(
    @InjectRepository(Inventory)
    private inventoryRepository: Repository<Inventory>,
    @InjectRepository(InventoryInbound)
    private inboundRepository: Repository<InventoryInbound>,
    @InjectRepository(InventoryOutbound)
    private outboundRepository: Repository<InventoryOutbound>,
    @InjectRepository(InventoryAlert)
    private alertRepository: Repository<InventoryAlert>,
  ) {}

  private generateNo(prefix: string): string {
    const now = new Date();
    const dateStr = now.toISOString().slice(0, 10).replace(/-/g, '');
    const random = Math.floor(Math.random() * 10000)
      .toString()
      .padStart(4, '0');
    return `${prefix}${dateStr}${random}`;
  }

  // ========== 库存管理 ==========

  async findAll(): Promise<Inventory[]> {
    return this.inventoryRepository.find({
      relations: ['product', 'product.category'],
      order: { productId: 'ASC' },
    });
  }

  async findByProduct(productId: number): Promise<Inventory | null> {
    return this.inventoryRepository.findOne({
      where: { productId },
      relations: ['product'],
    });
  }

  async getOrCreateInventory(productId: number): Promise<Inventory> {
    let inventory = await this.findByProduct(productId);
    if (!inventory) {
      inventory = this.inventoryRepository.create({
        productId,
        quantity: 0,
        totalWeight: 0,
      });
      inventory = await this.inventoryRepository.save(inventory);
    }
    return inventory;
  }

  async updateInventory(
    productId: number,
    dto: UpdateInventoryDto,
  ): Promise<Inventory> {
    const inventory = await this.getOrCreateInventory(productId);
    await this.inventoryRepository.update(inventory.id, dto);
    return this.findByProduct(productId) as Promise<Inventory>;
  }

  // ========== 入库管理 ==========

  async createInbound(
    dto: CreateInboundDto,
    operatorId: number,
  ): Promise<InventoryInbound> {
    const inventory = await this.getOrCreateInventory(dto.productId);

    // 创建入库记录
    const inbound = this.inboundRepository.create({
      inboundNo: this.generateNo('IN'),
      productId: dto.productId,
      quantity: dto.quantity,
      weight: dto.weight,
      unitPrice: dto.unitPrice,
      totalAmount: dto.unitPrice ? dto.unitPrice * dto.quantity : undefined,
      supplierId: dto.supplierId,
      batchNo: dto.batchNo,
      type: dto.type || InboundType.PURCHASE,
      remark: dto.remark,
      operatorId,
      inboundAt: new Date(),
    });
    await this.inboundRepository.save(inbound);

    // 更新库存
    await this.inventoryRepository.update(inventory.id, {
      quantity: inventory.quantity + dto.quantity,
      totalWeight: Number(inventory.totalWeight) + (dto.weight || 0),
    });

    // 检查是否需要解除预警
    await this.checkAndUpdateAlert(dto.productId);

    return inbound;
  }

  async findInbounds(query?: QueryInboundDto): Promise<{
    list: InventoryInbound[];
    total: number;
    page: number;
    pageSize: number;
  }> {
    const page = query?.page || 1;
    const pageSize = query?.pageSize || 20;
    const skip = (page - 1) * pageSize;

    const where: any = {};
    if (query?.productId) {
      where.productId = query.productId;
    }
    if (query?.supplierId) {
      where.supplierId = query.supplierId;
    }
    if (query?.type) {
      where.type = query.type;
    }
    if (query?.startDate && query?.endDate) {
      where.inboundAt = Between(
        new Date(query.startDate),
        new Date(query.endDate + ' 23:59:59'),
      );
    }

    const [list, total] = await this.inboundRepository.findAndCount({
      where,
      relations: ['product'],
      order: { inboundAt: 'DESC' },
      skip,
      take: pageSize,
    });

    return { list, total, page, pageSize };
  }

  // ========== 出库管理 ==========

  async createOutbound(
    dto: CreateOutboundDto,
    operatorId: number,
  ): Promise<InventoryOutbound> {
    const inventory = await this.getOrCreateInventory(dto.productId);

    if (inventory.quantity < dto.quantity) {
      throw new BadRequestException('库存不足');
    }

    // 创建出库记录
    const outbound = this.outboundRepository.create({
      outboundNo: this.generateNo('OUT'),
      productId: dto.productId,
      quantity: dto.quantity,
      weight: dto.weight,
      type: dto.type,
      orderId: dto.orderId,
      reason: dto.reason,
      operatorId,
      outboundAt: new Date(),
    });
    await this.outboundRepository.save(outbound);

    // 更新库存
    await this.inventoryRepository.update(inventory.id, {
      quantity: inventory.quantity - dto.quantity,
      totalWeight: Number(inventory.totalWeight) - (dto.weight || 0),
    });

    // 检查是否需要库存预警
    await this.checkAndCreateAlert(dto.productId);

    return outbound;
  }

  async findOutbounds(query?: QueryOutboundDto): Promise<{
    list: InventoryOutbound[];
    total: number;
    page: number;
    pageSize: number;
  }> {
    const page = query?.page || 1;
    const pageSize = query?.pageSize || 20;
    const skip = (page - 1) * pageSize;

    const where: any = {};
    if (query?.productId) {
      where.productId = query.productId;
    }
    if (query?.type) {
      where.type = query.type;
    }
    if (query?.startDate && query?.endDate) {
      where.outboundAt = Between(
        new Date(query.startDate),
        new Date(query.endDate + ' 23:59:59'),
      );
    }

    const [list, total] = await this.outboundRepository.findAndCount({
      where,
      relations: ['product'],
      order: { outboundAt: 'DESC' },
      skip,
      take: pageSize,
    });

    return { list, total, page, pageSize };
  }

  // ========== 库存预警 ==========

  async checkAndCreateAlert(productId: number): Promise<void> {
    const inventory = await this.inventoryRepository.findOne({
      where: { productId },
      relations: ['product'],
    });

    if (!inventory || !inventory.product) return;

    const minStock = inventory.minQuantity || inventory.product.minStock || 0;
    if (minStock <= 0) return;

    if (inventory.quantity <= minStock) {
      // 检查是否已存在未处理的预警
      const existingAlert = await this.alertRepository.findOne({
        where: { productId, handled: false },
      });

      if (!existingAlert) {
        const alertLevel =
          inventory.quantity <= minStock * 0.5
            ? AlertLevel.CRITICAL
            : AlertLevel.WARNING;

        const alert = this.alertRepository.create({
          productId,
          currentStock: inventory.quantity,
          minStock,
          alertLevel,
        });
        await this.alertRepository.save(alert);
      }
    }
  }

  async checkAndUpdateAlert(productId: number): Promise<void> {
    const inventory = await this.inventoryRepository.findOne({
      where: { productId },
      relations: ['product'],
    });

    if (!inventory || !inventory.product) return;

    const minStock = inventory.minQuantity || inventory.product.minStock || 0;

    if (inventory.quantity > minStock) {
      // 自动解除预警
      await this.alertRepository.update(
        { productId, handled: false },
        { handled: true, handledAt: new Date() },
      );
    }
  }

  async findAlerts(handled?: boolean): Promise<InventoryAlert[]> {
    const where: any = {};
    if (handled !== undefined) {
      where.handled = handled;
    }
    return this.alertRepository.find({
      where,
      relations: ['product'],
      order: { createdAt: 'DESC' },
    });
  }

  async handleAlert(id: number, operatorId: number): Promise<InventoryAlert> {
    const alert = await this.alertRepository.findOne({ where: { id } });
    if (!alert) {
      throw new NotFoundException(`预警记录 #${id} 不存在`);
    }
    await this.alertRepository.update(id, {
      handled: true,
      handledBy: operatorId,
      handledAt: new Date(),
    });
    return this.alertRepository.findOne({ where: { id } }) as Promise<InventoryAlert>;
  }

  // ========== 统计 ==========

  async getOverview(): Promise<any> {
    const totalProducts = await this.inventoryRepository.count();

    const stockStats = await this.inventoryRepository
      .createQueryBuilder('inv')
      .select('SUM(inv.quantity)', 'totalQuantity')
      .addSelect('SUM(inv.totalWeight)', 'totalWeight')
      .getRawOne();

    const lowStockCount = await this.inventoryRepository
      .createQueryBuilder('inv')
      .leftJoin('inv.product', 'product')
      .where('inv.quantity <= COALESCE(inv.minQuantity, product.minStock, 0)')
      .andWhere('COALESCE(inv.minQuantity, product.minStock, 0) > 0')
      .getCount();

    const pendingAlerts = await this.alertRepository.count({
      where: { handled: false },
    });

    // 今日入库
    const today = new Date();
    today.setHours(0, 0, 0, 0);
    const todayInbound = await this.inboundRepository
      .createQueryBuilder('inbound')
      .select('SUM(inbound.quantity)', 'quantity')
      .where('inbound.inboundAt >= :today', { today })
      .getRawOne();

    // 今日出库
    const todayOutbound = await this.outboundRepository
      .createQueryBuilder('outbound')
      .select('SUM(outbound.quantity)', 'quantity')
      .where('outbound.outboundAt >= :today', { today })
      .getRawOne();

    return {
      totalProducts,
      totalQuantity: stockStats?.totalQuantity || 0,
      totalWeight: stockStats?.totalWeight || 0,
      lowStockCount,
      pendingAlerts,
      todayInbound: todayInbound?.quantity || 0,
      todayOutbound: todayOutbound?.quantity || 0,
    };
  }
}
