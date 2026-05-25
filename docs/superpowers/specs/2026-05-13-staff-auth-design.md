# B 端认证模块设计：手机号 + 密码 + 微信/支付宝扫码

- 状态：Draft
- 日期：2026-05-13
- 范围：`qz-shop-be`（`/Users/qiaozhen/app/shop/shop-be`）
- 替代：`2026-05-12-auth-sso-and-password-design.md`（已废弃；C 端推迟到未来 spec）

## 1. 背景与目标

当前后端只有 Store CRUD，所有 `/api/**` 接口匿名可访问，没有任何认证逻辑。
本期目标只覆盖 **B 端 Web 后台管理系统** 的认证：

- 三种登录渠道：**手机号 + 密码**、**微信扫码**、**支付宝扫码**。
- 一种辅助渠道：**手机号 + 短信验证码**（用于扫码首登绑手机号、找回密码、首次设密码、备用登录）。
- **手机号是全局唯一身份**，跨渠道串联同一个 staff。
- 自助注册：任何人完成扫码 + 手机号验证后自动建 staff，默认最小角色 `STAFF_DEFAULT`，等 admin 升权。
- 现实约束：微信网站应用扫码登录默认不返回手机号，支付宝同样不稳定。所以 SSO 只用于"验证社交账号身份"，手机号通过 SMS 二次验证独立确认。

C 端（小程序 SSO 等）不在本期范围。

## 2. 架构总览（方案 A：单 auth 模块按角色分包）

```
com.qzshop.shopbe.auth
 ├─ staff/         B 端业务编排
 │   ├─ StaffAuthController        /api/admin/auth/**
 │   ├─ StaffAuthService           密码 / 短信 / 扫码统一编排
 │   ├─ StaffRegistrationService   自助建号
 │   └─ LoginAttemptService        密码失败计数 + 15 分钟锁
 ├─ sso/           扫码登录抽象 + 实现
 │   ├─ SsoScanProvider (interface)
 │   ├─ WechatScanProvider         微信网站应用 OAuth 2.0
 │   └─ AlipayScanProvider         支付宝第三方应用授权登录
 ├─ sms/           短信验证码
 │   ├─ SmsProvider (interface)
 │   ├─ MockSmsProvider            dev 返 123456，生产打日志
 │   └─ SmsVerificationService     发送 / 校验 / 节流 / 计数
 ├─ token/         JWT + refresh token
 │   ├─ JwtService
 │   ├─ RefreshToken (Entity/Repo)
 │   └─ TokenService               issue / refresh / revoke / 临时 BIND_PENDING token
 ├─ security/      Spring Security 配置
 │   ├─ SecurityConfig             3 条 SecurityFilterChain
 │   ├─ JwtAuthenticationFilter
 │   └─ StaffPrincipal
 └─ password/      BCrypt 封装
```

JWT、密码哈希、refresh token 复用一套；SSO/SMS provider 抽象，第二期可平替阿里云短信、企业微信扫码等。

## 3. 数据模型

### 3.1 `staff` 表改动
```sql
ALTER TABLE staff
  ADD COLUMN phone               VARCHAR(20) NOT NULL UNIQUE,
  ALTER COLUMN password DROP NOT NULL,                            -- 纯扫码用户没密码
  ADD COLUMN nickname            VARCHAR(64),
  ADD COLUMN avatar_url          VARCHAR(255),
  ADD COLUMN status              VARCHAR(16) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/DISABLED
  ADD COLUMN failed_login_count  INT NOT NULL DEFAULT 0,
  ADD COLUMN locked_until        TIMESTAMP NULL,
  ADD COLUMN last_login_at       TIMESTAMP NULL;

-- 已存在的 password 字段重命名为 password_hash 以表意更清晰（可选）
-- 已存在的 username 字段保留但不再用于登录；可作为展示用途
```
约束说明：
- `phone UNIQUE NOT NULL`：所有 staff 必须有手机号；自助注册时由短信验证保证可信。
- `password` 改为可空：扫码自助注册的 staff 没有初始密码，可在登录后通过"设密码"流程补一个。
- `failed_login_count / locked_until` **只对密码登录路径生效**；扫码登录和短信验证码登录不计入锁定（避免他人故意触发锁定门禁）。

