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
import { Product } from '../product/product.entity';
import { Customer } from '../customer/customer.entity';

export enum OrderStatus {
  PENDING = 'pending',
  PROCESSING = 'processing',
  COMPLETED = 'completed',
  CANCELLED = 'cancelled',
}

export enum PaymentMethod {
  CASH = 'cash',
  WECHAT = 'wechat',
  ALIPAY = 'alipay',
  CARD = 'card',
  CREDIT = 'credit',
}

export enum PaymentStatus {
  UNPAID = 'unpaid',
  PARTIAL = 'partial',
  PAID = 'paid',
}

@Entity('order')
export class Order {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'order_no', length: 50, unique: true })
  orderNo: string;

  @Column({ name: 'customer_id', nullable: true })
  customerId: number;

  @ManyToOne(() => Customer)
  @JoinColumn({ name: 'customer_id' })
  customer: Customer;

  @Column({ name: 'customer_name', length: 100, nullable: true })
  customerName: string;

  @Column({ name: 'total_quantity', default: 0 })
  totalQuantity: number;

  @Column('decimal', { name: 'total_weight', precision: 10, scale: 2, default: 0 })
  totalWeight: number;

  @Column('decimal', { name: 'total_amount', precision: 10, scale: 2, default: 0 })
  totalAmount: number;

  @Column('decimal', { name: 'discount_amount', precision: 10, scale: 2, default: 0 })
  discountAmount: number;

  @Column('decimal', { name: 'actual_amount', precision: 10, scale: 2, default: 0 })
  actualAmount: number;

  @Column({
    name: 'payment_method',
    type: 'enum',
    enum: PaymentMethod,
    default: PaymentMethod.CASH,
  })
  paymentMethod: PaymentMethod;

  @Column({
    name: 'payment_status',
    type: 'enum',
    enum: PaymentStatus,
    default: PaymentStatus.UNPAID,
  })
  paymentStatus: PaymentStatus;

  @Column('decimal', { name: 'paid_amount', precision: 10, scale: 2, default: 0 })
  paidAmount: number;

  @Column({
    type: 'enum',
    enum: OrderStatus,
    default: OrderStatus.PENDING,
  })
  status: OrderStatus;

  @Column({ length: 500, nullable: true })
  remark: string;

  @Column({ name: 'operator_id' })
  operatorId: number;

  @Column({ name: 'order_at' })
  orderAt: Date;

  @Column({ name: 'completed_at', nullable: true })
  completedAt: Date;

  @OneToMany(() => OrderItem, (item) => item.order, {
    cascade: true,
    eager: true,
  })
  items: OrderItem[];

  @OneToMany(() => OrderPayment, (payment) => payment.order)
  payments: OrderPayment[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}

@Entity('order_item')
export class OrderItem {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'order_id' })
  orderId: number;

  @ManyToOne(() => Order, (order) => order.items)
  @JoinColumn({ name: 'order_id' })
  order: Order;

  @Column({ name: 'product_id' })
  productId: number;

  @ManyToOne(() => Product)
  @JoinColumn({ name: 'product_id' })
  product: Product;

  @Column({ name: 'product_name', length: 100 })
  productName: string;

  @Column({ length: 20 })
  unit: string;

  @Column()
  quantity: number;

  @Column('decimal', { precision: 10, scale: 2, nullable: true })
  weight: number;

  @Column('decimal', { name: 'unit_price', precision: 10, scale: 2 })
  unitPrice: number;

  @Column('decimal', { precision: 10, scale: 2 })
  amount: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

@Entity('order_payment')
export class OrderPayment {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'order_id' })
  orderId: number;

  @ManyToOne(() => Order, (order) => order.payments)
  @JoinColumn({ name: 'order_id' })
  order: Order;

  @Column({
    name: 'payment_method',
    type: 'enum',
    enum: PaymentMethod,
  })
  paymentMethod: PaymentMethod;

  @Column('decimal', { precision: 10, scale: 2 })
  amount: number;

  @Column('decimal', { name: 'received_amount', precision: 10, scale: 2, nullable: true })
  receivedAmount: number;

  @Column('decimal', { name: 'change_amount', precision: 10, scale: 2, nullable: true })
  changeAmount: number;

  @Column({ name: 'transaction_no', length: 100, nullable: true })
  transactionNo: string;

  @Column({ name: 'operator_id' })
  operatorId: number;

  @Column({ name: 'paid_at' })
  paidAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}
