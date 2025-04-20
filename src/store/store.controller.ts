import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
} from '@nestjs/common';
import { StoreService } from './store.service';
import { Store } from './store.entity';

@Controller('store')
export class StoreController {
  constructor(private readonly storeService: StoreService) {}

  @Post()
  async create(@Body() store: Partial<Store>): Promise<Store> {
    return await this.storeService.create(store);
  }

  @Get()
  async findAll(): Promise<Store[]> {
    return await this.storeService.findAll();
  }

  @Get(':id')
  async findOne(@Param('id') id: number): Promise<Store> {
    return await this.storeService.findOne(id);
  }

  @Put(':id')
  async update(
    @Param('id') id: number,
    @Body() store: Partial<Store>,
  ): Promise<Store> {
    return await this.storeService.update(id, store);
  }

  @Delete(':id')
  async remove(@Param('id') id: number): Promise<void> {
    return await this.storeService.remove(id);
  }
}
