# QZ Shop 实战学习路线（Sprint 版）

> 目标：从零打造一个**生产级活禽零售电商后端系统**
> 节奏：每天 1 小时 · 每天 2–4 个 Ticket · 每周一个 Sprint
> 技术栈：Spring Boot 3 · JPA/Hibernate · PostgreSQL · Redis · RabbitMQ · MinIO · Docker · JWT/Spring Security · MapStruct · Swagger(OpenAPI) · JUnit 5

---

## 0. 项目目标与最终交付物

最终系统要包含的能力（Definition of Done）：

| 模块 | 关键能力 |
|---|---|
| 商户后台 (B 端) | 门店、员工、商品、库存、定价、订单、宰杀、对账、统计 |
| 顾客端 (C 端) | 注册登录、浏览/搜索/筛选商品、购物车、下单、支付、订单查询、评价 |
| 基础能力 | 鉴权 (JWT)、统一响应、全局异常、参数校验、日志、API 文档、参数白名单 |
| 工程化 | 单元测试 + 集成测试、Profile 多环境、Docker Compose、CI 流水线 |
| 性能 & 可用性 | Redis 缓存、分布式锁、ES 搜索、限流、熔断、消息队列、定时任务 |
| 运维 & 观测 | 日志聚合、Prometheus + Grafana 监控、健康检查、错误告警 |

---

## 1. 总览：10 个 Sprint

| Sprint | 周主题 | 产出 |
|---|---|---|
| Sprint 0 | 环境与脚手架 | 本地能跑、能连库、目录规范 |
| Sprint 1 | 基础数据 CRUD | 门店 / 员工 / 分类 / 商品 完整 CRUD |
| Sprint 2 | 商品体系完善 | 多门店定价、库存、图片上传 |
| Sprint 3 | 订单核心 | 客户、下单、订单详情、状态机 |
| Sprint 4 | 工程化基线 | 统一响应、异常、校验、日志、Swagger |
| Sprint 5 | 鉴权与权限 | JWT 登录、Spring Security、RBAC |
| Sprint 6 | C 端商城 | 商品浏览、购物车、下单 |
| Sprint 7 | 支付 & 履约 | 支付沙盒、退款、宰杀记录、对账 |
| Sprint 8 | 性能与缓存 | Redis、Redisson、ES 搜索、限流 |
| Sprint 9 | 异步与定时 | RabbitMQ、订单超时、库存预警 |
| Sprint 10 | 测试 & 部署 | JUnit、Docker、CI、监控 |

> 完成 Sprint 0-3 即拥有"能跑的 MVP"；完成 Sprint 4-7 即"对外可用"；完成 Sprint 8-10 即"生产可上线"。

---

## 2. Ticket 规范

每个 Ticket 形如：

```
- [ ] [SHOP-101] 创建 Store 实体
  - 目标: 一句话说清楚要做什么
  - 验收: 可被测试的、明确的几条
  - 技术点: 涉及的注解/类/概念
  - 接口: HTTP 方法 + 路径（如适用）
```

状态标记：
- `[ ]` 待办  ·  `[~]` 进行中  ·  `[x]` 已完成  ·  `[-]` 不做/砍掉

---

# Sprint 0 · 环境与脚手架（Day 0）

**Sprint 目标**：本地环境就绪、项目能启动、能连数据库、目录结构清晰。

### Day 0 - 开发环境

- [x] [SHOP-001] JDK / Gradle 安装
  - 验收: `java -version` ≥ 17，`./gradlew -v` 正常输出
- [x] [SHOP-002] Spring Boot 项目初始化
  - 验收: 通过 `start.spring.io` 或 IDEA 创建，能 `./gradlew bootRun` 启动
- [x] [SHOP-003] PostgreSQL 安装并创建 schema
  - 验收: 本地能登录 psql，存在 `shop` schema
- [x] [SHOP-004] 目录分层规范
  - 验收: `controller / service / dao(repository) / entity / dto / config / common` 目录存在
- [ ] [SHOP-005] Lombok 兼容性问题修复
  - 验收: `build.gradle` 中 Lombok 升级到 ≥ 1.18.42 或暂时移除；`./gradlew clean bootRun` 编译成功
  - 技术点: Lombok 注解处理器、JDK 版本兼容
