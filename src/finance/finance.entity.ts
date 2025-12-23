import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

export enum FinanceType {
  INCOME = 'income',
  EXPENSE = 'expense',
}

export enum FinanceCategory {
  SALE = 'sale',
  PURCHASE = 'purchase',
  CUSTOMER_REPAY = 'customer_repay',
  SUPPLIER_PAY = 'supplier_pay',
  SALARY = 'salary',
  RENT = 'rent',
  UTILITY = 'utility',
  OTHER = 'other',
}

@Entity('finance_record')
export class FinanceRecord {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'record_no', length: 50, unique: true })
  recordNo: string;

  @Column({
    type: 'enum',
    enum: FinanceType,
  })
  type: FinanceType;

  @Column({
    type: 'enum',
    enum: FinanceCategory,
  })
  category: FinanceCategory;

  @Column('decimal', { precision: 10, scale: 2 })
  amount: number;

  @Column({ name: 'payment_method', length: 20, nullable: true })
  paymentMethod: string;

  @Column({ name: 'related_type', length: 50, nullable: true })
  relatedType: string;

  @Column({ name: 'related_id', nullable: true })
  relatedId: number;

  @Column({ length: 255, nullable: true })
  description: string;

  @Column({ length: 500, nullable: true })
  remark: string;

  @Column({ name: 'operator_id' })
  operatorId: number;

  @Column({ name: 'record_at', type: 'date' })
  recordAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

@Entity('daily_settlement')
export class DailySettlement {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'settle_date', type: 'date', unique: true })
  settleDate: Date;

  @Column({ name: 'total_orders', default: 0 })
  totalOrders: number;

  @Column('decimal', { name: 'total_sales', precision: 12, scale: 2, default: 0 })
  totalSales: number;

  @Column('decimal', { name: 'cash_amount', precision: 10, scale: 2, default: 0 })
  cashAmount: number;

  @Column('decimal', { name: 'wechat_amount', precision: 10, scale: 2, default: 0 })
  wechatAmount: number;

  @Column('decimal', { name: 'alipay_amount', precision: 10, scale: 2, default: 0 })
  alipayAmount: number;

  @Column('decimal', { name: 'card_amount', precision: 10, scale: 2, default: 0 })
  cardAmount: number;

  @Column('decimal', { name: 'credit_amount', precision: 10, scale: 2, default: 0 })
  creditAmount: number;

  @Column('decimal', { name: 'total_expense', precision: 10, scale: 2, default: 0 })
  totalExpense: number;

  @Column('decimal', { precision: 12, scale: 2, default: 0 })
  profit: number;

  @Column({ name: 'operator_id', nullable: true })
  operatorId: number;

  @Column({ name: 'settled_at', nullable: true })
  settledAt: Date;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