### 3.2 新增 `staff_social_accounts`
```sql
CREATE TABLE staff_social_accounts (
  id           BIGSERIAL PRIMARY KEY,
  staff_id     BIGINT NOT NULL REFERENCES staff(id),
  provider     VARCHAR(32) NOT NULL,             -- 'WECHAT' / 'ALIPAY'
  open_id      VARCHAR(128) NOT NULL,
  union_id     VARCHAR(128),                     -- 微信 unionid，可空
  raw_profile  JSONB,                            -- 平台返回的资料快照（昵称、头像）
  created_at   TIMESTAMP NOT NULL DEFAULT now(),
  updated_at   TIMESTAMP NOT NULL DEFAULT now(),
  UNIQUE (provider, open_id),                    -- 同一外部账号只能绑一个 staff
  UNIQUE (staff_id, provider)                    -- 一个 staff 在每个 provider 下最多一个绑定
);
CREATE INDEX idx_ssa_staff ON staff_social_accounts(staff_id);
```
两个 UNIQUE 共同实现"一人一号"约束。

### 3.3 新增 `sms_verification_codes`
```sql
CREATE TABLE sms_verification_codes (
  id           BIGSERIAL PRIMARY KEY,
  phone        VARCHAR(20) NOT NULL,
  purpose      VARCHAR(32) NOT NULL,            -- BIND_PHONE / SMS_LOGIN / RESET_PASSWORD / SET_PASSWORD
  code_hash    VARCHAR(64) NOT NULL,            -- SHA-256(code)，不存原文
  expires_at   TIMESTAMP NOT NULL,              -- 默认 5 分钟
  consumed_at  TIMESTAMP NULL,
  attempts     INT NOT NULL DEFAULT 0,          -- 校验次数，>=5 视为作废
  created_at   TIMESTAMP NOT NULL DEFAULT now(),
  ip           VARCHAR(64)
);
CREATE INDEX idx_svc_phone_purpose ON sms_verification_codes(phone, purpose, created_at DESC);
```
节流规则（Service 层实现）：
- 同手机号同 purpose 60 秒内只能发一次。
- 同手机号 24 小时内最多发 10 次。
- 校验时取该 (phone, purpose) 最新一条未消费、未过期的记录；不匹配 attempts++；attempts >= 5 整体作废。

### 3.4 新增 `refresh_tokens`
```sql
CREATE TABLE refresh_tokens (
  id           BIGSERIAL PRIMARY KEY,
  token_hash   VARCHAR(64) NOT NULL UNIQUE,     -- SHA-256(token) hex
  subject_type VARCHAR(16) NOT NULL,            -- 第一期固定 'STAFF'
  subject_id   BIGINT NOT NULL,
  device_info  VARCHAR(255),
  expires_at   TIMESTAMP NOT NULL,
  revoked_at   TIMESTAMP NULL,
  created_at   TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_rt_subject ON refresh_tokens(subject_type, subject_id);
```
保留 `subject_type` 字段是为了未来扩 C 端时不用迁表；第一期写死 `STAFF`。

### 3.5 新增 `login_audit_logs`
```sql
CREATE TABLE login_audit_logs (
  id            BIGSERIAL PRIMARY KEY,
  staff_id      BIGINT,                          -- 失败时可能为空
  channel       VARCHAR(32) NOT NULL,            -- PASSWORD / SMS / WECHAT / ALIPAY / REFRESH / LOGOUT
  account_input VARCHAR(128),                    -- 输入的手机号或 openid 摘要
  result        VARCHAR(16) NOT NULL,            -- SUCCESS / FAIL / LOCKED / BIND_PENDING
  fail_reason   VARCHAR(64),
  ip            VARCHAR(64),
  user_agent    VARCHAR(255),
  created_at    TIMESTAMP NOT NULL DEFAULT now()
);
```

不引入 Redis：失败计数走 `staff` 表字段，验证码、refresh、审计都走 PG。后期热点上来再迁。

## 4. API 设计

所有路径前缀 `/api/admin/**`，由 adminChain 拦截；登录类公开端点由 publicChain 放行。

### 4.1 端点列表