- [ ] [SHOP-006] application-{dev,prod}.properties 拆分
  - 验收: `spring.profiles.active=dev` 走本地库；prod 走环境变量
  - 技术点: `@Profile`、`spring.config.activate.on-profile`

---

# Sprint 1 · 基础数据 CRUD（Day 1–5）

**Sprint 目标**：完成"门店、员工、品类、商品"4 个核心实体的完整 CRUD 接口。

### Day 1 - Hello Spring Boot ✅ 大部分已完成

- [x] [SHOP-101] HelloController 返回 JSON
  - 接口: `GET /hello?name=xxx`
- [x] [SHOP-102] 项目结构梳理
- [ ] [SHOP-103] 添加 `/health` 接口返回应用名 + 当前时间
  - 验收: 返回 `{"app":"shop-be","time":"..."}`
  - 技术点: `Map<String,Object>`、`LocalDateTime`

### Day 2 - 第一个实体 Store ✅ 基本完成

- [x] [SHOP-201] StoreEntity 字段映射
- [x] [SHOP-202] StoreRepository 继承 JpaRepository
- [x] [SHOP-203] PostgreSQL 配置 + data.sql 种子数据
- [x] [SHOP-204] 删除 `StoreRepository.findAll()` 重复声明（已被父类提供）
  - 验收: 接口里只剩自定义查询方法
- [x] [SHOP-205] StoreEntity 字段补齐
  - 验收: 增加 `createdAt / updatedAt`，使用 `@CreationTimestamp / @UpdateTimestamp`

### Day 3 - Store 完整 CRUD

- [x] [SHOP-301] `GET /api/stores` 列表查询（已部分完成）
  - 验收: 返回所有门店；字段含 id、name、status
- [x] [SHOP-302] `GET /api/stores/{id}` 详情查询
  - 验收: 不存在返回 404 + 错误信息
  - 技术点: `Optional`、`ResponseEntity`
- [x] [SHOP-303] `POST /api/stores` 新增门店
  - 验收: 请求体含 name/address/phone，落库后返回完整对象（含 id）
  - 技术点: `@RequestBody`
- [x] [SHOP-304] `PUT /api/stores/{id}` 更新门店
  - 验收: 仅更新传入的字段，未传的保持原值
  - 技术点: 部分更新、`Optional.ifPresent`
- [x] [SHOP-305] `DELETE /api/stores/{id}` 软删除
  - 验收: 把 status 置为 `CLOSED`，列表默认不再返回
  - 技术点: 软删除模式 vs 物理删除

### Day 4 - 员工 Staff 模块

- [ ] [SHOP-401] StaffEntity（含 storeId 外键、role 枚举字段）
  - 技术点: `@Enumerated(EnumType.STRING)`、`StaffRole` 枚举
- [ ] [SHOP-402] StaffRepository
  - 验收: 提供 `findByStoreId(Long storeId)` 方法名查询
- [ ] [SHOP-403] StaffService + StaffController（CRUD）
  - 接口: `GET/POST/PUT/DELETE /api/staff`
- [ ] [SHOP-404] 关联接口：`GET /api/stores/{id}/staff`
  - 验收: 返回该门店下所有员工
- [ ] [SHOP-405] 加密员工密码
  - 验收: 新增/修改员工时密码用 BCrypt 存储
  - 技术点: `BCryptPasswordEncoder`

### Day 5 - 分类 + 商品 (Category & Product)

- [ ] [SHOP-501] CategoryEntity + CRUD（含 sortOrder）
- [ ] [SHOP-502] ProductEntity（含 categoryId、unit、defaultPrice、description）
- [ ] [SHOP-503] ProductService + ProductController CRUD
- [ ] [SHOP-504] `GET /api/categories/{id}/products` 按分类查商品
  - 技术点: `findByCategoryIdOrderByIdDesc`
- [ ] [SHOP-505] **Sprint 1 验收**：用 Postman/curl 跑完一遍门店 + 员工 + 分类 + 商品全部 CRUD
  - 产出: `docs/sprint1-test.http`（IDEA HTTP Client 文件）

---

# Sprint 2 · 商品体系完善（Day 6–10）

**Sprint 目标**：实现门店级定价、库存管理、商品图片上传，构成可对外展示的商品体系。

### Day 6 - 门店定价 StoreProduct

- [ ] [SHOP-601] StoreProductEntity（store_id + product_id 联合唯一）
  - 技术点: `@UniqueConstraint`
