import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  OneToOne,
  ManyToOne,
  JoinColumn,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';
import { Product } from '../product/product.entity';

@Entity('inventory')
export class Inventory {
  @PrimaryGeneratedColumn()
  id: number;

  @OneToOne(() => Product)
  @JoinColumn({ name: 'product_id' })
  product: Product;

  @Column({ name: 'product_id' })
  productId: number;

  @Column({ default: 0 })
  quantity: number;

  @Column('decimal', { name: 'total_weight', precision: 10, scale: 2, default: 0 })
  totalWeight: number;

  @Column({ name: 'min_quantity', default: 0 })
  minQuantity: number;

  @Column({ name: 'max_quantity', nullable: true })
  maxQuantity: number;

  @Column({ name: 'low_stock_alert', default: false })
  lowStockAlert: boolean;

  @Column({ type: 'text', nullable: true })
  notes: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}

export enum InboundType {
  PURCHASE = 'purchase',
  RETURN = 'return',
  ADJUST = 'adjust',
  OTHER = 'other',
}

@Entity('inventory_inbound')
export class InventoryInbound {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'inbound_no', length: 50, unique: true })
  inboundNo: string;

  @Column({ name: 'supplier_id', nullable: true })
  supplierId: number;

  @Column({ name: 'product_id' })
  productId: number;

  @ManyToOne(() => Product)
  @JoinColumn({ name: 'product_id' })
  product: Product;

  @Column()
  quantity: number;

  @Column('decimal', { precision: 10, scale: 2, nullable: true })
  weight: number;

  @Column('decimal', { name: 'unit_price', precision: 10, scale: 2, nullable: true })
  unitPrice: number;

  @Column('decimal', { name: 'total_amount', precision: 10, scale: 2, nullable: true })
  totalAmount: number;

  @Column({ name: 'batch_no', length: 50, nullable: true })
  batchNo: string;

  @Column({
    type: 'enum',
    enum: InboundType,
    default: InboundType.PURCHASE,
  })
  type: InboundType;

  @Column({ length: 500, nullable: true })
  remark: string;

  @Column({ name: 'operator_id' })
  operatorId: number;

  @Column({ name: 'inbound_at' })
  inboundAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

export enum OutboundType {
  SALE = 'sale',
  DAMAGE = 'damage',
  ADJUST = 'adjust',
  OTHER = 'other',
}

@Entity('inventory_outbound')
export class InventoryOutbound {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'outbound_no', length: 50, unique: true })
  outboundNo: string;

  @Column({
    type: 'enum',
    enum: OutboundType,
    default: OutboundType.SALE,
  })
  type: OutboundType;

  @Column({ name: 'order_id', nullable: true })
  orderId: number;

  @Column({ name: 'product_id' })
  productId: number;

  @ManyToOne(() => Product)
  @JoinColumn({ name: 'product_id' })
  product: Product;

  @Column()
  quantity: number;

  @Column('decimal', { precision: 10, scale: 2, nullable: true })
  weight: number;

  @Column({ length: 500, nullable: true })
  reason: string;

  @Column({ name: 'operator_id' })
  operatorId: number;

  @Column({ name: 'outbound_at' })
  outboundAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

export enum AlertLevel {
  WARNING = 'warning',
  CRITICAL = 'critical',
}

@Entity('inventory_alert')
export class InventoryAlert {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'product_id' })
  productId: number;

  @ManyToOne(() => Product)
  @JoinColumn({ name: 'product_id' })
  product: Product;

  @Column({ name: 'current_stock' })
  currentStock: number;

  @Column({ name: 'min_stock' })
  minStock: number;

  @Column({
    name: 'alert_level',
    type: 'enum',
    enum: AlertLevel,
    default: AlertLevel.WARNING,
  })
  alertLevel: AlertLevel;

  @Column({ default: false })
  handled: boolean;

  @Column({ name: 'handled_by', nullable: true })
  handledBy: number;

  @Column({ name: 'handled_at', nullable: true })
  handledAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
