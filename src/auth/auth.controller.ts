import { Controller, Post, Body, UseGuards, Request } from '@nestjs/common';
import { AuthService } from './auth.service';
import { LocalAuthGuard } from './local-auth.guard';
import { UserService } from '../user/user.service';

@Controller('auth')
export class AuthController {
  constructor(
    private authService: AuthService,
    private userService: UserService,
  ) {}

  @UseGuards(LocalAuthGuard)
  @Post('login')
  async login(@Request() req) {
    return this.authService.login(req.user);
  }

  @Post('register')
  async register(
    @Body() createUserDto: { username: string; password: string; name: string },
  ) {
    // 对密码进行加密
    const hashedPassword = await this.authService.hashPassword(
      createUserDto.password,
    );

    // 创建新用户
    const newUser = await this.userService.create({
      ...createUserDto,
      password: hashedPassword,
    });

    // 返回用户信息（不包含密码）
    const { password, ...result } = newUser;
    return result;
  }
}