- [ ] [SHOP-602] StoreProductRepository
  - 验收: `findByStoreIdAndProductId(Long, Long)`
- [ ] [SHOP-603] `PUT /api/stores/{storeId}/products/{productId}/price` 调整门店价
  - 验收: 不存在则新建，存在则更新
- [ ] [SHOP-604] `GET /api/stores/{storeId}/products` 返回该门店所有商品+价格
  - 技术点: 多表 JOIN（先用 Service 层组装，后续可优化为 JPQL）

### Day 7 - 库存 StoreInventory

- [ ] [SHOP-701] StoreInventoryEntity（store_id + product_id + stock_date）
- [ ] [SHOP-702] `POST /api/inventory/incoming` 进货登记
  - 验收: 累加 incoming_qty，更新 remaining_qty
- [ ] [SHOP-703] `GET /api/stores/{storeId}/inventory?date=2026-04-22` 当日库存查询
- [ ] [SHOP-704] InventoryService 提供 `decreaseStock(storeId, productId, qty)`
  - 验收: 库存不足抛 `InsufficientStockException`
  - 技术点: 自定义异常

### Day 8 - DTO 与对象转换

- [ ] [SHOP-801] 引入 MapStruct 依赖
  - 技术点: `org.mapstruct:mapstruct` + `mapstruct-processor`
- [ ] [SHOP-802] 设计 `StoreVO / ProductVO / StoreProductVO`
  - 验收: VO 不暴露密码等敏感字段
- [ ] [SHOP-803] 把 Controller 返回类型从 Entity 全部改成 VO
  - 技术点: 为什么 Entity 不应该直接返回前端
- [ ] [SHOP-804] DTO 用作请求参数（`StoreCreateDTO` / `StoreUpdateDTO`）

### Day 9 - 文件上传：商品图片

- [ ] [SHOP-901] 接入 MinIO（Docker 跑一个）
  - 技术点: Docker、MinIO 控制台
- [ ] [SHOP-902] `POST /api/upload/image` 接收 multipart 上传
  - 验收: 返回可访问 URL
  - 技术点: `MultipartFile`、`@RequestPart`
- [ ] [SHOP-903] Product 增加 `imageUrl` 字段
- [ ] [SHOP-904] 限制：仅允许 jpg/png，单文件 ≤ 2MB
  - 技术点: `application.properties` `spring.servlet.multipart.max-file-size`

### Day 10 - 分页与排序

- [ ] [SHOP-1001] 商品列表改为分页：`GET /api/products?page=0&size=20&sort=id,desc`
  - 技术点: `Pageable`、`Page<T>`
- [ ] [SHOP-1002] 统一分页响应结构 `PageResult<T>(list, total, page, size)`
- [ ] [SHOP-1003] 商品按名称模糊搜索：`GET /api/products?keyword=鸡`
  - 技术点: `findByNameContaining`、Specification
- [ ] [SHOP-1004] **Sprint 2 验收**：能上传商品图、能改门店价、能扣库存、列表带分页

---

# Sprint 3 · 订单核心（Day 11–15）

**Sprint 目标**：完成"客户—下单—订单管理—状态流转"完整链路。

### Day 11 - 客户 Customer

- [ ] [SHOP-1101] CustomerEntity（含 phone 唯一、points 积分）
- [ ] [SHOP-1102] CRUD + `GET /api/customers/by-phone/{phone}`
- [ ] [SHOP-1103] 积分接口：`POST /api/customers/{id}/points` 增/减积分
  - 验收: 不允许积分为负

### Day 12 - 订单实体设计

- [ ] [SHOP-1201] OrderEntity（order_no 唯一、status 枚举、payment_method 枚举）
- [ ] [SHOP-1202] OrderItemEntity（订单明细，含 weight、unit_price、amount）
- [ ] [SHOP-1203] OrderRepository + OrderItemRepository
- [ ] [SHOP-1204] 订单号生成器 `OrderNoGenerator`
  - 验收: 形如 `ORD20260422000001`，并发安全
  - 技术点: 雪花算法 / 数据库序列 / Redis 自增

### Day 13 - 下单流程

- [ ] [SHOP-1301] `POST /api/orders` 下单接口
  - 入参: storeId、customerId、items[{productId, quantity, weight}]
  - 验收: 一次写入 order + order_items + 扣库存
- [ ] [SHOP-1302] OrderService.createOrder() 加 `@Transactional`
  - 验收: 任意一步失败，全部回滚（用断点 + 抛异常验证）
