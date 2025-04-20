import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Region } from './region.entity';

@Injectable()
export class RegionService {
  constructor(
    @InjectRepository(Region)
    private regionRepository: Repository<Region>,
  ) {}

  async findAll(): Promise<Region[]> {
    return this.regionRepository.find();
  }

  async findOne(id: number): Promise<Region> {
    const region = await this.regionRepository.findOne({ where: { id } });
    if (!region) {
      throw new Error('Region not found');
    }
    return region;
  }

  async create(region: Partial<Region>): Promise<Region> {
    const newRegion = this.regionRepository.create(region);
    return this.regionRepository.save(newRegion);
  }

  async update(id: number, region: Partial<Region>): Promise<Region> {
    const updatedRegion = await this.regionRepository.findOne({ where: { id } });
    if (!updatedRegion) {
      throw new Error('Region not found after update');
    }
    return updatedRegion;
  }

  async remove(id: number): Promise<void> {
    await this.regionRepository.delete(id);
  }
}