| Method | Path | 说明 | 鉴权 |
|---|---|---|---|
| POST | `/api/admin/auth/login` | 手机号 + 密码登录；锁定时返回 423 | 否 |
| POST | `/api/admin/auth/sms/send` | 发短信验证码；body `{phone, purpose}` | 否（节流由后端控） |
| POST | `/api/admin/auth/sms/login` | 手机号 + 短信验证码登录（要求 staff 已存在） | 否 |
| POST | `/api/admin/auth/sso/wechat/start` | 返回 `{qrUrl, state}`，state 后端缓存 5 分钟 | 否 |
| POST | `/api/admin/auth/sso/wechat/exchange` | body `{code, state}`，返回正式 token 或 `BIND_PENDING` | 否 |
| POST | `/api/admin/auth/sso/alipay/start` | 同上 | 否 |
| POST | `/api/admin/auth/sso/alipay/exchange` | 同上 | 否 |
| POST | `/api/admin/auth/bind-phone` | 携带 `BIND_PENDING` token；body `{phone, smsCode}`；登录或自助注册 | BIND_PENDING token |
| POST | `/api/admin/auth/bind/{provider}` | 已登录用户额外绑微信/支付宝；body `{code}` | STAFF token |
| DELETE | `/api/admin/auth/bind/{provider}` | 解绑（保留至少一种登录方式） | STAFF token |
| POST | `/api/admin/auth/set-password` | 已登录设/改密码 | STAFF token |
| POST | `/api/admin/auth/reset-password` | 忘记密码：手机号 + 短信验证码 + 新密码 | 否 |
| GET  | `/api/admin/me` | 当前 staff 资料 + 已绑渠道 + 是否有密码 | STAFF token |
| POST | `/api/admin/auth/logout` | 撤销当前 refresh | STAFF token |
| POST | `/api/auth/refresh` | 刷新 token，旋转策略 | 否（凭 refresh） |

### 4.2 统一返回结构

**正式登录成功**：
```json
{
  "tokenType": "Bearer",
  "accessToken": "<jwt typ=STAFF>",
  "accessTokenExpiresIn": 1800,
  "refreshToken": "<opaque-random>",
  "refreshTokenExpiresIn": 1209600,
  "subject": {
    "type": "STAFF",
    "id": 123,
    "phone": "138****1234",
    "nickname": "张三",
    "roles": ["STAFF_DEFAULT"]
  }
}
```

**扫码后未绑手机号（待自助注册或绑定到既有 staff）**：
```json
{
  "status": "BIND_PENDING",
  "bindToken": "<jwt typ=BIND_PENDING, sub=staff_social_accounts.id, exp=now+5min>",
  "bindTokenExpiresIn": 300,
  "provider": "WECHAT",
  "profile": { "nickname": "张三", "avatarUrl": "..." }
}
```

**`/api/admin/auth/sms/send` 成功**：
```json
{ "phone": "138****1234", "expiresIn": 300, "resendAfter": 60 }
```

**错误响应**（沿用 Store 模块风格）：
| 场景 | HTTP | Body |
|---|---|---|
| 账号或密码错 | 401 | `{"message":"Invalid credentials"}` |
| 账号被禁用 | 403 | `{"message":"Account disabled"}` |
| 账号锁定中 | 423 | `{"message":"Account locked","retryAfterSeconds":N}` + `Retry-After` 头 |
| 短信验证码错/过期 | 400 | `{"message":"Invalid or expired SMS code"}` |
| 短信发送过频 | 429 | `{"message":"Too many requests","retryAfterSeconds":N}` |
| 缺少/非法 token | 401 | `{"message":"Authentication required"}` |
| token 类型不匹配（如用 BIND_PENDING 调业务接口） | 403 | `{"message":"Forbidden"}` |
| refresh 无效/过期/撤销 | 401 | `{"message":"Refresh token expired or revoked"}` |
| 校验失败（@Valid） | 400 | `{"message":"<field> <reason>"}` |
| SSO 平台返回错误 | 502 | `{"message":"SSO provider error","provider":"..."}` |
| 绑定冲突（该微信已绑别人；该 staff 已绑该 provider） | 409 | `{"message":"Already bound"}` |
| 解绑导致无登录方式 | 409 | `{"message":"Cannot unbind: at least one login method required"}` |

