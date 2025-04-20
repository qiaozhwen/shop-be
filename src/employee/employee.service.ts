import { Injectable } from '@nestjs/common';
import { InjectRepository } from '@nestjs/typeorm';
import { Repository } from 'typeorm';
import { Employee } from './employee.entity';

@Injectable()
export class EmployeeService {
  constructor(
    @InjectRepository(Employee)
    private employeeRepository: Repository<Employee>,
  ) {}

  async create(employee: Partial<Employee>): Promise<Employee> {
    const newEmployee = this.employeeRepository.create(employee);
    return await this.employeeRepository.save(newEmployee);
  }

  async findAll(): Promise<Employee[]> {
    return await this.employeeRepository.find();
  }

  async findOne(id: number): Promise<Employee> {
    const employee = await this.employeeRepository.findOne({ where: { id } });
    if (!employee) {
      // 如果未找到员工记录，抛出异常
      throw new Error(`未找到ID为 ${id} 的员工记录`);
    }
    return employee;
  }

  async update(id: number, employee: Partial<Employee>): Promise<Employee> {
    await this.employeeRepository.update(id, employee);
    return await this.findOne(id);
  }

  async remove(id: number): Promise<void> {
    await this.employeeRepository.delete(id);
  }
}
