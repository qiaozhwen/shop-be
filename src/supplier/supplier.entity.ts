import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
  UpdateDateColumn,
} from 'typeorm';

@Entity('supplier')
export class Supplier {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ length: 100 })
  name: string;

  @Column({ name: 'contact_name', length: 50, nullable: true })
  contactName: string;

  @Column({ length: 20 })
  phone: string;

  @Column({ length: 255, nullable: true })
  address: string;

  @Column({ name: 'bank_name', length: 100, nullable: true })
  bankName: string;

  @Column({ name: 'bank_account', length: 50, nullable: true })
  bankAccount: string;

  @Column({ name: 'supply_products', length: 255, nullable: true })
  supplyProducts: string;

  @Column('decimal', { name: 'total_purchase', precision: 12, scale: 2, default: 0 })
  totalPurchase: number;

  @Column('decimal', { name: 'unpaid_amount', precision: 10, scale: 2, default: 0 })
  unpaidAmount: number;

  @Column({ default: 5 })
  rating: number;

  @Column({ length: 500, nullable: true })
  remark: string;

  @Column({ default: true })
  status: boolean;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;

  @UpdateDateColumn({ name: 'updated_at' })
  updatedAt: Date;
}