## 5. 登录与绑定流程

### 5.1 手机号 + 密码登录
```
POST /api/admin/auth/login {phone, password}
  ↓
1. SELECT * FROM staff WHERE phone = ?；找不到也跑一次 BCrypt 假比对（防时间侧信道）
2. status='DISABLED' → 403
3. locked_until > now() → 423 + Retry-After
4. password_hash IS NULL → 401（该账号未设密码，提示走 SMS 登录）
5. BCrypt.matches?
     ├─ 是：failed_login_count=0、locked_until=NULL、last_login_at=now()、写 SUCCESS、issueTokens
     └─ 否：failed_login_count += 1
            if count >= 5: locked_until=now()+15min, count=0, 写 LOCKED → 423
            else 写 FAIL → 401
```

### 5.2 短信验证码发送（公共能力）
```
POST /api/admin/auth/sms/send {phone, purpose}
  ↓
1. 校验 phone 格式
2. 节流：同 (phone, purpose) 60s 内已发过 → 429
        同 phone 24h 内 >= 10 次 → 429
3. 生成 6 位数字 code，code_hash = SHA256(code)
4. INSERT sms_verification_codes (..., expires_at = now()+5min)
5. SmsProvider.send(phone, code, purpose)
6. 返回 {phone(脱敏), expiresIn, resendAfter}
```

`SmsProvider` 抽象：
```java
interface SmsProvider {
    void send(String phone, String code, SmsPurpose purpose);
}
```
- `MockSmsProvider`（默认）：开发/测试环境永远发 `123456`（不写库，纯打印）；生产环境通过日志输出。
- 后期可加 `AliyunSmsProvider`、`TencentSmsProvider`，配置切换。

### 5.3 短信验证码登录
```
POST /api/admin/auth/sms/login {phone, code}
  ↓
1. consumeSmsCode(phone, SMS_LOGIN, code) → 不通过 → 400
2. SELECT staff WHERE phone = ?
3. 不存在 → 404 "Staff not found"（注意：自助注册必须走扫码路径，纯手机号短信不开放注册）
4. status='DISABLED' → 403
5. 写 SUCCESS、issueTokens
```
注意短信登录不会触发 `failed_login_count`（验证码本身已有 attempts 限制）。

### 5.4 微信扫码登录
```
[前端]                         [shop-be]                       [微信开放平台]
  | start ----------------> |                                       |
  |                         | 生成 state（random）+ 缓存 5 分钟     |
  |                         | 拼装 qrUrl =                          |
  |                         |   open.weixin.qq.com/connect/qrconnect|
  |                         |   ?appid&redirect_uri&state...        |
  | <-- {qrUrl, state} ---- |                                       |
  | 跳转微信扫码           |                                       |
  | 用户扫码 → 微信回调 redirect_uri?code=...&state=...             |
  | exchange ---{code,state}-> |                                    |
  |                         | 校验 state（一次性）                  |
  |                         | access_token, openid, unionid <------ |
  |                         |                                       |
  |                         | findOrCreate staff_social_accounts:   |
  |                         |   ├─ 命中 + 已绑 staff → issueTokens(STAFF, staff_id)
  |                         |   └─ 未绑：临时存储 social 记录（staff_id NULL，限 5min）
  |                         |             返回 BIND_PENDING + bindToken（社交记录 id 在 claim）
  | <-- token / BIND_PENDING|                                       |
```

`SsoScanProvider` 抽象：
```java
interface SsoScanProvider {
    String name();                          // "WECHAT" / "ALIPAY"
    String buildAuthorizeUrl(String state, String redirectUri);
    SsoUserInfo exchange(String code);      // 调平台拿 openid 等
}
record SsoUserInfo(String openId, String unionId, JsonNode rawProfile) {}
```

