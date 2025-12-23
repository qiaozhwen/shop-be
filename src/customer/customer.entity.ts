import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  OneToMany,
  ManyToOne,
  JoinColumn,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

export enum CustomerType {
  RESTAURANT = 'restaurant',
  RETAIL = 'retail',
  WHOLESALE = 'wholesale',
  PERSONAL = 'personal',
}

export enum CustomerLevel {
  NORMAL = 'normal',
  VIP = 'vip',
  SVIP = 'svip',
}

@Entity('customer')
export class Customer {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ length: 100 })
  name: string;

  @Column({
    type: 'enum',
    enum: CustomerType,
    default: CustomerType.RESTAURANT,
  })
  type: CustomerType;

  @Column({
    type: 'enum',
    enum: CustomerLevel,
    default: CustomerLevel.NORMAL,
  })
  level: CustomerLevel;

  @Column({ name: 'contact_name', length: 50, nullable: true })
  contactName: string;

  @Column({ length: 20 })
  phone: string;

  @Column({ length: 255, nullable: true })
  address: string;

  @Column('decimal', { name: 'credit_limit', precision: 10, scale: 2, default: 0 })
  creditLimit: number;

  @Column('decimal', { name: 'credit_balance', precision: 10, scale: 2, default: 0 })
  creditBalance: number;

  @Column({ name: 'total_orders', default: 0 })
  totalOrders: number;

  @Column('decimal', { name: 'total_amount', precision: 12, scale: 2, default: 0 })
  totalAmount: number;

  @Column({ name: 'last_order_at', nullable: true })
  lastOrderAt: Date;

  @Column({ length: 500, nullable: true })
  remark: string;

  @Column({ default: true })
  status: boolean;

  @OneToMany(() => CustomerCreditLog, (log) => log.customer)
  creditLogs: CustomerCreditLog[];

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}

@Entity('customer_credit_log')
export class CustomerCreditLog {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'customer_id' })
  customerId: number;

  @Column({
    type: 'enum',
    enum: ['credit', 'repay'],
  })
  type: 'credit' | 'repay';

  @Column('decimal', { precision: 10, scale: 2 })
  amount: number;

  @Column({ name: 'order_id', nullable: true })
  orderId: number;

  @Column('decimal', { name: 'balance_before', precision: 10, scale: 2 })
  balanceBefore: number;

  @Column('decimal', { name: 'balance_after', precision: 10, scale: 2 })
  balanceAfter: number;

  @Column({ length: 255, nullable: true })
  remark: string;

  @Column({ name: 'operator_id' })
  operatorId: number;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @ManyToOne(() => Customer, (customer) => customer.creditLogs)
  @JoinColumn({ name: 'customer_id' })
  customer: Customer;
}
