import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  ManyToOne,
  OneToMany,
  JoinColumn,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';
import { Supplier } from '../supplier/supplier.entity';
import { Product } from '../product/product.entity';

export enum PurchaseStatus {
  PENDING = 'pending',
  CONFIRMED = 'confirmed',
  RECEIVED = 'received',
  CANCELLED = 'cancelled',
}

export enum PaymentStatus {
  UNPAID = 'unpaid',
  PARTIAL = 'partial',
  PAID = 'paid',
}

@Entity('purchase_order')
export class PurchaseOrder {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'purchase_no', length: 50, unique: true })
  purchaseNo: string;

  @Column({ name: 'supplier_id' })
  supplierId: number;

  @ManyToOne(() => Supplier)
  @JoinColumn({ name: 'supplier_id' })
  supplier: Supplier;

  @Column({ name: 'total_quantity', default: 0 })
  totalQuantity: number;

  @Column('decimal', { name: 'total_weight', precision: 10, scale: 2, default: 0 })
  totalWeight: number;

  @Column('decimal', { name: 'total_amount', precision: 10, scale: 2, default: 0 })
  totalAmount: number;

  @Column('decimal', { name: 'paid_amount', precision: 10, scale: 2, default: 0 })
  paidAmount: number;

  @Column({
    name: 'payment_status',
    type: 'enum',
    enum: PaymentStatus,
    default: PaymentStatus.UNPAID,
  })
  paymentStatus: PaymentStatus;

  @Column({
    type: 'enum',
    enum: PurchaseStatus,
    default: PurchaseStatus.PENDING,
  })
  status: PurchaseStatus;

  @Column({ name: 'expected_at', type: 'date', nullable: true })
  expectedAt: Date;

  @Column({ name: 'received_at', nullable: true })
  receivedAt: Date;

  @Column({ length: 500, nullable: true })
  remark: string;

  @Column({ name: 'operator_id' })
  operatorId: number;

  @OneToMany(() => PurchaseOrderItem, (item) => item.purchaseOrder, {
    cascade: true,
    eager: true,
  })
  items: PurchaseOrderItem[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}

@Entity('purchase_order_item')
export class PurchaseOrderItem {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'purchase_id' })
  purchaseId: number;

  @ManyToOne(() => PurchaseOrder, (order) => order.items)
  @JoinColumn({ name: 'purchase_id' })
  purchaseOrder: PurchaseOrder;

  @Column({ name: 'product_id' })
  productId: number;

  @ManyToOne(() => Product)
  @JoinColumn({ name: 'product_id' })
  product: Product;

  @Column({ name: 'product_name', length: 100 })
  productName: string;

  @Column()
  quantity: number;

  @Column('decimal', { precision: 10, scale: 2, nullable: true })
  weight: number;

  @Column('decimal', { name: 'unit_price', precision: 10, scale: 2 })
  unitPrice: number;

  @Column('decimal', { precision: 10, scale: 2 })
  amount: number;

  @Column({ name: 'received_quantity', default: 0 })
  receivedQuantity: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

