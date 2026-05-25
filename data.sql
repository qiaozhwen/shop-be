-- ========== 门店 ==========
INSERT INTO stores (name, address, phone, owner_name, status) VALUES
    ('上海下南店', '上海市下南路农贸市场', '13800001001', '张老板', 'OPEN'),
    ('上海陈春店', '上海市陈春路菜市场',   '13800001002', '张老板', 'OPEN')
ON CONFLICT DO NOTHING;

-- ========== 员工 ==========
INSERT INTO staff (store_id, name, phone, role, password, status) VALUES
    (1, '张老板', '13800001001', 'ADMIN',   '$2a$10$dummyhash', 'ACTIVE'),
    (1, '刘师傅', '13800002001', 'BUTCHER', '$2a$10$dummyhash', 'ACTIVE'),
    (1, '陈小妹', '13800002002', 'SELLER',  '$2a$10$dummyhash', 'ACTIVE'),
    (2, '李店长', '13800001002', 'MANAGER', '$2a$10$dummyhash', 'ACTIVE'),
    (2, '赵师傅', '13800002003', 'BUTCHER', '$2a$10$dummyhash', 'ACTIVE'),
    (2, '周小弟', '13800002004', 'SELLER',  '$2a$10$dummyhash', 'ACTIVE')
ON CONFLICT DO NOTHING;

-- ========== 禽类分类 ==========
INSERT INTO categories (name, sort_order) VALUES
    ('鸡', 1),
    ('鸭', 2),
    ('鹅', 3),
    ('鸽', 4),
    ('其他', 5)
ON CONFLICT DO NOTHING;

-- ========== 商品 ==========
INSERT INTO products (category_id, name, unit, default_price, description) VALUES
    (1, '三黄鸡',  '斤', 18.00, '本地散养三黄鸡，肉质鲜嫩'),
    (1, '老母鸡',  '斤', 22.00, '散养两年以上老母鸡，炖汤首选'),
    (1, '乌鸡',   '斤', 28.00, '正宗乌骨鸡，滋补养生'),
    (1, '土公鸡',  '斤', 20.00, '本地土公鸡，红烧爆炒'),
    (2, '麻鸭',   '斤', 16.00, '本地麻鸭，皮薄肉嫩'),
    (2, '番鸭',   '斤', 19.00, '正番鸭，肉质紧实'),
    (2, '老鸭',   '斤', 18.00, '散养一年以上老鸭，煲汤佳品'),
    (3, '狮头鹅',  '斤', 25.00, '潮汕狮头鹅，个大肉厚'),
    (3, '白鹅',   '斤', 22.00, '本地白鹅，肉质细嫩'),
    (4, '乳鸽',   '只', 25.00, '嫩乳鸽，适合煲汤红烧'),
    (4, '老鸽',   '只', 30.00, '老鸽子，炖汤滋补')
ON CONFLICT DO NOTHING;

-- ========== 门店定价（两店略有差异） ==========
INSERT INTO store_products (store_id, product_id, price) VALUES
    -- 下南店（全品类）
    (1, 1, 18.00), (1, 2, 22.00), (1, 3, 28.00), (1, 4, 20.00),
    (1, 5, 16.00), (1, 6, 19.00), (1, 7, 18.00),
    (1, 8, 25.00), (1, 9, 22.00), (1, 10, 25.00), (1, 11, 30.00),
    -- 陈春店（主打鸡鸭，鹅和鸽少量）
    (2, 1, 18.50), (2, 2, 23.00), (2, 3, 29.00), (2, 4, 20.50),
    (2, 5, 16.50), (2, 6, 19.50), (2, 7, 18.50),
    (2, 10, 26.00), (2, 11, 32.00)
ON CONFLICT DO NOTHING;

-- ========== 门店库存（今日到货） ==========
INSERT INTO store_inventory (store_id, product_id, stock_date, incoming_qty, sold_qty, remaining_qty) VALUES
    -- 下南店
    (1, 1,  CURRENT_DATE, 30, 12, 18),
    (1, 2,  CURRENT_DATE, 15,  5, 10),
    (1, 3,  CURRENT_DATE, 10,  3,  7),
    (1, 4,  CURRENT_DATE, 12,  4,  8),
    (1, 5,  CURRENT_DATE, 20,  8, 12),
    (1, 6,  CURRENT_DATE,  8,  2,  6),
    (1, 8,  CURRENT_DATE,  6,  1,  5),
    (1, 10, CURRENT_DATE, 20,  6, 14),
    -- 陈春店
    (2, 1,  CURRENT_DATE, 25, 10, 15),
    (2, 2,  CURRENT_DATE, 10,  4,  6),
    (2, 4,  CURRENT_DATE, 10,  3,  7),
    (2, 5,  CURRENT_DATE, 15,  5, 10),
    (2, 6,  CURRENT_DATE,  6,  2,  4),
    (2, 10, CURRENT_DATE, 15,  4, 11)
