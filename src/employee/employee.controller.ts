import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
} from '@nestjs/common';
import { EmployeeService } from './employee.service';
import { Employee } from './employee.entity';

@Controller('employees')
export class EmployeeController {
  constructor(private readonly employeeService: EmployeeService) {}

  // @Post()
  // async create(@Body() employee: Partial<Employee>): Promise<Employee> {
  //   return await this.employeeService.create(employee);
  // }

  @Get()
  async findAll(): Promise<Employee[]> {
    return await this.employeeService.findAll();
  }

  @Get(':id')
  async findOne(@Param('id') id: number): Promise<Employee> {
    return await this.employeeService.findOne(id);
  }

  // @Put(':id')
  // async update(
  //   @Param('id') id: number,
  //   @Body() employee: Partial<Employee>,
  // ): Promise<Employee> {
  //   return await this.employeeService.update(id, employee);
  // }

  // @Delete(':id')
  // async remove(@Param('id') id: number): Promise<void> {
  //   return await this.employeeService.remove(id);
  // }
}
