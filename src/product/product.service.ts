import { Injectable, NotFoundException } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Like } from 'typeorm';
import { Product } from './product.entity';
import {
  CreateProductDto,
  UpdateProductDto,
  QueryProductDto,
} from './dto/product.dto';

@Injectable()
export class ProductService {
  constructor(
    @InjectRepository(Product)
    private productRepository: Repository<Product>,
  ) {}

  async findAll(query?: QueryProductDto): Promise<{
    list: Product[];
    total: number;
    page: number;
    pageSize: number;
  }> {
    const page = query?.page || 1;
    const pageSize = query?.pageSize || 20;
    const skip = (page - 1) * pageSize;

    const where: any = {};
    if (query?.categoryId) {
      where.categoryId = query.categoryId;
    }
    if (query?.isActive !== undefined) {
      where.isActive = query.isActive;
    }
    if (query?.keyword) {
      where.name = Like(`%${query.keyword}%`);
    }

    const [list, total] = await this.productRepository.findAndCount({
      where,
      relations: ['category'],
      order: { id: 'DESC' },
      skip,
      take: pageSize,
    });

    return { list, total, page, pageSize };
  }

  async findActive(): Promise<Product[]> {
    return this.productRepository.find({
      where: { isActive: true },
      relations: ['category'],
      order: { categoryId: 'ASC', name: 'ASC' },
    });
  }

  async findOne(id: number): Promise<Product> {
    const product = await this.productRepository.findOne({
      where: { id },
      relations: ['category'],
    });
    if (!product) {
      throw new NotFoundException(`商品 #${id} 不存在`);
    }
    return product;
  }

  async create(createProductDto: CreateProductDto): Promise<Product> {
    const product = this.productRepository.create(createProductDto);
    return this.productRepository.save(product);
  }

  async update(id: number, updateProductDto: UpdateProductDto): Promise<Product> {
    await this.productRepository.update(id, updateProductDto);
    return this.findOne(id);
  }

  async remove(id: number): Promise<void> {
    const product = await this.findOne(id);
    await this.productRepository.remove(product);
  }

  async updateStatus(id: number, isActive: boolean): Promise<Product> {
    await this.productRepository.update(id, { isActive });
    return this.findOne(id);
  }

  async getLowStockProducts(): Promise<Product[]> {
    return this.productRepository
      .createQueryBuilder('product')
      .leftJoinAndSelect('product.category', 'category')
      .leftJoin('inventory', 'inv', 'inv.productId = product.id')
      .where('inv.quantity <= product.minStock')
      .andWhere('product.isActive = :isActive', { isActive: true })
      .getMany();
  }
}