ON CONFLICT DO NOTHING;

-- ========== 客户 ==========
INSERT INTO customers (name, phone, store_id, points, remark) VALUES
    ('周大姐', '15900001001', 1, 120, '老客户，每周来两次'),
    ('吴阿姨', '15900001002', 1,  80, '喜欢买老母鸡炖汤'),
    ('郑老板', '15900001003', 1, 200, '餐馆老板，量大'),
    ('冯叔叔', '15900001004', 2,  50, NULL),
    ('杨小姐', '15900001005', 2,  30, '偏好乳鸽'),
    ('孙阿婆', '15900001006', 2,  90, '每天早上来，买鸡为主')
ON CONFLICT DO NOTHING;

-- ========== 订单 ==========
INSERT INTO orders (order_no, store_id, staff_id, customer_id, total_amount, payment_method, status) VALUES
    -- 下南店订单
    ('ORD20260413001', 1, 3, 1,  68.00, 'WECHAT',  'COMPLETED'),
    ('ORD20260413002', 1, 3, 3, 406.00, 'ALIPAY',  'COMPLETED'),
    ('ORD20260413003', 1, 3, NULL, 61.00, 'CASH',   'COMPLETED'),
    -- 陈春店订单
    ('ORD20260413004', 2, 6, 4,  97.50, 'WECHAT',  'COMPLETED'),
    ('ORD20260413005', 2, 6, 6, 111.00, 'WECHAT',  'COMPLETED'),
    ('ORD20260413006', 2, 6, 5,  52.00, 'ALIPAY',  'PROCESSING')
ON CONFLICT DO NOTHING;

-- ========== 订单明细 ==========
INSERT INTO order_items (order_id, product_id, quantity, weight, unit_price, amount, need_slaughter, slaughter_fee) VALUES
    -- 订单1: 周大姐买三黄鸡1只 + 宰杀费
    (1, 1, 1, 3.50, 18.00, 63.00, TRUE, 5.00),
    -- 订单2: 郑老板买老母鸡4只（餐馆备货）
    (2, 2, 4, 18.00, 22.00, 396.00, TRUE, 10.00),
    -- 订单3: 散客买麻鸭1只
    (3, 5, 1, 3.50, 16.00, 56.00, TRUE, 5.00),
    -- 订单4: 冯叔叔在陈春店买三黄鸡1只
    (4, 1, 1, 5.00, 18.50, 92.50, TRUE, 5.00),
    -- 订单5: 孙阿婆买三黄鸡1只 + 老母鸡1只
    (5, 1, 1, 3.20, 18.50, 59.20, TRUE, 5.00),
    (5, 2, 1, 2.00, 23.00, 46.00, TRUE, 0.80),
    -- 订单6: 杨小姐买乳鸽2只（处理中）
    (6, 10, 2, NULL, 26.00, 52.00, TRUE, 0.00)
ON CONFLICT DO NOTHING;

-- ========== 宰杀记录 ==========
INSERT INTO slaughter_records (order_item_id, live_weight, cleaned_weight, slaughter_type, staff_id, completed_at) VALUES
    (1, 3.80, 3.50, 'FULL', 2, CURRENT_TIMESTAMP - INTERVAL '3 hours'),
    (2, 19.50, 18.00, 'FULL', 2, CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    (3, 3.80, 3.50, 'FULL', 2, CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (4, 5.30, 5.00, 'FULL', 5, CURRENT_TIMESTAMP - INTERVAL '2 hours'),
    (5, 3.50, 3.20, 'FULL', 5, CURRENT_TIMESTAMP - INTERVAL '1 hour'),
    (6, 2.20, 2.00, 'FULL', 5, CURRENT_TIMESTAMP - INTERVAL '1 hour')
ON CONFLICT DO NOTHING;

-- ========== 每日对账（昨日） ==========
INSERT INTO daily_settlements (store_id, settle_date, total_orders, total_amount, wechat_amount, alipay_amount, cash_amount, settled_by) VALUES
    (1, CURRENT_DATE - 1, 32, 2860.00, 1780.00, 680.00, 400.00, 1),
    (2, CURRENT_DATE - 1, 22, 1950.00, 1360.00, 350.00, 240.00, 4)
ON CONFLICT DO NOTHING;