- [ ] [SHOP-1303] 金额计算用 `BigDecimal`，禁止 double
  - 技术点: `BigDecimal.setScale(2, RoundingMode.HALF_UP)`

### Day 14 - 订单查询

- [ ] [SHOP-1401] `GET /api/orders/{id}` 订单详情（含明细）
- [ ] [SHOP-1402] `GET /api/orders?storeId=&status=&startDate=&endDate=` 多条件分页查询
  - 技术点: JPA `Specification` 或 QueryDSL
- [ ] [SHOP-1403] 关联客户和员工信息（避免 N+1）
  - 技术点: `@EntityGraph` 或 `JOIN FETCH`

### Day 15 - 订单状态机

- [ ] [SHOP-1501] OrderStatus 枚举：PENDING → PAID → PROCESSING → COMPLETED / CANCELLED
- [ ] [SHOP-1502] `PUT /api/orders/{id}/status` 状态变更
  - 验收: 非法跳转返回 400，并附详细原因
- [ ] [SHOP-1503] 取消订单：`POST /api/orders/{id}/cancel` 自动回库存
- [ ] [SHOP-1504] **Sprint 3 验收**：能完整跑通"创建客户 → 下单 → 改状态 → 取消回库存"

---

# Sprint 4 · 工程化基线（Day 16–20）

**Sprint 目标**：把项目从"能跑"升级到"可维护、可调试、可对接前端"。

### Day 16 - 统一响应

- [ ] [SHOP-1601] `Result<T>(code, message, data, traceId)` 通用包装
- [ ] [SHOP-1602] `ResultCode` 枚举：200/400/401/403/404/500/业务码
- [ ] [SHOP-1603] `@RestControllerAdvice` 自动包装返回值
  - 技术点: `ResponseBodyAdvice`
- [ ] [SHOP-1604] 改造已有所有 Controller，全部返回 `Result<T>`

### Day 17 - 全局异常处理

- [ ] [SHOP-1701] `BusinessException(code, message)` 自定义业务异常
- [ ] [SHOP-1702] `GlobalExceptionHandler` 拦截：BusinessException、ValidationException、Exception
- [ ] [SHOP-1703] 异常日志带 traceId
- [ ] [SHOP-1704] 自定义业务异常码集中管理（`ErrorCode` 接口）

### Day 18 - 参数校验

- [ ] [SHOP-1801] `spring-boot-starter-validation` 依赖
- [ ] [SHOP-1802] DTO 加 `@NotBlank / @NotNull / @Min / @Pattern` 注解
- [ ] [SHOP-1803] Controller 入参加 `@Valid`
- [ ] [SHOP-1804] 校验失败统一返回 400 + 字段级错误信息

### Day 19 - 日志与 traceId

- [ ] [SHOP-1901] logback-spring.xml：按天滚动 + 按级别拆分
- [ ] [SHOP-1902] MDC 透传 traceId（请求入口生成 UUID）
  - 技术点: `Filter` + `MDC.put("traceId", ...)`
- [ ] [SHOP-1903] 关键 Service 加 `@Slf4j` 打日志（参数、耗时、异常）
- [ ] [SHOP-1904] application.properties: `logging.level.com.qzshop=DEBUG`（dev only）

### Day 20 - API 文档：Swagger / OpenAPI

- [ ] [SHOP-2001] 引入 `springdoc-openapi-starter-webmvc-ui`
- [ ] [SHOP-2002] 访问 `/swagger-ui.html` 看到所有接口
- [ ] [SHOP-2003] 关键接口加 `@Operation / @Parameter / @Schema` 注释
- [ ] [SHOP-2004] **Sprint 4 验收**：随便挑一个接口故意传错参数，能拿到统一 400 错误结构 + traceId + 字段错误明细

---

# Sprint 5 · 鉴权与权限（Day 21–25）

**Sprint 目标**：B 端员工和 C 端客户能登录，接口按角色保护。

### Day 21 - Spring Security 入门

- [ ] [SHOP-2101] 引入 `spring-boot-starter-security`
- [ ] [SHOP-2102] `SecurityFilterChain` 配置：放开 /hello、/swagger，其余需要登录
- [ ] [SHOP-2103] 自定义 `UserDetailsService` 用 Staff 表
- [ ] [SHOP-2104] 登录接口：`POST /api/auth/login`（用户名 + 密码）

