import {
  Controller,
  Get,
  Post,
  Body,
  Patch,
  Param,
  Delete,
  UseGuards,
} from '@nestjs/common';
import { HistoricalAmountService } from './historical-amount.service';
import { CreateHistoricalAmountDto } from './dto/create-historical-amount.dto';
import { UpdateHistoricalAmountDto } from './dto/update-historical-amount.dto';
import { JwtAuthGuard } from 'src/auth/local-auth.guard';

@Controller('historicalAmount')
@UseGuards(JwtAuthGuard) // 对整个控制器应用认证守卫
export class HistoricalAmountController {
  constructor(
    private readonly historicalAmountService: HistoricalAmountService,
  ) {}

  @Post()
  create(@Body() createHistoricalAmountDto: CreateHistoricalAmountDto) {
    return this.historicalAmountService.create(createHistoricalAmountDto);
  }

  @Get()
  findAll() {
    return this.historicalAmountService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string) {
    return this.historicalAmountService.findOne(+id);
  }

  @Patch(':id')
  update(
    @Param('id') id: string,
    @Body() updateHistoricalAmountDto: UpdateHistoricalAmountDto,
  ) {
    return this.historicalAmountService.update(+id, updateHistoricalAmountDto);
  }

  @Delete(':id')
  remove(@Param('id') id: string) {
    return this.historicalAmountService.remove(+id);
  }
}
