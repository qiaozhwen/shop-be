import { Module } from '@nestjs/common';
import { HistoricalAmountService } from './historical-amount.service';
import { HistoricalAmountController } from './historical-amount.controller';
import { TypeOrmModule } from '@nestjs/typeorm';
import { HistoricalAmount } from './entities/historical-amount.entity';

@Module({
  imports: [TypeOrmModule.forFeature([HistoricalAmount])],
  controllers: [HistoricalAmountController],
  providers: [HistoricalAmountService],
})
export class HistoricalAmountModule {}
