import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository, Between } from 'typeorm';
import { SystemLog, LogModule, LogAction } from './system-log.entity';
import { CreateLogDto, QueryLogDto } from './dto/system-log.dto';

@Injectable()
export class SystemLogService {
  constructor(
    @InjectRepository(SystemLog)
    private logRepository: Repository<SystemLog>,
  ) {}

  async findAll(query?: QueryLogDto): Promise<{
    list: SystemLog[];
    total: number;
    page: number;
    pageSize: number;
  }> {
    const page = query?.page || 1;
    const pageSize = query?.pageSize || 20;
    const skip = (page - 1) * pageSize;

    const where: any = {};
    if (query?.staffId) {
      where.staffId = query.staffId;
    }
    if (query?.module) {
      where.module = query.module;
    }
    if (query?.action) {
      where.action = query.action;
    }
    if (query?.startDate && query?.endDate) {
      where.createdAt = Between(
        new Date(query.startDate),
        new Date(query.endDate + ' 23:59:59'),
      );
    }

    const [list, total] = await this.logRepository.findAndCount({
      where,
      order: { createdAt: 'DESC' },
      skip,
      take: pageSize,
    });

    return { list, total, page, pageSize };
  }

  async create(dto: CreateLogDto): Promise<SystemLog> {
    const log = this.logRepository.create(dto);
    return this.logRepository.save(log);
  }

  // 便捷方法
  async log(
    module: LogModule,
    action: LogAction,
    content: string,
    staffId?: number,
    staffName?: string,
    ip?: string,
    userAgent?: string,
  ): Promise<void> {
    await this.create({
      module,
      action,
      content,
      staffId,
      staffName,
      ip,
      userAgent,
    });
  }

  // 登录日志
  async logLogin(
    staffId: number,
    staffName: string,
    ip?: string,
    userAgent?: string,
  ): Promise<void> {
    await this.log(
      LogModule.AUTH,
      LogAction.LOGIN,
      `用户 ${staffName} 登录系统`,
      staffId,
      staffName,
      ip,
      userAgent,
    );
  }

  // 登出日志
  async logLogout(
    staffId: number,
    staffName: string,
    ip?: string,
  ): Promise<void> {
    await this.log(
      LogModule.AUTH,
      LogAction.LOGOUT,
      `用户 ${staffName} 退出系统`,
      staffId,
      staffName,
      ip,
    );
  }

  // 操作日志
  async logOperation(
    module: LogModule,
    action: LogAction,
    content: string,
    staffId?: number,
    staffName?: string,
  ): Promise<void> {
    await this.log(module, action, content, staffId, staffName);
  }

  // 获取统计
  async getStatistics(): Promise<any> {
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    // 今日日志数
    const todayCount = await this.logRepository.count({
      where: {
        createdAt: Between(today, new Date()),
      },
    });

    // 按模块统计
    const moduleStats = await this.logRepository
      .createQueryBuilder('log')
      .select('log.module', 'module')
      .addSelect('COUNT(*)', 'count')
      .groupBy('log.module')
      .getRawMany();

    // 按操作统计
    const actionStats = await this.logRepository
      .createQueryBuilder('log')
      .select('log.action', 'action')
      .addSelect('COUNT(*)', 'count')
      .groupBy('log.action')
      .getRawMany();

    // 最近登录记录
    const recentLogins = await this.logRepository.find({
      where: {
        module: LogModule.AUTH,
        action: LogAction.LOGIN,
      },
      order: { createdAt: 'DESC' },
      take: 10,
    });

    return {
      todayCount,
      moduleStats,
      actionStats,
      recentLogins,
    };
  }

  // 清理旧日志
  async cleanOldLogs(days = 90): Promise<number> {
    const cutoffDate = new Date();
    cutoffDate.setDate(cutoffDate.getDate() - days);

    const result = await this.logRepository
      .createQueryBuilder()
      .delete()
      .where('createdAt < :cutoffDate', { cutoffDate })
      .execute();

    return result.affected || 0;
  }
}

