import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { CreateHistoricalAmountDto } from './dto/create-historical-amount.dto';
import { UpdateHistoricalAmountDto } from './dto/update-historical-amount.dto';
import { HistoricalAmount } from './entities/historical-amount.entity';

@Injectable()
export class HistoricalAmountService {
  constructor(
    @InjectRepository(HistoricalAmount)
    private historicalAmountRepository: Repository<HistoricalAmount>,
  ) {}

  // create(createHistoricalAmountDto: CreateHistoricalAmountDto) {
  //   const newAmount = this.historicalAmountRepository.create(
  //     createHistoricalAmountDto,
  //   );
  //   return this.historicalAmountRepository.save(newAmount);
  // }

  findAll() {
    console.log('findall');
    return this.historicalAmountRepository.find();
  }

  findOne(id: any) {
    return this.historicalAmountRepository.findOneBy({ id });
  }

  async update(
    id: number,
    updateHistoricalAmountDto: UpdateHistoricalAmountDto,
  ) {
    await this.historicalAmountRepository.update(id, updateHistoricalAmountDto);
    return this.findOne(id);
  }

  remove(id: number) {
    return this.historicalAmountRepository.delete(id);
  }

  async create(createDto: CreateHistoricalAmountDto) {
    const dataToCreate = {
      date: createDto.date,
      amounts: createDto.amounts || [],
    };

    const existing = await this.historicalAmountRepository.findOne({
      where: { date: createDto.date },
    });

    if (existing) {
      existing.amounts.push(...dataToCreate.amounts);
      return this.historicalAmountRepository.save(existing);
    }

    return this.historicalAmountRepository.save(dataToCreate);
  }
}