### Day 22 - JWT

- [ ] [SHOP-2201] 引入 `io.jsonwebtoken:jjwt`
- [ ] [SHOP-2202] `JwtUtil`：生成 + 解析（含 userId、role、过期）
- [ ] [SHOP-2203] `JwtAuthenticationFilter` 解析 Header `Authorization: Bearer xxx`
- [ ] [SHOP-2204] 登录成功返回 `{token, refreshToken, expiresIn}`

### Day 23 - 角色与权限

- [ ] [SHOP-2301] 启用方法级权限：`@EnableMethodSecurity`
- [ ] [SHOP-2302] 关键接口加 `@PreAuthorize("hasRole('ADMIN')")`
- [ ] [SHOP-2303] 数据权限：店长只能看自己门店的数据
  - 技术点: `SecurityContextHolder.getContext().getAuthentication()` 拿当前用户

### Day 24 - C 端客户登录

- [ ] [SHOP-2401] 客户注册：`POST /api/customer/register`（手机号 + 验证码）
- [ ] [SHOP-2402] 短信验证码（先用日志打印模拟）
- [ ] [SHOP-2403] 客户登录：`POST /api/customer/login`
- [ ] [SHOP-2404] 区分 B 端 token 与 C 端 token（可用不同前缀或不同 secret）

### Day 25 - 安全增强

- [ ] [SHOP-2501] 密码重置接口
- [ ] [SHOP-2502] 登录失败次数限制（连续 5 次锁 15 分钟）
- [ ] [SHOP-2503] 接口防刷：同 IP 1 秒最多 10 次（先用内存计数器，后续换 Redis）
- [ ] [SHOP-2504] **Sprint 5 验收**：未登录访问受保护接口 → 401；普通员工访问门店管理 → 403；token 过期 → 401 并带刷新提示

---

# Sprint 6 · C 端商城接口（Day 26–30）

**Sprint 目标**：顾客 App / 小程序能浏览商品、加购、下单。

### Day 26 - 商品浏览

- [ ] [SHOP-2601] `GET /api/shop/stores/nearby?lng=&lat=` 附近门店
- [ ] [SHOP-2602] `GET /api/shop/stores/{id}/products` 门店商品（含价格、库存、图片）
- [ ] [SHOP-2603] `GET /api/shop/products/{id}` 商品详情
- [ ] [SHOP-2604] 商品列表支持按分类筛选

### Day 27 - 购物车（数据库版）

- [ ] [SHOP-2701] CartItemEntity（customer_id + store_id + product_id 联合唯一）
- [ ] [SHOP-2702] `POST /api/cart` 加入购物车
- [ ] [SHOP-2703] `PUT /api/cart/{id}` 改数量
- [ ] [SHOP-2704] `DELETE /api/cart/{id}` 删除
- [ ] [SHOP-2705] `GET /api/cart` 列表 + 总价

### Day 28 - C 端下单

- [ ] [SHOP-2801] `POST /api/shop/orders` 顾客下单
  - 入参: storeId、cartItemIds[]、deliveryType(SELF_PICKUP/DELIVERY)、address
- [ ] [SHOP-2802] 下单成功后清空对应购物车
- [ ] [SHOP-2803] `GET /api/shop/orders/mine` 我的订单
- [ ] [SHOP-2804] `GET /api/shop/orders/{id}` 我的订单详情（带数据所有权校验）

### Day 29 - 优惠券（基础版）

- [ ] [SHOP-2901] CouponEntity（满减/折扣、有效期、领取上限）
- [ ] [SHOP-2902] CustomerCouponEntity（领取记录 + status）
- [ ] [SHOP-2903] `POST /api/coupons/{id}/claim` 领券
- [ ] [SHOP-2904] 下单时传 couponId，价格自动减免

### Day 30 - 评价

- [ ] [SHOP-3001] ReviewEntity（关联订单 + 商品 + 评分 1–5 + 内容）
- [ ] [SHOP-3002] `POST /api/orders/{id}/reviews` 订单完成后可评价
- [ ] [SHOP-3003] `GET /api/products/{id}/reviews` 商品评价列表（分页）
- [ ] [SHOP-3004] **Sprint 6 验收**：用 Postman 模拟一个顾客全流程：注册 → 浏览 → 加购 → 领券 → 下单 → 评价

---

