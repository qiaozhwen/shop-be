import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
} from '@nestjs/common';
import { RegionService } from './region.service';
import { Region } from './region.entity';

@Controller('regions')
export class RegionController {
  constructor(private readonly regionService: RegionService) {}

  @Get()
  findAll(): Promise<Region[]> {
    return this.regionService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string): Promise<Region> {
    return this.regionService.findOne(+id);
  }

  // @Post()
  // create(@Body() region: Partial<Region>): Promise<Region> {
  //   return this.regionService.create(region);
  // }

  // @Put(':id')
  // update(
  //   @Param('id') id: string,
  //   @Body() region: Partial<Region>,
  // ): Promise<Region> {
  //   return this.regionService.update(+id, region);
  // }

  // @Delete(':id')
  // remove(@Param('id') id: string): Promise<void> {
  //   return this.regionService.remove(+id);
  // }
}
