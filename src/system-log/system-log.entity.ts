import {
  Entity,
  PrimaryGeneratedColumn,
  Column,
  CreateDateColumn,
} from 'typeorm';

export enum LogModule {
  AUTH = 'auth',
  PRODUCT = 'product',
  CATEGORY = 'category',
  INVENTORY = 'inventory',
  ORDER = 'order',
  CUSTOMER = 'customer',
  SUPPLIER = 'supplier',
  PURCHASE = 'purchase',
  FINANCE = 'finance',
  SYSTEM = 'system',
}

export enum LogAction {
  CREATE = 'create',
  UPDATE = 'update',
  DELETE = 'delete',
  LOGIN = 'login',
  LOGOUT = 'logout',
  EXPORT = 'export',
  IMPORT = 'import',
  OTHER = 'other',
}

@Entity('system_log')
export class SystemLog {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ name: 'staff_id', nullable: true })
  staffId: number;

  @Column({ name: 'staff_name', length: 50, nullable: true })
  staffName: string;

  @Column({
    type: 'enum',
    enum: LogModule,
  })
  module: LogModule;

  @Column({
    type: 'enum',
    enum: LogAction,
  })
  action: LogAction;

  @Column({ type: 'text', nullable: true })
  content: string;

  @Column({ length: 50, nullable: true })
  ip: string;

  @Column({ name: 'user_agent', length: 500, nullable: true })
  userAgent: string;

  @CreateDateColumn({ name: 'created_at' })
  createdAt: Date;
}