# Sprint 7 · 支付 & 履约（Day 31–35）

**Sprint 目标**：接入支付沙盒，打通支付—履约—对账闭环。

### Day 31 - 支付抽象

- [ ] [SHOP-3101] PaymentEntity（订单号、支付方式、第三方流水号、状态）
- [ ] [SHOP-3102] `PaymentService` 接口 + WechatPayServiceImpl / AlipayServiceImpl
- [ ] [SHOP-3103] 工厂模式：根据支付方式路由到对应实现
- [ ] [SHOP-3104] `POST /api/payments` 发起支付（沙盒）

### Day 32 - 支付回调

- [ ] [SHOP-3201] `POST /api/payments/callback/wechat` 微信回调（先 mock）
- [ ] [SHOP-3202] 验签 + 幂等处理（避免回调多次重复处理）
- [ ] [SHOP-3203] 支付成功 → 订单 PAID + 发送内部事件
  - 技术点: `ApplicationEventPublisher`

### Day 33 - 退款

- [ ] [SHOP-3301] `POST /api/orders/{id}/refund` 退款申请
- [ ] [SHOP-3302] 退款后回库存、回积分
- [ ] [SHOP-3303] 退款审核流：申请 → 商家审核 → 第三方退款

### Day 34 - 宰杀记录

- [ ] [SHOP-3401] SlaughterRecordEntity（关联 order_item、毛重、净重、师傅）
- [ ] [SHOP-3402] `POST /api/slaughter` 录入宰杀
- [ ] [SHOP-3403] `GET /api/orders/{id}/slaughter` 查询订单宰杀记录
- [ ] [SHOP-3404] 订单完成时间依赖宰杀完成

### Day 35 - 每日对账

- [ ] [SHOP-3501] DailySettlementEntity
- [ ] [SHOP-3502] 定时任务：每天 23:55 汇总当日订单（先用 `@Scheduled`）
- [ ] [SHOP-3503] `GET /api/settlements?storeId=&date=` 查对账
- [ ] [SHOP-3504] **Sprint 7 验收**：完整跑通"下单 → 支付（沙盒）→ 宰杀录入 → 完成订单 → 当日对账"

---

# Sprint 8 · 性能与缓存（Day 36–40）

**Sprint 目标**：用 Redis、ES、限流让接口扛得住高并发。

### Day 36 - Redis 接入

- [ ] [SHOP-3601] Docker 起 Redis；`spring-boot-starter-data-redis`
- [ ] [SHOP-3602] `RedisTemplate` 与 `StringRedisTemplate` 配置
- [ ] [SHOP-3603] 商品详情缓存：`@Cacheable("product")`
- [ ] [SHOP-3604] 缓存失效策略：商品更新 → `@CacheEvict`

### Day 37 - 防穿透/雪崩/击穿

- [ ] [SHOP-3701] 空值缓存（防穿透）
- [ ] [SHOP-3702] 随机过期时间（防雪崩）
- [ ] [SHOP-3703] 互斥锁热点 key（防击穿）
- [ ] [SHOP-3704] 库存查询走 Redis（每天首次查询从库加载）

### Day 38 - 分布式锁

- [ ] [SHOP-3801] 引入 Redisson
- [ ] [SHOP-3802] 扣库存改为 Redisson 分布式锁
- [ ] [SHOP-3803] 短信验证码发送加锁（同手机号 60s 内只能发 1 次）

### Day 39 - 全文搜索 ES

- [ ] [SHOP-3901] Docker 起 Elasticsearch
- [ ] [SHOP-3902] `spring-boot-starter-data-elasticsearch`
- [ ] [SHOP-3903] ProductDocument + 索引同步（商品保存时双写）
- [ ] [SHOP-3904] `GET /api/shop/search?keyword=` 改走 ES（高亮、分词、按销量排序）

### Day 40 - 限流

- [ ] [SHOP-4001] 引入 Sentinel 或 Bucket4j
- [ ] [SHOP-4002] C 端核心接口：单 IP 100 QPS
- [ ] [SHOP-4003] 下单接口：单用户 1 秒 1 次
- [ ] [SHOP-4004] **Sprint 8 验收**：商品详情命中缓存（看日志）；秒杀场景下不超卖（用 jmeter 压一下）

---

# Sprint 9 · 异步与定时（Day 41–45）

**Sprint 目标**：把同步阻塞的事情扔到队列里，提升吞吐和解耦。

