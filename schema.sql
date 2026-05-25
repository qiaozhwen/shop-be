-- 门店表
CREATE TABLE IF NOT EXISTS stores (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     VARCHAR(255),
    phone       VARCHAR(20),
    owner_name  VARCHAR(50),
    status      VARCHAR(20) DEFAULT 'OPEN',
    opening_time TIME DEFAULT '06:00',
    closing_time TIME DEFAULT '18:00',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 员工表
CREATE TABLE IF NOT EXISTS staff (
    id          BIGSERIAL PRIMARY KEY,
    store_id    BIGINT NOT NULL REFERENCES stores(id),
    name        VARCHAR(50) NOT NULL,
    phone       VARCHAR(20),
    role        VARCHAR(20) NOT NULL DEFAULT 'SELLER',
    password    VARCHAR(255) NOT NULL,
    status      VARCHAR(20) DEFAULT 'ACTIVE',
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 禽类分类表
CREATE TABLE IF NOT EXISTS categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    sort_order  INTEGER DEFAULT 0
);

-- 商品表（禽类产品）
CREATE TABLE IF NOT EXISTS products (
    id              BIGSERIAL PRIMARY KEY,
    category_id     BIGINT REFERENCES categories(id),
    name            VARCHAR(100) NOT NULL,
    unit            VARCHAR(10) NOT NULL DEFAULT '斤',
    default_price   DECIMAL(10, 2) NOT NULL,
    description     VARCHAR(255),
    is_active       BOOLEAN DEFAULT TRUE
);

-- 门店商品价格表（不同门店可以有不同定价）
CREATE TABLE IF NOT EXISTS store_products (
    id          BIGSERIAL PRIMARY KEY,
    store_id    BIGINT NOT NULL REFERENCES stores(id),
    product_id  BIGINT NOT NULL REFERENCES products(id),
    price       DECIMAL(10, 2) NOT NULL,
    UNIQUE(store_id, product_id)
);

-- 门店库存表（每日活禽到货数量）
CREATE TABLE IF NOT EXISTS store_inventory (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT NOT NULL REFERENCES stores(id),
    product_id      BIGINT NOT NULL REFERENCES products(id),
    stock_date      DATE NOT NULL DEFAULT CURRENT_DATE,
    incoming_qty    INTEGER NOT NULL DEFAULT 0,
    sold_qty        INTEGER NOT NULL DEFAULT 0,
    remaining_qty   INTEGER NOT NULL DEFAULT 0,
    UNIQUE(store_id, product_id, stock_date)
);

-- 客户表
CREATE TABLE IF NOT EXISTS customers (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50),
    phone       VARCHAR(20) UNIQUE,
    store_id    BIGINT REFERENCES stores(id),
    points      INTEGER DEFAULT 0,
    remark      VARCHAR(255),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单表
CREATE TABLE IF NOT EXISTS orders (
    id              BIGSERIAL PRIMARY KEY,
    order_no        VARCHAR(30) NOT NULL UNIQUE,
    store_id        BIGINT NOT NULL REFERENCES stores(id),
    staff_id        BIGINT REFERENCES staff(id),
    customer_id     BIGINT REFERENCES customers(id),
    total_amount    DECIMAL(10, 2) NOT NULL,
    payment_method  VARCHAR(20) DEFAULT 'WECHAT',
    status          VARCHAR(20) DEFAULT 'PENDING',
    remark          VARCHAR(255),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 订单明细表
CREATE TABLE IF NOT EXISTS order_items (
    id              BIGSERIAL PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders(id),
    product_id      BIGINT NOT NULL REFERENCES products(id),
    quantity        INTEGER NOT NULL DEFAULT 1,
    weight          DECIMAL(8, 2),
    unit_price      DECIMAL(10, 2) NOT NULL,
    amount          DECIMAL(10, 2) NOT NULL,
    need_slaughter  BOOLEAN DEFAULT TRUE,
    slaughter_fee   DECIMAL(10, 2) DEFAULT 0
);

-- 宰杀记录表
CREATE TABLE IF NOT EXISTS slaughter_records (
    id              BIGSERIAL PRIMARY KEY,
    order_item_id   BIGINT NOT NULL REFERENCES order_items(id),
    live_weight     DECIMAL(8, 2),
    cleaned_weight  DECIMAL(8, 2),
    slaughter_type  VARCHAR(20) DEFAULT 'FULL',
    staff_id        BIGINT REFERENCES staff(id),
    completed_at    TIMESTAMP
);

-- 每日对账表
CREATE TABLE IF NOT EXISTS daily_settlements (
    id              BIGSERIAL PRIMARY KEY,
    store_id        BIGINT NOT NULL REFERENCES stores(id),
    settle_date     DATE NOT NULL,
    total_orders    INTEGER DEFAULT 0,
    total_amount    DECIMAL(12, 2) DEFAULT 0,
    wechat_amount   DECIMAL(12, 2) DEFAULT 0,
    alipay_amount   DECIMAL(12, 2) DEFAULT 0,
    cash_amount     DECIMAL(12, 2) DEFAULT 0,
    settled_by      BIGINT REFERENCES staff(id),
    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(store_id, settle_date)
);

-- ── Auth: staff 改造 ──
ALTER TABLE staff ADD COLUMN IF NOT EXISTS phone VARCHAR(20);
ALTER TABLE staff ADD COLUMN IF NOT EXISTS nickname VARCHAR(64);
ALTER TABLE staff ADD COLUMN IF NOT EXISTS avatar_url VARCHAR(255);
ALTER TABLE staff ADD COLUMN IF NOT EXISTS failed_login_count INT NOT NULL DEFAULT 0;
ALTER TABLE staff ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP NULL;
ALTER TABLE staff ADD COLUMN IF NOT EXISTS last_login_at TIMESTAMP NULL;
ALTER TABLE staff ALTER COLUMN password DROP NOT NULL;
DO $$ BEGIN
  IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'staff_phone_unique') THEN
    ALTER TABLE staff ADD CONSTRAINT staff_phone_unique UNIQUE (phone);
  END IF;
END $$;

-- ── Auth: 社交账号绑定 ──
CREATE TABLE IF NOT EXISTS staff_social_accounts (
  id          BIGSERIAL PRIMARY KEY,
  staff_id    BIGINT REFERENCES staff(id),
  provider    VARCHAR(32) NOT NULL,
  open_id     VARCHAR(128) NOT NULL,
  union_id    VARCHAR(128),
  raw_profile JSONB,
  created_at  TIMESTAMP NOT NULL DEFAULT now(),
  updated_at  TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (provider, open_id)
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_ssa_staff_provider
  ON staff_social_accounts(staff_id, provider) WHERE staff_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ssa_staff ON staff_social_accounts(staff_id);

-- ── Auth: 短信验证码 ──
CREATE TABLE IF NOT EXISTS sms_verification_codes (
  id          BIGSERIAL PRIMARY KEY,
  phone       VARCHAR(20) NOT NULL,
  purpose     VARCHAR(32) NOT NULL,
  code_hash   VARCHAR(64) NOT NULL,
  expires_at  TIMESTAMP NOT NULL,
  consumed_at TIMESTAMP NULL,
  attempts    INT NOT NULL DEFAULT 0,
  created_at  TIMESTAMP NOT NULL DEFAULT now(),
  ip          VARCHAR(64)
);
CREATE INDEX IF NOT EXISTS idx_svc_phone_purpose
  ON sms_verification_codes(phone, purpose, created_at DESC);

-- ── Auth: refresh token ──
CREATE TABLE IF NOT EXISTS refresh_tokens (
  id           BIGSERIAL PRIMARY KEY,
  token_hash   VARCHAR(64) NOT NULL UNIQUE,
  subject_type VARCHAR(16) NOT NULL,
  subject_id   BIGINT NOT NULL,
  device_info  VARCHAR(255),
  expires_at   TIMESTAMP NOT NULL,
  revoked_at   TIMESTAMP NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX IF NOT EXISTS idx_rt_subject ON refresh_tokens(subject_type, subject_id);

-- ── Auth: 登录审计 ──
CREATE TABLE IF NOT EXISTS login_audit_logs (
  id            BIGSERIAL PRIMARY KEY,
  staff_id      BIGINT,
  channel       VARCHAR(32) NOT NULL,
  account_input VARCHAR(128),
  result        VARCHAR(16) NOT NULL,
  fail_reason   VARCHAR(64),
  ip            VARCHAR(64),
  user_agent    VARCHAR(255),
  created_at    TIMESTAMP NOT NULL DEFAULT now()
);
