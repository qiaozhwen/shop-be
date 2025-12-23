-- 创建新数据库
CREATE DATABASE IF NOT EXISTS `freshbird` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE `freshbird`;

-- =============================================
-- 1. 用户与权限模块
-- =============================================

-- 员工/用户表
CREATE TABLE `staff` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '员工ID',
  `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
  `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
  `name` VARCHAR(50) NOT NULL COMMENT '姓名',
  `phone` VARCHAR(20) DEFAULT NULL COMMENT '手机号',
  `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
  `role` ENUM('admin', 'manager', 'cashier', 'warehouse') NOT NULL DEFAULT 'cashier' COMMENT '角色',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `last_login_at` DATETIME DEFAULT NULL COMMENT '最后登录时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_phone` (`phone`),
  INDEX `idx_role` (`role`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工表';

-- 系统日志表
CREATE TABLE `system_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `staff_id` BIGINT DEFAULT NULL COMMENT '操作人ID',
  `staff_name` VARCHAR(50) DEFAULT NULL COMMENT '操作人姓名',
  `module` ENUM('auth', 'product', 'category', 'inventory', 'order', 'customer', 'supplier', 'purchase', 'finance', 'system') NOT NULL COMMENT '模块',
  `action` ENUM('create', 'update', 'delete', 'login', 'logout', 'export', 'import', 'other') NOT NULL COMMENT '操作',
  `content` TEXT DEFAULT NULL COMMENT '操作内容描述',
  `ip` VARCHAR(50) DEFAULT NULL COMMENT 'IP地址',
  `user_agent` VARCHAR(500) DEFAULT NULL COMMENT '浏览器信息',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_staff` (`staff_id`),
  INDEX `idx_module` (`module`),
  INDEX `idx_action` (`action`),
  INDEX `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='系统日志表';

-- =============================================
-- 2. 商品管理模块
-- =============================================

-- 商品分类表
CREATE TABLE `category` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL COMMENT '分类名称',
  `icon` VARCHAR(50) DEFAULT NULL COMMENT '图标',
  `sort` INT NOT NULL DEFAULT 0 COMMENT '排序',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品分类表';

-- 商品表(活禽)
CREATE TABLE `product` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `category_id` BIGINT DEFAULT NULL COMMENT '分类ID',
  `code` VARCHAR(50) DEFAULT NULL UNIQUE COMMENT '商品编码',
  `name` VARCHAR(100) NOT NULL COMMENT '商品名称',
  `unit` VARCHAR(20) NOT NULL DEFAULT '只' COMMENT '单位: 只/斤',
  `price` DECIMAL(10,2) NOT NULL COMMENT '销售单价',
  `cost_price` DECIMAL(10,2) DEFAULT NULL COMMENT '成本价',
  `weight_avg` DECIMAL(10,2) DEFAULT NULL COMMENT '平均重量(斤)',
  `image_url` VARCHAR(255) DEFAULT NULL COMMENT '商品图片',
  `description` TEXT DEFAULT NULL COMMENT '商品描述',
  `min_stock` INT NOT NULL DEFAULT 0 COMMENT '最低库存预警值',
  `is_active` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-下架, 1-上架',
  `sku` VARCHAR(50) DEFAULT NULL COMMENT 'SKU',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_category` (`category_id`),
  INDEX `idx_is_active` (`is_active`),
  INDEX `idx_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='商品表';

-- =============================================
-- 3. 库存管理模块
-- =============================================

-- 库存表
CREATE TABLE `inventory` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `quantity` INT NOT NULL DEFAULT 0 COMMENT '当前库存数量',
  `total_weight` DECIMAL(10,2) DEFAULT 0 COMMENT '总重量(斤)',
  `min_quantity` INT DEFAULT 0 COMMENT '最低库存',
  `max_quantity` INT DEFAULT NULL COMMENT '最高库存',
  `low_stock_alert` TINYINT DEFAULT 0 COMMENT '低库存预警',
  `notes` TEXT DEFAULT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY `uk_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存表';

-- 入库记录表
CREATE TABLE `inventory_inbound` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `inbound_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '入库单号',
  `supplier_id` BIGINT DEFAULT NULL COMMENT '供应商ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `quantity` INT NOT NULL COMMENT '入库数量',
  `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '入库重量(斤)',
  `unit_price` DECIMAL(10,2) DEFAULT NULL COMMENT '采购单价',
  `total_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '采购总额',
  `batch_no` VARCHAR(50) DEFAULT NULL COMMENT '批次号',
  `type` ENUM('purchase', 'return', 'adjust', 'other') NOT NULL DEFAULT 'purchase' COMMENT '入库类型',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `inbound_at` DATETIME NOT NULL COMMENT '入库时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_product` (`product_id`),
  INDEX `idx_supplier` (`supplier_id`),
  INDEX `idx_inbound_at` (`inbound_at`),
  INDEX `idx_type` (`type`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='入库记录表';

-- 出库记录表
CREATE TABLE `inventory_outbound` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `outbound_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '出库单号',
  `type` ENUM('sale', 'damage', 'adjust', 'other') NOT NULL DEFAULT 'sale' COMMENT '出库类型',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID(销售出库)',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `quantity` INT NOT NULL COMMENT '出库数量',
  `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '出库重量(斤)',
  `reason` VARCHAR(500) DEFAULT NULL COMMENT '出库原因',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `outbound_at` DATETIME NOT NULL COMMENT '出库时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_product` (`product_id`),
  INDEX `idx_order` (`order_id`),
  INDEX `idx_type` (`type`),
  INDEX `idx_outbound_at` (`outbound_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='出库记录表';

-- 库存预警记录表
CREATE TABLE `inventory_alert` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `current_stock` INT NOT NULL COMMENT '当前库存',
  `min_stock` INT NOT NULL COMMENT '最低库存',
  `alert_level` ENUM('warning', 'critical') NOT NULL DEFAULT 'warning' COMMENT '预警级别',
  `handled` TINYINT NOT NULL DEFAULT 0 COMMENT '处理状态: 0-未处理, 1-已处理',
  `handled_by` BIGINT DEFAULT NULL COMMENT '处理人ID',
  `handled_at` DATETIME DEFAULT NULL COMMENT '处理时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_product` (`product_id`),
  INDEX `idx_handled` (`handled`),
  INDEX `idx_alert_level` (`alert_level`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='库存预警表';

-- =============================================
-- 4. 客户管理模块
-- =============================================

-- 客户表
CREATE TABLE `customer` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '客户名称(店名/个人名)',
  `type` ENUM('restaurant', 'retail', 'wholesale', 'personal') NOT NULL DEFAULT 'restaurant' COMMENT '客户类型',
  `level` ENUM('normal', 'vip', 'svip') NOT NULL DEFAULT 'normal' COMMENT '客户等级',
  `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `credit_limit` DECIMAL(10,2) DEFAULT 0 COMMENT '赊账额度',
  `credit_balance` DECIMAL(10,2) DEFAULT 0 COMMENT '当前欠款',
  `total_orders` INT DEFAULT 0 COMMENT '累计订单数',
  `total_amount` DECIMAL(12,2) DEFAULT 0 COMMENT '累计消费金额',
  `last_order_at` DATETIME DEFAULT NULL COMMENT '最后下单时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_phone` (`phone`),
  INDEX `idx_type` (`type`),
  INDEX `idx_level` (`level`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户表';

-- 客户欠款记录表
CREATE TABLE `customer_credit_log` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `customer_id` BIGINT NOT NULL COMMENT '客户ID',
  `type` ENUM('credit', 'repay') NOT NULL COMMENT '类型: credit-赊账, repay-还款',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
  `order_id` BIGINT DEFAULT NULL COMMENT '关联订单ID',
  `balance_before` DECIMAL(10,2) NOT NULL COMMENT '变动前余额',
  `balance_after` DECIMAL(10,2) NOT NULL COMMENT '变动后余额',
  `remark` VARCHAR(255) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_customer` (`customer_id`),
  INDEX `idx_type` (`type`),
  INDEX `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='客户欠款记录表';

-- =============================================
-- 5. 订单管理模块
-- =============================================

-- 订单主表
CREATE TABLE `order` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '订单号',
  `customer_id` BIGINT DEFAULT NULL COMMENT '客户ID',
  `customer_name` VARCHAR(100) DEFAULT NULL COMMENT '客户名称(冗余)',
  `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '商品总数量',
  `total_weight` DECIMAL(10,2) DEFAULT 0 COMMENT '商品总重量',
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '订单总金额',
  `discount_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '优惠金额',
  `actual_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '实付金额',
  `payment_method` ENUM('cash', 'wechat', 'alipay', 'card', 'credit') NOT NULL DEFAULT 'cash' COMMENT '支付方式',
  `payment_status` ENUM('unpaid', 'partial', 'paid') NOT NULL DEFAULT 'unpaid' COMMENT '支付状态',
  `paid_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '已付金额',
  `status` ENUM('pending', 'processing', 'completed', 'cancelled') NOT NULL DEFAULT 'pending' COMMENT '订单状态',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT NOT NULL COMMENT '开单人ID',
  `order_at` DATETIME NOT NULL COMMENT '下单时间',
  `completed_at` DATETIME DEFAULT NULL COMMENT '完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_customer` (`customer_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_order_at` (`order_at`),
  INDEX `idx_payment_status` (`payment_status`),
  INDEX `idx_order_query` (`status`, `order_at`, `customer_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单主表';

-- 订单明细表
CREATE TABLE `order_item` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称(冗余)',
  `unit` VARCHAR(20) NOT NULL DEFAULT '只' COMMENT '单位',
  `quantity` INT NOT NULL COMMENT '数量',
  `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '重量(斤)',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '单价',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_order` (`order_id`),
  INDEX `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单明细表';

-- 订单支付记录表
CREATE TABLE `order_payment` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `order_id` BIGINT NOT NULL COMMENT '订单ID',
  `payment_method` ENUM('cash', 'wechat', 'alipay', 'card', 'credit') NOT NULL COMMENT '支付方式',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额',
  `received_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '实收金额(现金)',
  `change_amount` DECIMAL(10,2) DEFAULT NULL COMMENT '找零金额(现金)',
  `transaction_no` VARCHAR(100) DEFAULT NULL COMMENT '交易流水号',
  `operator_id` BIGINT NOT NULL COMMENT '收款人ID',
  `paid_at` DATETIME NOT NULL COMMENT '支付时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_order` (`order_id`),
  INDEX `idx_paid_at` (`paid_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='订单支付记录表';

-- =============================================
-- 6. 供应商与采购模块
-- =============================================

-- 供应商表
CREATE TABLE `supplier` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL COMMENT '供应商名称',
  `contact_name` VARCHAR(50) DEFAULT NULL COMMENT '联系人',
  `phone` VARCHAR(20) NOT NULL COMMENT '联系电话',
  `address` VARCHAR(255) DEFAULT NULL COMMENT '地址',
  `bank_name` VARCHAR(100) DEFAULT NULL COMMENT '开户银行',
  `bank_account` VARCHAR(50) DEFAULT NULL COMMENT '银行账号',
  `supply_products` VARCHAR(255) DEFAULT NULL COMMENT '主营商品',
  `total_purchase` DECIMAL(12,2) DEFAULT 0 COMMENT '累计采购金额',
  `unpaid_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '待付款金额',
  `rating` TINYINT DEFAULT 5 COMMENT '评分: 1-5',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `status` TINYINT NOT NULL DEFAULT 1 COMMENT '状态: 0-禁用, 1-启用',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_phone` (`phone`),
  INDEX `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='供应商表';

-- 采购订单表
CREATE TABLE `purchase_order` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `purchase_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '采购单号',
  `supplier_id` BIGINT NOT NULL COMMENT '供应商ID',
  `total_quantity` INT NOT NULL DEFAULT 0 COMMENT '采购总数量',
  `total_weight` DECIMAL(10,2) DEFAULT 0 COMMENT '采购总重量',
  `total_amount` DECIMAL(10,2) NOT NULL DEFAULT 0 COMMENT '采购总金额',
  `paid_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '已付金额',
  `payment_status` ENUM('unpaid', 'partial', 'paid') NOT NULL DEFAULT 'unpaid' COMMENT '付款状态',
  `status` ENUM('pending', 'confirmed', 'received', 'cancelled') NOT NULL DEFAULT 'pending' COMMENT '采购状态',
  `expected_at` DATE DEFAULT NULL COMMENT '预计到货日期',
  `received_at` DATETIME DEFAULT NULL COMMENT '实际到货时间',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT NOT NULL COMMENT '采购员ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX `idx_supplier` (`supplier_id`),
  INDEX `idx_status` (`status`),
  INDEX `idx_payment_status` (`payment_status`),
  INDEX `idx_created` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购订单表';

-- 采购明细表
CREATE TABLE `purchase_order_item` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `purchase_id` BIGINT NOT NULL COMMENT '采购单ID',
  `product_id` BIGINT NOT NULL COMMENT '商品ID',
  `product_name` VARCHAR(100) NOT NULL COMMENT '商品名称(冗余)',
  `quantity` INT NOT NULL COMMENT '采购数量',
  `weight` DECIMAL(10,2) DEFAULT NULL COMMENT '采购重量(斤)',
  `unit_price` DECIMAL(10,2) NOT NULL COMMENT '采购单价',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '小计金额',
  `received_quantity` INT DEFAULT 0 COMMENT '已收货数量',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_purchase` (`purchase_id`),
  INDEX `idx_product` (`product_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='采购明细表';

-- =============================================
-- 7. 财务管理模块
-- =============================================

-- 财务流水表
CREATE TABLE `finance_record` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `record_no` VARCHAR(50) NOT NULL UNIQUE COMMENT '流水号',
  `type` ENUM('income', 'expense') NOT NULL COMMENT '类型: income-收入, expense-支出',
  `category` ENUM('sale', 'purchase', 'customer_repay', 'supplier_pay', 'salary', 'rent', 'utility', 'other') NOT NULL COMMENT '分类',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '金额',
  `payment_method` VARCHAR(20) DEFAULT NULL COMMENT '支付方式',
  `related_type` VARCHAR(50) DEFAULT NULL COMMENT '关联类型: order/purchase/customer_repay',
  `related_id` BIGINT DEFAULT NULL COMMENT '关联ID',
  `description` VARCHAR(255) DEFAULT NULL COMMENT '描述',
  `remark` VARCHAR(500) DEFAULT NULL COMMENT '备注',
  `operator_id` BIGINT NOT NULL COMMENT '操作人ID',
  `record_at` DATE NOT NULL COMMENT '记账日期',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_type` (`type`),
  INDEX `idx_category` (`category`),
  INDEX `idx_record_at` (`record_at`),
  INDEX `idx_finance_stat` (`record_at`, `type`, `category`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='财务流水表';

-- 日结算表
CREATE TABLE `daily_settlement` (
  `id` BIGINT PRIMARY KEY AUTO_INCREMENT,
  `settle_date` DATE NOT NULL UNIQUE COMMENT '结算日期',
  `total_orders` INT DEFAULT 0 COMMENT '订单数',
  `total_sales` DECIMAL(12,2) DEFAULT 0 COMMENT '销售总额',
  `cash_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '现金收入',
  `wechat_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '微信收入',
  `alipay_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '支付宝收入',
  `card_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '刷卡收入',
  `credit_amount` DECIMAL(10,2) DEFAULT 0 COMMENT '赊账金额',
  `total_expense` DECIMAL(10,2) DEFAULT 0 COMMENT '支出总额',
  `profit` DECIMAL(12,2) DEFAULT 0 COMMENT '利润',
  `operator_id` BIGINT DEFAULT NULL COMMENT '结算人ID',
  `settled_at` DATETIME DEFAULT NULL COMMENT '结算时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX `idx_settle_date` (`settle_date`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='日结算表';

-- =============================================
-- 8. 初始数据
-- =============================================

-- 插入默认管理员 (密码使用 bcrypt 加密，cost=10)
-- 账号: qiaozhen / 密码: ******** (已加密存储)
INSERT INTO `staff` (`username`, `password`, `name`, `phone`, `role`, `status`) VALUES
('qiaozhen', '$2b$10$8bGqkR3BPsReKnZFCCH9lObS/TsIpA4r3JyLREinnKRIs2KuXUtM.', '乔振', '13800138000', 'admin', 1);

-- 插入默认分类
INSERT INTO `category` (`name`, `icon`, `sort`, `status`) VALUES
('鸡类', '🐔', 1, 1),
('鸭类', '🦆', 2, 1),
('鸽类', '🕊️', 3, 1),
('鹅类', '🦢', 4, 1);

-- 插入示例商品
INSERT INTO `product` (`category_id`, `code`, `name`, `unit`, `price`, `cost_price`, `min_stock`, `is_active`) VALUES
(1, 'TJ001', '散养土鸡', '只', 45.00, 35.00, 50, 1),
(1, 'SHJ001', '三黄鸡', '只', 35.00, 28.00, 50, 1),
(1, 'WJ001', '乌鸡', '只', 58.00, 45.00, 30, 1),
(1, 'BYJ001', '白羽鸡', '只', 28.00, 22.00, 60, 1),
(2, 'MY001', '麻鸭', '只', 38.00, 30.00, 40, 1),
(2, 'FY001', '番鸭', '只', 48.00, 38.00, 30, 1),
(2, 'BBY001', '北京烤鸭', '只', 55.00, 42.00, 25, 1),
(3, 'RG001', '肉鸽', '只', 45.00, 35.00, 60, 1),
(3, 'RGY001', '乳鸽', '只', 38.00, 28.00, 40, 1),
(4, 'DBE001', '大白鹅', '只', 128.00, 98.00, 20, 1),
(4, 'HE001', '灰鹅', '只', 118.00, 88.00, 15, 1);

-- 初始化商品库存
INSERT INTO `inventory` (`product_id`, `quantity`, `total_weight`, `min_quantity`) 
SELECT `id`, 100, 0, `min_stock` FROM `product`;

-- 插入示例供应商
INSERT INTO `supplier` (`name`, `contact_name`, `phone`, `address`, `supply_products`, `rating`, `status`) VALUES
('张记养鸡场', '张老板', '13900139001', '广东省清远市', '土鸡、三黄鸡', 5, 1),
('李氏禽业', '李经理', '13900139002', '广东省惠州市', '乌鸡、白羽鸡', 4, 1),
('王家鸭场', '王老板', '13900139003', '广东省佛山市', '麻鸭、番鸭', 5, 1),
('陈记鸽舍', '陈老板', '13900139004', '广东省广州市', '肉鸽、乳鸽', 4, 1),
('刘氏鹅业', '刘经理', '13900139005', '广东省汕头市', '大白鹅、灰鹅', 5, 1);

-- 插入示例客户
INSERT INTO `customer` (`name`, `type`, `level`, `contact_name`, `phone`, `address`, `credit_limit`, `status`) VALUES
('张记酒楼', 'restaurant', 'vip', '张老板', '13800001001', '广州市天河区天河路100号', 10000.00, 1),
('李氏餐馆', 'restaurant', 'normal', '李经理', '13800001002', '广州市越秀区中山路200号', 5000.00, 1),
('王府酒家', 'restaurant', 'svip', '王总', '13800001003', '广州市海珠区江南大道300号', 20000.00, 1),
('赵家菜馆', 'restaurant', 'normal', '赵老板', '13800001004', '广州市白云区白云大道400号', 3000.00, 1),
('福满楼', 'restaurant', 'vip', '钱经理', '13800001005', '广州市番禺区市桥路500号', 15000.00, 1),
('陈记大排档', 'retail', 'normal', '陈老板', '13800001006', '广州市荔湾区荔湾路600号', 2000.00, 1),
('孙氏批发', 'wholesale', 'vip', '孙经理', '13800001007', '广州市增城区增城大道700号', 30000.00, 1);

-- =============================================
-- 9. 外键约束 (可选，根据需要启用)
-- =============================================

-- 如需启用外键约束，取消以下注释

-- ALTER TABLE `product` ADD CONSTRAINT `fk_product_category` 
--   FOREIGN KEY (`category_id`) REFERENCES `category`(`id`) ON DELETE SET NULL;

-- ALTER TABLE `inventory` ADD CONSTRAINT `fk_inventory_product` 
--   FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE;

-- ALTER TABLE `inventory_inbound` ADD CONSTRAINT `fk_inbound_product` 
--   FOREIGN KEY (`product_id`) REFERENCES `product`(`id`);

-- ALTER TABLE `inventory_outbound` ADD CONSTRAINT `fk_outbound_product` 
--   FOREIGN KEY (`product_id`) REFERENCES `product`(`id`);

-- ALTER TABLE `order_item` ADD CONSTRAINT `fk_order_item_order` 
--   FOREIGN KEY (`order_id`) REFERENCES `order`(`id`) ON DELETE CASCADE;

-- ALTER TABLE `order_item` ADD CONSTRAINT `fk_order_item_product` 
--   FOREIGN KEY (`product_id`) REFERENCES `product`(`id`);

-- ALTER TABLE `order_payment` ADD CONSTRAINT `fk_payment_order` 
--   FOREIGN KEY (`order_id`) REFERENCES `order`(`id`) ON DELETE CASCADE;

-- ALTER TABLE `purchase_order` ADD CONSTRAINT `fk_purchase_supplier` 
--   FOREIGN KEY (`supplier_id`) REFERENCES `supplier`(`id`);

-- ALTER TABLE `purchase_order_item` ADD CONSTRAINT `fk_purchase_item_order` 
--   FOREIGN KEY (`purchase_id`) REFERENCES `purchase_order`(`id`) ON DELETE CASCADE;

-- ALTER TABLE `customer_credit_log` ADD CONSTRAINT `fk_credit_log_customer` 
--   FOREIGN KEY (`customer_id`) REFERENCES `customer`(`id`);

-- =============================================
-- 完成
-- =============================================

SELECT '数据库初始化完成!' AS message;
SELECT CONCAT('- 分类: ', COUNT(*), ' 条') AS info FROM `category`
UNION ALL
SELECT CONCAT('- 商品: ', COUNT(*), ' 条') FROM `product`
UNION ALL
SELECT CONCAT('- 供应商: ', COUNT(*), ' 条') FROM `supplier`
UNION ALL
SELECT CONCAT('- 客户: ', COUNT(*), ' 条') FROM `customer`
UNION ALL
SELECT CONCAT('- 库存: ', COUNT(*), ' 条') FROM `inventory`;

