import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Inventory } from './inventory.entity';

@Injectable()
export class InventoryService {
  constructor(
    @InjectRepository(Inventory)
    private inventoryRepository: Repository<Inventory>,
  ) {}

  async findAll(): Promise<Inventory[]> {
    return this.inventoryRepository.find();
  }

  async findOne(id: number): Promise<Inventory> {
    const inventory = await this.inventoryRepository.findOne({ where: { id } });
    if (!inventory) {
      throw new Error('找不到库存记录');
    }
    return inventory;
  }

  async create(inventory: Partial<Inventory>): Promise<Inventory> {
    const newInventory = this.inventoryRepository.create(inventory);
    return this.inventoryRepository.save(newInventory);
  }

  async update(id: number, inventory: Partial<Inventory>): Promise<Inventory> {
    await this.inventoryRepository.update(id, inventory);
    const updatedInventory = await this.inventoryRepository.findOne({ where: { id } });
    if (!updatedInventory) {
      throw new Error('找不到库存记录');
    }
    return updatedInventory;
  }

  async updateQuantity(id: number, quantity: number): Promise<Inventory> {
    const inventory = await this.inventoryRepository.findOne({ where: { id } });
    if (inventory) {
      inventory.quantity = quantity;
      inventory.lowStockAlert = quantity <= inventory.minQuantity;
      return this.inventoryRepository.save(inventory);
    }
    throw new Error('找不到库存记录');
  }

  async remove(id: number): Promise<void> {
    await this.inventoryRepository.delete(id);
  }
}
