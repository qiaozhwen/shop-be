import {
  Controller,
  Get,
  Post,
  Put,
  Delete,
  Body,
  Param,
} from '@nestjs/common';
import { ProductService } from './product.service';
import { Product } from './product.entity';

@Controller('products')
export class ProductController {
  constructor(private readonly productService: ProductService) {}

  @Get()
  findAll(): Promise<Product[]> {
    return this.productService.findAll();
  }

  @Get(':id')
  findOne(@Param('id') id: string): Promise<Product> {
    return this.productService.findOne(+id);
  }

  // @Post()
  // create(@Body() product: Partial<Product>): Promise<Product> {
  //   return this.productService.create(product);
  // }

  // @Put(':id')
  // update(
  //   @Param('id') id: string,
  //   @Body() product: Partial<Product>,
  // ): Promise<Product> {
  //   return this.productService.update(+id, product);
  // }

  // @Delete(':id')
  // remove(@Param('id') id: string): Promise<void> {
  //   return this.productService.remove(+id);
  // }
}
