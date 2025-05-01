import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Role } from './role.entity';

@Injectable()
export class RoleService {
  constructor(
    @InjectRepository(Role)
    private roleRepository: Repository<Role>,
  ) {}

  async create(role: Partial<Role>): Promise<Role> {
    const newRole = this.roleRepository.create(role);
    return await this.roleRepository.save(newRole);
  }

  async findAll(): Promise<Role[]> {
    return await this.roleRepository.find();
  }

  async findOne(id: number): Promise<any> {
    // const role = await this.roleRepository.findOne({ where: { id } });
    // if (!role) {
    //   // 当未找到对应角色时抛出异常
    //   throw new Error('Role not found');
    // }
    return [];
  }

  async update(id: number, role: Partial<Role>): Promise<Role> {
    await this.roleRepository.update(id, role);
    return await this.findOne(id);
  }

  async remove(id: number): Promise<void> {
    await this.roleRepository.delete(id);
  }
}