### Day 41 - RabbitMQ 接入

- [ ] [SHOP-4101] Docker 起 RabbitMQ
- [ ] [SHOP-4102] `spring-boot-starter-amqp`
- [ ] [SHOP-4103] 配置 Exchange / Queue / Binding（订单事件、库存事件、消息事件）

### Day 42 - 订单事件

- [ ] [SHOP-4201] 下单成功 → 发 `order.created` 事件
- [ ] [SHOP-4202] 消费者：发短信、加积分、记录日志
- [ ] [SHOP-4203] 消息可靠投递（confirm + return + 持久化）
- [ ] [SHOP-4204] 失败入死信队列 + 告警

### Day 43 - 订单超时

- [ ] [SHOP-4301] 订单创建后 15 分钟未支付自动取消
- [ ] [SHOP-4302] 用 RabbitMQ 延迟队列（TTL + DLX）实现
- [ ] [SHOP-4303] 取消时回库存

### Day 44 - 定时任务

- [ ] [SHOP-4401] 引入 XXL-Job 或保留 `@Scheduled`
- [ ] [SHOP-4402] 任务：每日对账、库存预警、积分清零、报表生成
- [ ] [SHOP-4403] 任务执行幂等

### Day 45 - 异步导出报表

- [ ] [SHOP-4501] `POST /api/reports/export` 异步生成 Excel（EasyExcel）
- [ ] [SHOP-4502] 任务进度查询接口
- [ ] [SHOP-4503] 导出文件存 MinIO，返回下载链接
- [ ] [SHOP-4504] **Sprint 9 验收**：下单后 15 分钟自动取消；异步导出 1 万条订单不阻塞主线程

---

# Sprint 10 · 测试 & 部署（Day 46–50）

**Sprint 目标**：项目可以一键部署、有测试保护、可观测。

### Day 46 - 单元测试

- [ ] [SHOP-4601] JUnit 5 + Mockito + AssertJ
- [ ] [SHOP-4602] 给 OrderService、InventoryService 写覆盖率 ≥ 70% 的单测
- [ ] [SHOP-4603] 测试数据用 `@DataJpaTest` + H2 内存库

### Day 47 - 集成测试

- [ ] [SHOP-4701] `@SpringBootTest` + `MockMvc`
- [ ] [SHOP-4702] 用 Testcontainers 起真实 PostgreSQL + Redis
- [ ] [SHOP-4703] 关键链路（注册 → 下单 → 支付）端到端测试

### Day 48 - Dockerize

- [ ] [SHOP-4801] 多阶段 Dockerfile（构建 + 运行分离）
- [ ] [SHOP-4802] `docker-compose.yml`：app + postgres + redis + rabbitmq + minio
- [ ] [SHOP-4803] 一条命令 `docker compose up -d` 跑起完整栈

### Day 49 - CI / CD

- [ ] [SHOP-4901] GitHub Actions：push 到 main 时自动 build + test
- [ ] [SHOP-4902] 单测失败阻断合并
- [ ] [SHOP-4903] Docker 镜像推到 Docker Hub / GHCR
- [ ] [SHOP-4904] 部署脚本（SSH 到服务器 pull + restart）

### Day 50 - 监控与上线

- [ ] [SHOP-5001] Spring Boot Actuator：`/actuator/health` `/actuator/metrics`
- [ ] [SHOP-5002] Prometheus 抓取 + Grafana 看板
- [ ] [SHOP-5003] 应用日志聚合到 ELK（或 Loki）
- [ ] [SHOP-5004] 错误告警接入飞书/钉钉机器人
- [ ] [SHOP-5005] **🎉 Sprint 10 验收**：拿到一个公网 URL，能完整跑业务，能在 Grafana 看到 QPS / 错误率

---

## 3. 每日工作流（Sprint 操作手册）

每天 1 小时建议这样过：

| 时间 | 动作 |
|---|---|
| 0–5 min | 看昨天提交的代码 / TODO，确认进度 |
| 5–10 min | 选今天的 1–3 个 ticket，写在便签或 IDE TODO |
| 10–50 min | 集中编码：先写接口（Controller）→ Service → DAO → 测试 |
| 50–60 min | curl/Postman 验证，把 ticket 改成 `[x]`，git commit |

**Commit 信息建议格式**：
```
[SHOP-302] feat(store): add GET /api/stores/{id}
```

