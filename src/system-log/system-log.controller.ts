import {
  Controller,
  Get,
  Post,
  Body,
  Query,
  UseGuards,
} from '@nestjs/common';
import { SystemLogService } from './system-log.service';
import { QueryLogDto } from './dto/system-log.dto';
import { AuthGuard } from '@nestjs/passport';

@Controller('system-log')
@UseGuards(AuthGuard('jwt'))
export class SystemLogController {
  constructor(private readonly logService: SystemLogService) {}

  @Get()
  findAll(@Query() query: QueryLogDto) {
    return this.logService.findAll(query);
  }

  @Get('statistics')
  getStatistics() {
    return this.logService.getStatistics();
  }

  @Post('clean')
  cleanOldLogs(@Body('days') days?: number) {
    return this.logService.cleanOldLogs(days || 90);
  }
}

