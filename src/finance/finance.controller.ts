import {
  Controller,
  Get,
  Post,
  Body,
  Param,
  Query,
  ParseIntPipe,
  UseGuards,
  Request,
} from '@nestjs/common';
import { FinanceService } from './finance.service';
import { CreateFinanceDto, QueryFinanceDto } from './dto/finance.dto';
import { AuthGuard } from '@nestjs/passport';

@Controller('finance')
@UseGuards(AuthGuard('jwt'))
export class FinanceController {
  constructor(private readonly financeService: FinanceService) {}

  @Get()
  findAll(@Query() query: QueryFinanceDto) {
    return this.financeService.findAll(query);
  }

  @Get('summary')
  getSummary(
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
  ) {
    return this.financeService.getSummary(startDate, endDate);
  }

  @Get('settlements')
  getSettlements(
    @Query('startDate') startDate?: string,
    @Query('endDate') endDate?: string,
  ) {
    return this.financeService.getSettlements(startDate, endDate);
  }

  @Get(':id')
  findOne(@Param('id', ParseIntPipe) id: number) {
    return this.financeService.findOne(id);
  }

  @Post()
  create(@Body() dto: CreateFinanceDto, @Request() req: any) {
    return this.financeService.create(dto, req.user?.id || 1);
  }

  @Post('settle')
  createSettlement(@Body('date') date: string, @Request() req: any) {
    return this.financeService.createDailySettlement(
      new Date(date),
      req.user?.id || 1,
    );
  }
}