### 5.5 BIND_PENDING → 绑手机号 / 自助注册
```
POST /api/admin/auth/bind-phone {phone, smsCode}    (Authorization: Bearer <bindToken>)
  ↓
1. 解析 bindToken：typ 必须是 BIND_PENDING；拿到挂起的 socialAccountId
2. consumeSmsCode(phone, BIND_PHONE, smsCode) → 不通过 → 400
3. 查找 staff WHERE phone = phone:
     ├─ 找到（既有 staff 第一次扫码）：
     │     检查 staff_social_accounts(staff_id=found, provider) 是否已存在 → 已存在 → 409
     │     UPDATE socialAccount SET staff_id = found.id
     │     写 audit channel=<provider> SUCCESS
     │     issueTokens(STAFF, found.id)
     └─ 没找到（自助注册）：
           INSERT staff(phone, role=STAFF_DEFAULT, status=ACTIVE, password_hash=NULL)
           UPDATE socialAccount SET staff_id = newStaff.id
           写 audit channel=<provider> SUCCESS, fail_reason=null, account_input=phone
           issueTokens(STAFF, newStaff.id)
```
并发：`staff.phone UNIQUE` + `staff_social_accounts (staff_id, provider) UNIQUE` 兜底，冲突回查既有记录。

### 5.6 已登录绑定额外社交账号
```
POST /api/admin/auth/bind/wechat {code}   (STAFF token)
  ↓
1. provider.exchange(code) → openId/unionId
2. SELECT staff_social_accounts WHERE provider=? AND open_id=?
     ├─ 不存在 → INSERT(staff_id=current, ...)
     └─ 存在但 staff_id=current → 200（幂等）
     └─ 存在但 staff_id!=current → 409（已被别人绑）
3. (staff_id, provider) UNIQUE 兜底；冲突 → 409
```

### 5.7 解绑
```
DELETE /api/admin/auth/bind/wechat   (STAFF token)
  ↓
1. 计算当前 staff 剩余可用登录方式：password / wechat / alipay / sms（手机号在 → sms 永远可用）
2. 第一期 sms 永远可用，因此解绑微信不会导致无登录方式 → 跳过冲突检查
3. DELETE FROM staff_social_accounts WHERE staff_id=? AND provider=?
4. 写 audit
```
保留约束逻辑（基于"剩余至少一种"），便于将来关闭 SMS 登录时无需改代码。

### 5.8 Token 刷新（保留共用 `/api/auth/refresh`）
```
POST /api/auth/refresh {refreshToken}
  ↓
1. token_hash = SHA256(refreshToken)
2. SELECT refresh_tokens WHERE token_hash = ?
3. 不存在 → 401
4. revoked_at IS NOT NULL → 视为重放/泄露：撤销该 (subject_type, subject_id) 的所有 refresh，写 audit REFRESH_REUSE → 401
5. expires_at < now() → 401
6. 旋转：UPDATE old SET revoked_at=now()；INSERT 新行；签发新 access JWT
```

### 5.9 设/改密码与重置密码
- `set-password`：
  - 当前 staff 无密码：仅需 `{newPassword, smsCode}`（一次 SET_PASSWORD 短信确认）。
  - 当前 staff 有密码：需要 `{oldPassword, newPassword}`。
- `reset-password`（公开）：`{phone, smsCode, newPassword}`，purpose=RESET_PASSWORD；成功后清零失败计数和 locked_until，并撤销该 staff 全部 refresh。

### 5.10 登出
- 请求体可选 `{refreshToken}`：给了就撤销该 token；没给就撤销当前 staff 最近一条未撤销 refresh。
- access token 不入黑名单，依赖短过期自然失效。

### 5.11 JWT claims
- `iss`：`auth.jwt.issuer`
- `sub`：staff id（或 BIND_PENDING 时为 staff_social_accounts.id）
- `typ`：`STAFF` / `BIND_PENDING`
- `provider`：仅 BIND_PENDING token 携带
- `roles`：staff 角色数组（仅 STAFF token）
- `jti`：UUID
- `iat` / `exp`
- 算法：HS256，secret 从环境变量读取，启动校验长度 ≥ 32 字节。

## 6. Spring Security 配置

3 条 `SecurityFilterChain`：

