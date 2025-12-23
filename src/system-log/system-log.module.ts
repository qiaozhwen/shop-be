import { Module, Global } from '@nestjs/common';
import { TypeOrmModule } from '@nestjs/typeorm';
import { SystemLog } from './system-log.entity';
import { SystemLogService } from './system-log.service';
import { SystemLogController } from './system-log.controller';

@Global()
@Module({
  imports: [TypeOrmModule.forFeature([SystemLog])],
  controllers: [SystemLogController],
  providers: [SystemLogService],
  exports: [SystemLogService],
})
export class SystemLogModule {}

