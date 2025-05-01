import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
} from '@nestjs/common';
import { RoleService } from './role.service';
import { Role } from './role.entity';

@Controller('roles')
export class RoleController {
  constructor(private readonly roleService: RoleService) {}

  @Get()
  async findAll(): Promise<Role[]> {
    return await this.roleService.findAll();
  }
  @Post()
  async create(@Body() role: Partial<Role>): Promise<Role> {
    return await this.roleService.create(role);
  }

  @Get(':id')
  async findOne(@Param('id') id: number): Promise<Role> {
    return await this.roleService.findOne(id);
  }

  @Put(':id')
  async update(
    @Param('id') id: number,
    @Body() role: Partial<Role>,
  ): Promise<Role> {
    return await this.roleService.update(id, role);
  }

  @Delete(':id')
  async remove(@Param('id') id: number): Promise<void> {
    return await this.roleService.remove(id);
  }
}