1. **publicChain**（@Order 1，`permitAll`）：
   - `/api/admin/auth/login`
   - `/api/admin/auth/sms/send`
   - `/api/admin/auth/sms/login`
   - `/api/admin/auth/sso/**/start`
   - `/api/admin/auth/sso/**/exchange`
   - `/api/admin/auth/reset-password`
   - `/api/admin/auth/bind-phone`（凭 BIND_PENDING token，由 controller 单独校验）
   - `/api/auth/refresh`

2. **adminChain**（@Order 2，securityMatcher `/api/admin/**`）：
   - 要求 JWT authority `TYPE_STAFF`（即 `typ=STAFF`）。
   - BIND_PENDING token 不会被这里接受（authority 不匹配）。

3. **defaultChain**（@Order 99，`permitAll`）：
   - 兼容现有 `/api/stores`；后续业务模块逐步迁入 `/api/admin/**`。

公共配置：所有链 `STATELESS`，关闭 CSRF（无 cookie 方案）。

`JwtAuthenticationFilter`：解析 `Authorization: Bearer ...`，成功放入 `SecurityContext`；失败不抛异常，由 `JsonAuthEntryPoint` 统一返 401 JSON。

`@RestControllerAdvice AuthExceptionHandler` 集中处理认证相关异常 → 第 4.2 节错误响应表。

业务 controller 通过 `@AuthenticationPrincipal StaffPrincipal me` 强类型拿当前 staff。

## 7. 配置项与依赖

### 7.1 `build.gradle`
```gradle
implementation 'org.springframework.boot:spring-boot-starter-security'
implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-impl:0.12.6'
runtimeOnly    'io.jsonwebtoken:jjwt-jackson:0.12.6'
testImplementation 'org.springframework.security:spring-security-test'
```
BCrypt 由 Spring Security 自带。HTTP 客户端用 Spring 自带 `RestClient`。

### 7.2 `application.properties`
```properties
# JWT
auth.jwt.issuer=qz-shop-be
auth.jwt.secret=${AUTH_JWT_SECRET}
auth.jwt.access-ttl=PT30M
auth.jwt.refresh-ttl=P14D
auth.jwt.bind-pending-ttl=PT5M

# 密码登录锁定
auth.staff.max-failed-attempts=5
auth.staff.lock-duration=PT15M

# 短信
auth.sms.provider=mock                 # mock / aliyun / tencent
auth.sms.code-ttl=PT5M
auth.sms.resend-cooldown=PT1M
auth.sms.daily-limit=10

# SSO - 微信扫码
auth.sso.wechat.app-id=${WECHAT_OPEN_APPID}
auth.sso.wechat.app-secret=${WECHAT_OPEN_SECRET}
auth.sso.wechat.redirect-uri=${WECHAT_OPEN_REDIRECT}

# SSO - 支付宝扫码
auth.sso.alipay.app-id=${ALIPAY_OPEN_APPID}
auth.sso.alipay.private-key=${ALIPAY_PRIVATE_KEY}
auth.sso.alipay.public-key=${ALIPAY_PUBLIC_KEY}
auth.sso.alipay.redirect-uri=${ALIPAY_REDIRECT}
```
`@ConfigurationProperties("auth")` 强类型绑定，启动校验 `secret` 长度。

## 8. 测试策略（TDD，沿用 Store 模块风格）

| 层 | 工具 | 覆盖重点 |
|---|---|---|
| `JwtServiceTest` | 纯单测 | 签发/解析/过期/篡改/错误算法/BIND_PENDING 类型 |
| `TokenServiceTest` | `@DataJpaTest` | 旋转、撤销、重放检测一次性失效全部 |
| `LoginAttemptServiceTest` | `@DataJpaTest` | 失败计数、锁定窗口、解锁清零 |
| `SmsVerificationServiceTest` | `@DataJpaTest` + 内存 fake provider | 发送节流、校验、attempts 上限、过期 |
| `MockSmsProviderTest` | 纯单测 | dev 永远 123456 / 生产打日志 |
| `WechatScanProviderTest` | MockWebServer | 正常 / 平台错误码 / 网络异常 |
| `AlipayScanProviderTest` | MockWebServer | 同上 |
| `StaffAuthServiceTest` | Mockito | 密码成功/失败/锁定/禁用/假比对、SMS 登录、扫码命中、扫码 BIND_PENDING |
| `StaffRegistrationServiceTest` | Mockito | 自助注册成功、并发去重、绑定到既有 staff |
| `StaffAuthControllerTest` | MockMvc `standaloneSetup` | 200/400/401/403/409/423/429 各路径 |
| `SecurityFilterChainIT` | `@SpringBootTest` + MockMvc | 未登录 401、BIND_PENDING token 不能进业务接口、公开路径放行 |
| `AuthFlowIT` | `@SpringBootTest` | 扫码 → BIND_PENDING → 绑手机号 → 业务请求 → refresh → logout 全链路 |

