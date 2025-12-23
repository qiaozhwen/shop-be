# 禽翼鲜生 - 门店管理系统后端

基于 Flask 的门店管理系统后端 API。

## 技术栈

- **框架**: Flask 3.0
- **数据库**: MySQL 8.0 + SQLAlchemy
- **认证**: JWT (Flask-JWT-Extended)
- **部署**: Docker + Gunicorn

## 功能模块

- 🔐 用户认证 (登录/JWT)
- 👥 员工管理
- 📦 商品分类与商品管理
- 📊 库存管理 (入库/出库)
- 👤 客户管理
- 🏭 供应商管理
- 🛒 订单管理
- 📋 采购管理
- 💰 财务管理
- 📈 数据仪表盘

## 快速开始

### 1. 创建虚拟环境

```bash
python -m venv venv
source venv/bin/activate  # macOS/Linux
# 或 venv\Scripts\activate  # Windows
```

### 2. 安装依赖

```bash
pip install -r requirements.txt
```

### 3. 配置环境变量

创建 `.env` 文件：

```env
DB_HOST=localhost
DB_PORT=3306
DB_USERNAME=root
DB_PASSWORD=your_password
DB_DATABASE=freshbird
JWT_SECRET=your-secret-key
```

### 4. 初始化数据库

```bash
mysql -u root -p < sql/init.sql
```

### 5. 运行开发服务器

```bash
python run.py
```

服务将在 http://localhost:3000 启动。

## API 接口

### 认证
- `POST /api/auth/login` - 用户登录
- `GET /api/auth/profile` - 获取当前用户信息
- `POST /api/auth/logout` - 用户登出
- `POST /api/auth/change-password` - 修改密码

### 员工
- `GET /api/staff` - 员工列表
- `POST /api/staff` - 创建员工
- `PUT /api/staff/:id` - 更新员工
- `DELETE /api/staff/:id` - 删除员工

### 商品分类
- `GET /api/category` - 分类列表
- `POST /api/category` - 创建分类
- `PUT /api/category/:id` - 更新分类
- `DELETE /api/category/:id` - 删除分类

### 商品
- `GET /api/product` - 商品列表
- `POST /api/product` - 创建商品
- `PUT /api/product/:id` - 更新商品
- `DELETE /api/product/:id` - 删除商品

### 库存
- `GET /api/inventory` - 库存列表
- `POST /api/inventory/inbound` - 入库
- `POST /api/inventory/outbound` - 出库

### 客户
- `GET /api/customer` - 客户列表
- `POST /api/customer` - 创建客户
- `PUT /api/customer/:id` - 更新客户
- `DELETE /api/customer/:id` - 删除客户

### 供应商
- `GET /api/supplier` - 供应商列表
- `POST /api/supplier` - 创建供应商
- `PUT /api/supplier/:id` - 更新供应商
- `DELETE /api/supplier/:id` - 删除供应商

### 订单
- `GET /api/order` - 订单列表
- `POST /api/order` - 创建订单
- `POST /api/order/:id/pay` - 订单支付
- `POST /api/order/:id/cancel` - 取消订单

### 采购
- `GET /api/purchase` - 采购单列表
- `POST /api/purchase` - 创建采购单
- `POST /api/purchase/:id/receive` - 采购收货
- `POST /api/purchase/:id/cancel` - 取消采购

### 财务
- `GET /api/finance/records` - 财务流水
- `POST /api/finance/records` - 创建财务记录
- `GET /api/finance/summary` - 财务汇总

### 仪表盘
- `GET /api/dashboard/overview` - 概览数据
- `GET /api/dashboard/sales-trend` - 销售趋势
- `GET /api/dashboard/top-products` - 热销商品
- `GET /api/dashboard/low-stock` - 库存预警

## Docker 部署

```bash
docker build -t shop-be .
docker run -d -p 3000:3000 --name shop-be shop-be
```

## 默认账号

- 用户名: `qiaozhen`
- 密码: `123456`

## License

MIT
