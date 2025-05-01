import { Entity, Column, PrimaryGeneratedColumn, OneToMany } from 'typeorm';
import { User } from 'src/user/user.entity';

@Entity('role')
export class Role {
  @PrimaryGeneratedColumn()
  id: number;

  @Column({ length: 50 })
  name: string;

  @Column({ length: 50, unique: true })
  code: string;

  @Column({ type: 'text' })
  description: string;

  @Column({ default: true })
  isActive: boolean;

  @Column({ type: 'timestamp', default: () => 'CURRENT_TIMESTAMP' })
  createdAt: Date;

  @Column({
    type: 'timestamp',
    default: () => 'CURRENT_TIMESTAMP',
    onUpdate: 'CURRENT_TIMESTAMP',
  })
  updatedAt: Date;

  @OneToMany(() => User, (user) => user.id)
  users: User[]; // 类型为 Book 数组
}
