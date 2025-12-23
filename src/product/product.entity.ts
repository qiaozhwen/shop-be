import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  ManyToOne,
  JoinColumn,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';
import { Category } from '../category/category.entity';

@Entity('product')
export class Product {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'category_id', nullable: true })
  categoryId: number;

  @ManyToOne(() => Category, (category) => category.products)
  @JoinColumn({ name: 'category_id' })
  category: Category;

  @Column({ length: 50, nullable: true })
  code: string;

  @Column({ length: 100 })
  name: string;

  @Column({ length: 20, default: '只' })
  unit: string;

  @Column('decimal', { precision: 10, scale: 2 })
  price: number;

  @Column('decimal', { name: 'cost_price', precision: 10, scale: 2, nullable: true })
  costPrice: number;

  @Column('decimal', { name: 'weight_avg', precision: 10, scale: 2, nullable: true })
  weightAvg: number;

  @Column({ name: 'image_url', nullable: true })
  imageUrl: string;

  @Column({ type: 'text', nullable: true })
  description: string;

  @Column({ name: 'min_stock', default: 0 })
  minStock: number;

  @Column({ name: 'is_active', default: true })
  isActive: boolean;

  // 兼容旧字段
  @Column({ nullable: true })
  sku: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}