约束：Java 25 + Mockito/ByteBuddy 兼容性问题——MockMvc 优先 `standaloneSetup`，避免 `@WebMvcTest + @MockBean`。

## 9. 实施路线图（3 个 PR）

| PR | 范围 | 关键交付 |
|---|---|---|
| **PR1 auth-foundation** | DB migration（`staff` 改造 + 4 张新表）+ Spring Security 3 条 FilterChain 骨架 + JwtService（含 BIND_PENDING 类型）+ TokenService（含旋转）+ SmsProvider 抽象 + MockSmsProvider + SmsVerificationService + 单测 | 不暴露登录端点；现有 Store 接口保持可用 |
| **PR2 staff-password-and-sms** | StaffAuthService/Controller 中的密码登录路径 + 锁定 + `/sms/send` `/sms/login` + `/me` `/logout` `/set-password` `/reset-password` + `/api/auth/refresh` + 集成测试 | 第一个能登录的渠道（admin 可手动 INSERT staff 后用） |
| **PR3 staff-sso-wechat-alipay** | SsoScanProvider 抽象 + Wechat/Alipay 实现 + start/exchange + BIND_PENDING 流程 + bind-phone 自助注册 + bind/{provider} 绑定 + DELETE bind/{provider} 解绑 + 集成测试 | 扫码登录闭环 |

每个 PR 单独走一次 `writing-plans` 产出实施计划。

## 10. 安全基线

第一期必须满足：
- 密码只存 BCrypt（cost ≥ 10）
- refresh token 仅存 SHA-256 hash
- SMS 验证码仅存 SHA-256 hash
- JWT secret 来自环境变量，启动校验长度 ≥ 32 字节
- 错误信息不泄露账号是否存在（密码登录失败统一 `Invalid credentials`）
- 时间侧信道：staff 找不到时仍跑一次 BCrypt 假比对
- 密码登录失败锁定 + 解锁清零；扫码 / SMS 登录不计入锁定
- SMS 节流（60s / 24h 上限）+ attempts 上限
- refresh 旋转 + 重放检测一次性失效全部
- BIND_PENDING token 类型隔离（FilterChain 不接受 + controller 显式校验）
- HTTPS 由部署层（Nginx/网关）强制
- 所有登录事件写 `login_audit_logs`

第一期不做（明确 out-of-scope）：
- 真实短信通道（用 MockSmsProvider 占位，留 SmsProvider 接口）
- 图形验证码 / 滑块（短信节流已能挡基础刷量）
- 双因素 / TOTP
- 设备指纹 / 风控阈值告警
- Redis 黑名单 / 全端踢下线 API
- C 端登录（小程序 SSO 等）
- 角色权限管理 UI（自助注册默认 STAFF_DEFAULT，admin 直接改库或在后续 staff 管理模块里升权）
- 企业微信 / 钉钉 SSO

## 11. 未决事项

- `staff` 表已有 `username` 字段是否真要保留：PR1 实施时核对当前 schema，确认是否能直接改为可空或废弃。
- `STAFF_DEFAULT` 角色与现有 staff 角色字段（schema 中 staff 是否已有 role 字段）的对接方式：PR1 实施时确认 staff schema 详情，必要时补 `staff.role VARCHAR(32) NOT NULL DEFAULT 'STAFF_DEFAULT'`。
- 微信网站应用 `redirect_uri` 一般要求是后端可回调地址。前端用 SPA 的话，前端通过 `state` 自己 polling exchange 还是直接接受重定向，PR3 实施时与前端对齐。
- 支付宝第三方应用授权登录的 sandbox 联调：PR3 实施时申请沙箱应用并验证回调链路。
