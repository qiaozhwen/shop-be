import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
} from '@nestjs/common';
import { UserService } from './user.service';
import { User } from './user.entity';

@Controller('users')
export class UserController {
  constructor(private readonly userService: UserService) {}

  @Post()
  async create(@Body() user: Partial<User>): Promise<User> {
    return await this.userService.create(user);
  }

  @Get()
  async findAll(): Promise<User[]> {
    return await this.userService.findAll();
  }

  @Get(':id')
  async findOne(@Param('id') id: number): Promise<User> {
    return await this.userService.findOne(id);
  }

  @Put(':id')
  async update(
    @Param('id') id: number,
    @Body() user: Partial<User>,
  ): Promise<User> {
    return await this.userService.update(id, user);
  }

  @Delete(':id')
  async remove(@Param('id') id: number): Promise<void> {
    return await this.userService.remove(id);
  }
}