---

## 4. 每周回顾模板（Sprint Retro）

每个 Sprint 结束（每周末）写一段：

```
## Sprint X 回顾（2026-MM-DD）
- 完成 ticket: X / Y
- 学到的知识点: ...
- 踩到的坑: ...
- 没做完的搬到下个 Sprint: SHOP-XXX, SHOP-YYY
- 下个 Sprint 风险: ...
```

---

## 5. 技术栈一览（什么时候学什么）

| 技术 | 首次出现 | 用途 |
|---|---|---|
| Spring Boot 3 | Sprint 0 | 主框架 |
| JPA / Hibernate | Sprint 1 | ORM |
| PostgreSQL | Sprint 0 | 主库 |
| Lombok | Sprint 0 | 简化代码（可选） |
| MapStruct | Sprint 2 | DTO ↔ Entity |
| MinIO | Sprint 2 | 对象存储 |
| Spring Security + JWT | Sprint 5 | 鉴权 |
| Springdoc OpenAPI | Sprint 4 | API 文档 |
| Redis | Sprint 8 | 缓存 / 计数 |
| Redisson | Sprint 8 | 分布式锁 |
| Elasticsearch | Sprint 8 | 搜索 |
| Sentinel / Bucket4j | Sprint 8 | 限流 |
| RabbitMQ | Sprint 9 | 异步消息 |
| EasyExcel | Sprint 9 | 报表导出 |
| JUnit 5 / Mockito / Testcontainers | Sprint 10 | 测试 |
| Docker / Compose | Sprint 10 | 部署 |
| GitHub Actions | Sprint 10 | CI |
| Prometheus / Grafana | Sprint 10 | 监控 |

---

## 6. 关键 Spring 注解速查

| 注解 | 作用 | 首次出现 |
|---|---|---|
| `@RestController` | REST 控制器 | Day 1 |
| `@GetMapping` / `@PostMapping` / `@PutMapping` / `@DeleteMapping` | HTTP 映射 | Day 1 |
| `@RequestParam` / `@PathVariable` / `@RequestBody` | 参数绑定 | Day 1–3 |
| `@Entity` / `@Table` / `@Id` / `@GeneratedValue` / `@Column` | JPA 映射 | Day 2 |
| `@Service` / `@Repository` / `@Component` | Bean 标记 | Day 3 |
| `@Autowired` / `@RequiredArgsConstructor` | 依赖注入 | Day 3 |
| `@Transactional` | 事务 | Day 13 |
| `@RestControllerAdvice` / `@ExceptionHandler` | 全局异常 | Day 17 |
| `@Valid` / `@NotBlank` / `@NotNull` | 参数校验 | Day 18 |
| `@Slf4j` | 日志 | Day 19 |
| `@Operation` / `@Schema` | OpenAPI 文档 | Day 20 |
| `@PreAuthorize` / `@EnableMethodSecurity` | 权限 | Day 23 |
| `@Cacheable` / `@CacheEvict` / `@CachePut` | Spring Cache | Day 36 |
| `@Async` | 异步 | Day 41 |
| `@Scheduled` | 定时任务 | Day 35/44 |
| `@SpringBootTest` / `@DataJpaTest` / `@WebMvcTest` | 测试切片 | Day 46 |

---

## 7. Definition of Done（每个 Ticket 收工标准）

满足以下全部才算 `[x]`：

1. ✅ 代码本地能跑通
2. ✅ 用 Postman / curl 验证过返回值
3. ✅ 写了至少一行有意义的日志
4. ✅ 异常路径已考虑（参数错、数据不存在、并发）
5. ✅ 不破坏已通过的单测
6. ✅ git 已提交，commit 带 ticket 号

---

## 8. 学习资源（按需查阅）

- Spring Boot 官方文档: https://docs.spring.io/spring-boot/
- Spring Data JPA: https://docs.spring.io/spring-data/jpa/reference/
- Baeldung（Java/Spring 教程）: https://www.baeldung.com/
- 阿里 Java 开发手册（黄山版）：代码规范底线
- 《Spring 实战（第 6 版）》：可作为枕边书

---

> 现在你在 **Sprint 1 · Day 3** 附近，建议下一步：先把 [SHOP-005] Lombok 修掉让项目能跑，再依次完成 SHOP-301~305 把 Store 的完整 CRUD 拿下，正式开启 Sprint 节奏。
