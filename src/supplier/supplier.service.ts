import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like } from 'typeorm';
import { Supplier } from './supplier.entity';
import {
  CreateSupplierDto,
  UpdateSupplierDto,
  QuerySupplierDto,
} from './dto/supplier.dto';

@Injectable()
export class SupplierService {
  constructor(
    @InjectRepository(Supplier)
    private supplierRepository: Repository<Supplier>,
  ) {}

  async findAll(query?: QuerySupplierDto): Promise<{
    list: Supplier[];
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

    const [list, total] = await this.supplierRepository.findAndCount({
      where,
      order: { rating: 'DESC', id: 'DESC' },
      skip,
      take: pageSize,
    });

    return { list, total, page, pageSize };
  }

  async findActive(): Promise<Supplier[]> {
    return this.supplierRepository.find({
      where: { status: true },
      order: { name: 'ASC' },
    });
  }

  async findOne(id: number): Promise<Supplier> {
    const supplier = await this.supplierRepository.findOne({
      where: { id },
    });
    if (!supplier) {
      throw new NotFoundException(`供应商 #${id} 不存在`);
    }
    return supplier;
  }

  async create(createSupplierDto: CreateSupplierDto): Promise<Supplier> {
    const supplier = this.supplierRepository.create(createSupplierDto);
    return this.supplierRepository.save(supplier);
  }

  async update(id: number, updateSupplierDto: UpdateSupplierDto): Promise<Supplier> {
    await this.supplierRepository.update(id, updateSupplierDto);
    return this.findOne(id);
  }

  async remove(id: number): Promise<void> {
    await this.supplierRepository.update(id, { status: false });
  }

  // 更新采购统计
  async updatePurchaseStats(id: number, amount: number, isPaid: boolean): Promise<void> {
    const supplier = await this.findOne(id);
    const updateData: any = {
      totalPurchase: Number(supplier.totalPurchase) + amount,
    };
    if (!isPaid) {
      updateData.unpaidAmount = Number(supplier.unpaidAmount) + amount;
    }
    await this.supplierRepository.update(id, updateData);
  }

  // 付款给供应商
  async pay(id: number, amount: number): Promise<Supplier> {
    const supplier = await this.findOne(id);
    await this.supplierRepository.update(id, {
      unpaidAmount: Number(supplier.unpaidAmount) - amount,
    });
    return this.findOne(id);
  }
}

