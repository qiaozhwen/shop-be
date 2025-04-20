import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Store } from './store.entity';

@Injectable()
export class StoreService {
  constructor(
    @InjectRepository(Store)
    private storeRepository: Repository<Store>,
  ) {}

  async create(store: Partial<Store>): Promise<Store> {
    const newStore = this.storeRepository.create(store);
    return await this.storeRepository.save(newStore);
  }

  async findAll(): Promise<Store[]> {
    return await this.storeRepository.find();
  }

  async findOne(id: number): Promise<Store> {
    const store = await this.storeRepository.findOne({ where: { id } });
    if (!store) {
      // 如果未找到商店则抛出异常
      throw new Error(`未找到ID为${id}的商店`);
    }
    return store;
  }

  async update(id: number, store: Partial<Store>): Promise<Store> {
    await this.storeRepository.update(id, store);
    return await this.findOne(id);
  }

  async remove(id: number): Promise<void> {
    await this.storeRepository.delete(id);
  }
}
