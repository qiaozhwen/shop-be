# QZ Shop 前端执行路线（Sprint 版 · AI 可执行）

> 本文档的使命：可以**直接整份发给 AI 编码助手**（Claude / Cursor / Codex 等），让它按 Sprint/Ticket 顺序一步步把前端项目建起来，**无需再补充上下文**。
> 配套后端：`qz-shop-be`（Spring Boot 3 · PostgreSQL · JWT），后端路线见 `LEARNING_ROADMAP.md`。

---

## 0. AI Agent 使用说明（请 AI 先阅读这一节）

你是一名资深前端工程师。接下来会按 Sprint（周）× Day（天）× Ticket（原子任务）推进。执行规则：

1. **永远按 Ticket 顺序做**；每个 Ticket 做完再做下一个。
2. **每个 Ticket 做完后**：
   - 用 Git 提交一次：`[SHOP-FE-XXX] <type>(<scope>): <subject>`
   - 在 Ticket 前把 `[ ]` 改成 `[x]`
3. **做完整个 Day 后**：运行 `npm run dev`（或 `pnpm dev`）自检能跑起来，不能跑就修复再继续。
4. **做完整个 Sprint 后**：更新根目录 `README.md`，添加本 Sprint 的使用说明与截图位。
5. **遇到不确定**：选"主流、社区最多、最稳"的那个方案，不要发散。
6. **禁止**：
   - 不要加无用 `//` 注释来解释代码"做什么"
   - 不要一次性生成多 Sprint 代码，必须按节奏
   - 不要换掉下面锁死的技术栈

7. **TypeScript 严格**：全量使用 TS，禁用 `any`（必要时写 `unknown` + 类型守卫）。
8. **代码风格**：函数组件 + Hooks，API 层单独抽，UI 逻辑和请求逻辑分离。

---

## 1. 项目目标

打造**活禽零售电商**配套前端，覆盖：

| 端 | 受众 | 技术栈 | 对应后端 Sprint |
|---|---|---|---|
| **admin**（B 端管理后台） | 店长 / 员工 / 总部 | React 18 + Vite + TS + **Ant Design 5** + TanStack Query + Zustand + React Router 6 | Sprint 1–5、7（运营功能） |
| **mall**（C 端商城） | 顾客（PC + 移动端自适应） | **Next.js 14 App Router** + TS + **Tailwind CSS** + shadcn/ui + TanStack Query + Zustand | Sprint 6+（C 端商城） |

> 两个项目独立代码库，独立部署；共享一份 OpenAPI 类型定义（由后端 Swagger 生成）。

---

## 2. 仓库与目录规范

最终仓库布局（与后端平级）：

```
/Users/qiaozhen/app/
├── qz-shop-be/              # 后端（已存在）
├── qz-shop-admin/           # B 端管理后台 ← Sprint 1 创建
└── qz-shop-mall/            # C 端商城     ← Sprint 6 创建
```

### 2.1 `qz-shop-admin` 目录

```
qz-shop-admin/
├── src/
│   ├── api/              # axios + 各模块 API 封装
│   │   ├── client.ts     # axios 实例 + 拦截器
│   │   └── modules/      # storeApi.ts / staffApi.ts / productApi.ts ...
│   ├── components/       # 公共组件
│   ├── hooks/            # 自定义 hook
│   ├── layouts/          # 主布局 / 登录布局
│   ├── pages/            # 页面（每个模块一个目录）
│   ├── router/           # 路由定义 + 权限守卫
│   ├── store/            # Zustand 全局状态（user、menu 等）
│   ├── types/            # TS 类型定义（可由 OpenAPI 生成）
│   ├── utils/            # 通用工具
│   ├── App.tsx
│   └── main.tsx
├── .env.development
├── .env.production
├── index.html
├── package.json
├── tsconfig.json
├── vite.config.ts
└── README.md
```

### 2.2 `qz-shop-mall` 目录

```
qz-shop-mall/
├── src/
│   ├── app/              # Next.js App Router
│   │   ├── (shop)/       # 商城路由组
│   │   ├── (auth)/       # 登录/注册路由组
│   │   └── api/          # 极少数需要 BFF 的路由
│   ├── components/
│   │   ├── ui/           # shadcn 组件
│   │   └── business/     # 业务组件
│   ├── lib/              # fetcher、工具
│   ├── hooks/
│   ├── store/            # Zustand
│   └── types/
├── public/
├── .env.local
├── package.json
└── tailwind.config.ts
```

---

## 3. 后端 API 契约（AI 对接必读）

### 3.1 基础信息

- **Dev 环境**：`http://localhost:8080`
- **统一响应结构**（Sprint 4 后生效，Sprint 1–3 期间可能是裸 JSON，要兼容）：

```ts
interface Result<T> {
  code: number;       // 200 成功
  message: string;
  data: T;
  traceId?: string;
}

interface PageResult<T> {
  list: T[];
  total: number;
  page: number;
  size: number;
}
```

- **鉴权**（Sprint 5 后生效）：`Authorization: Bearer <jwt>`
- **分页参数**：`?page=0&size=20&sort=id,desc`

### 3.2 主要模块接口（按后端 Sprint 陆续可用）

| 模块 | 端点 | 后端 Sprint |
|---|---|---|
| 门店 | `GET/POST/PUT/DELETE /api/stores` | 1 |
| 员工 | `GET/POST/PUT/DELETE /api/staff` | 1 |
| 分类 | `GET/POST/PUT/DELETE /api/categories` | 1 |
| 商品 | `GET/POST/PUT/DELETE /api/products` | 1 |
| 门店定价 | `PUT /api/stores/{storeId}/products/{productId}/price` | 2 |
| 库存 | `POST /api/inventory/incoming` / `GET /api/stores/{id}/inventory` | 2 |
| 图片上传 | `POST /api/upload/image` | 2 |
| 客户 | `/api/customers` | 3 |
| 订单 | `/api/orders` | 3 |
| 登录 | `POST /api/auth/login` / `POST /api/customer/login` | 5 |
| C 端商品 | `/api/shop/stores/*` `/api/shop/products/*` | 6 |
| 购物车 | `/api/cart` | 6 |
| 优惠券 | `/api/coupons` | 6 |
| 支付 | `/api/payments` | 7 |
| 退款 | `/api/orders/{id}/refund` | 7 |
| 宰杀 | `/api/slaughter` | 7 |
| 对账 | `/api/settlements` | 7 |

> **对接原则**：后端接口没做好之前，用 **MSW（Mock Service Worker）** 按契约先 mock，切后端时只改 baseURL，不改代码。

---

## 4. Ticket 规范

沿用后端风格：

```
- [ ] [SHOP-FE-101] 创建门店列表页
  - 目标: 一句话
  - 验收: 明确的、可测试的
  - 技术点: 用到的 API / 库 / 概念
  - 接口: 依赖后端接口
```

状态标记：`[ ]` 待办 · `[~]` 进行中 · `[x]` 已完成 · `[-]` 不做

Commit 前缀：`[SHOP-FE-XXX]`
示例：`[SHOP-FE-105] feat(store): add store list page with pagination`

---

## 5. 总览：10 个 Sprint（与后端 1:1 对齐）

| Sprint | 周主题 | 前端产出 |
|---|---|---|
| Sprint 0 | 双项目脚手架 | admin + mall 项目骨架 |
| Sprint 1 | 基础数据 CRUD | 门店/员工/分类/商品管理页 |
| Sprint 2 | 商品体系完善 | 定价页、库存页、图片上传 |
| Sprint 3 | 订单管理 | 客户、下单、订单列表/详情、状态流转 |
| Sprint 4 | 工程化基线 | 统一响应适配、全局错误、权限路由、OpenAPI 自动类型 |
| Sprint 5 | 鉴权与权限 | 登录页、token 刷新、菜单权限、按钮权限 |
| Sprint 6 | C 端商城上线 | Next.js 骨架、首页、商品列表/详情、购物车 |
| Sprint 7 | 支付 & 履约 | 下单收银台、支付、订单追踪、宰杀录入 |
| Sprint 8 | 性能与体验 | 骨架屏、虚拟滚动、图片懒加载、搜索 |
| Sprint 9 | 实时与通知 | WebSocket、消息中心、异步任务进度 |
| Sprint 10 | 测试 & 部署 | Vitest + Playwright、Docker、CI/CD、监控 |

---

# Sprint 0 · 双项目脚手架（Day 0）

**目标**：建起 admin + mall 两个项目骨架，能启动、能访问空白首页。

### Day 0 - 脚手架

- [ ] [SHOP-FE-001] 创建 `qz-shop-admin` 项目
  - 命令: `pnpm create vite qz-shop-admin --template react-ts`
  - 验收: `pnpm dev` 能打开 http://localhost:5173
- [ ] [SHOP-FE-002] admin 接入 Ant Design 5 + dayjs
  - 验收: 页面能渲染一个 `<Button type="primary">Hello</Button>`
  - 技术点: `antd`、`@ant-design/icons`
- [ ] [SHOP-FE-003] admin 接入 React Router 6 + 基础布局
  - 验收: `/login` 和 `/` 可路由，`/` 使用含侧边栏的 `AdminLayout`
  - 结构: `Sider (Menu) + Header + Content`
- [ ] [SHOP-FE-004] admin 接入 axios + TanStack Query
  - 验收: `src/api/client.ts` 导出统一 axios 实例；`QueryClientProvider` 包裹 App
  - 技术点: `@tanstack/react-query`、`axios`
- [ ] [SHOP-FE-005] admin 接入 Zustand + 环境变量
  - 验收: `useUserStore` 包含 `token / userInfo / login / logout`；`.env.development` 配置 `VITE_API_BASE=http://localhost:8080`
- [ ] [SHOP-FE-006] admin 代码规范
  - 验收: 接入 ESLint + Prettier + husky + lint-staged；commit 会自动 lint
- [ ] [SHOP-FE-007] admin README
  - 验收: 写明脚本、目录结构、端口、技术栈

> `mall` 项目放到 Sprint 6 再建，避免前期分心。

---

# Sprint 1 · 基础数据 CRUD（Day 1–5）

**目标**：admin 能对门店/员工/分类/商品做完整 CRUD，UI 统一走 Ant Design Pro Table 风格。

### Day 1 - Axios 拦截器 + Mock

- [ ] [SHOP-FE-101] axios 请求拦截器：自动附 token（从 Zustand 取）
- [ ] [SHOP-FE-102] axios 响应拦截器：解包 `Result<T>`，非 200 抛 `message.error`
  - 验收: `response.data.code !== 200` 自动报错；401 自动跳 `/login`
- [ ] [SHOP-FE-103] 接入 MSW，mock `/api/stores` 列表
  - 验收: 未连后端时也能跑通整个 admin
  - 技术点: `msw`、`worker.start()`
- [ ] [SHOP-FE-104] 类型定义：新建 `src/types/store.ts`
  - 字段: id, name, address, phone, ownerName, status, createdAt

### Day 2 - 门店管理

- [ ] [SHOP-FE-201] 门店列表页 `/stores`
  - 组件: Ant Design `Table` + `Pagination`
  - 功能: 分页、按门店名模糊搜索、按 status 筛选
  - Hook: 用 `useQuery(['stores', query], fetchStores)`
- [ ] [SHOP-FE-202] 新建门店弹窗 `StoreFormModal`
  - 功能: 表单校验（name / phone 必填，phone 手机号正则）
  - Hook: `useMutation` + 成功后 `invalidateQueries(['stores'])`
- [ ] [SHOP-FE-203] 编辑门店（复用同一个 Modal，传 initialValue）
- [ ] [SHOP-FE-204] 删除门店（Popconfirm 确认）
- [ ] [SHOP-FE-205] 门店详情抽屉 `StoreDetailDrawer`
  - 使用: 点击行展开；显示门店基本信息 + 预留"员工/商品"两个 Tab

### Day 3 - 员工管理

- [ ] [SHOP-FE-301] 员工列表页 `/staff`
  - 筛选: 门店下拉、角色多选、状态
  - 列: 姓名、手机号、门店、角色、状态、操作
- [ ] [SHOP-FE-302] 新增/编辑员工 `StaffFormModal`
  - 字段: 姓名、手机号、所属门店（下拉）、角色（ADMIN/MANAGER/BUTCHER/SELLER）、密码（新增时必填）
- [ ] [SHOP-FE-303] 员工启停：列表行操作按钮，调 PUT 改 status
- [ ] [SHOP-FE-304] "门店下员工"联动：门店详情 Tab 展示该门店所有员工

### Day 4 - 分类管理

- [ ] [SHOP-FE-401] 分类列表页 `/categories`
  - 组件: 带拖拽排序的 Table（`@dnd-kit/core` 或 AntD 自带 `sortableTr`）
  - 字段: name、sortOrder、操作
- [ ] [SHOP-FE-402] 新增/编辑分类 Modal
- [ ] [SHOP-FE-403] 拖拽调整 `sortOrder` 并保存

### Day 5 - 商品管理

- [ ] [SHOP-FE-501] 商品列表页 `/products`
  - 功能: 按分类筛选、按名称搜索、分页
  - 列: 图片占位、名称、分类、默认价、单位、描述、操作
- [ ] [SHOP-FE-502] 新增/编辑商品 `ProductFormModal`
  - 字段: 分类、名称、单位（斤/只）、默认价、描述
- [ ] [SHOP-FE-503] 商品详情抽屉（为 Sprint 2 的门店定价做准备，预留 Tab）
- [ ] [SHOP-FE-504] **Sprint 1 验收**：全模块 CRUD 通过，无前端报错，刷新不丢状态

---

# Sprint 2 · 商品体系完善（Day 6–10）

### Day 6 - 门店定价

- [ ] [SHOP-FE-601] 门店定价页 `/stores/:id/prices`
  - UI: 左侧"当前门店已上架商品"，右侧"可添加商品"拖拽 / 点击添加
- [ ] [SHOP-FE-602] 行内编辑价格：单元格双击可编辑，失焦保存
  - 技术点: Antd `Table` `editable` 用法
- [ ] [SHOP-FE-603] 批量调价：选中多行 → 模态框输入"上调 X% / 下调 X 元"

### Day 7 - 库存

- [ ] [SHOP-FE-701] 门店当日库存页 `/stores/:id/inventory`
  - 默认: 今天；支持选日期
- [ ] [SHOP-FE-702] 进货登记模态框：选商品 + 进货数量
- [ ] [SHOP-FE-703] 库存可视化：低库存红标（`remainingQty < 5` 飘红）
- [ ] [SHOP-FE-704] 当前库存总览仪表盘（小饼图 + 数字卡片）
  - 技术点: `@ant-design/charts` 或 `echarts-for-react`

### Day 8 - 文件上传组件

- [ ] [SHOP-FE-801] 封装 `ImageUpload` 组件（单图/多图、裁剪、预览）
  - 上传方式: `multipart/form-data` 到 `/api/upload/image`
- [ ] [SHOP-FE-802] 把商品编辑里的"图片"接入 `ImageUpload`
- [ ] [SHOP-FE-803] 限制：仅 jpg/png，≤2MB，前端提前校验

### Day 9 - 表单体系

- [ ] [SHOP-FE-901] 抽出通用 `SchemaForm`：基于配置渲染表单
- [ ] [SHOP-FE-902] 业务表单全部迁移到 `SchemaForm`（减少重复代码）
- [ ] [SHOP-FE-903] 表单联动示例（选分类 → 动态变字段可选项）

### Day 10 - 列表通用化

- [ ] [SHOP-FE-1001] 封装 `ProTable`：封装分页、筛选、刷新、导出 CSV
- [ ] [SHOP-FE-1002] 列表列配置支持持久化（存 localStorage）
- [ ] [SHOP-FE-1003] **Sprint 2 验收**：商品含图、门店定价可编辑、库存可视化

---

# Sprint 3 · 订单管理（Day 11–15）

### Day 11 - 客户管理

- [ ] [SHOP-FE-1101] 客户列表页 `/customers`
  - 搜索: 手机号、姓名
  - 列: 姓名、手机、门店、积分、备注
- [ ] [SHOP-FE-1102] 客户详情抽屉：显示该客户历史订单、总消费、积分变动
- [ ] [SHOP-FE-1103] 积分调整：加/减积分，附原因

### Day 12 - 订单列表

- [ ] [SHOP-FE-1201] 订单列表页 `/orders`
  - 筛选: 订单号、门店、状态、下单时间、支付方式
  - 列: 订单号、门店、客户、金额、状态、支付方式、下单时间
  - 状态 Tag 颜色: PENDING=蓝 / PAID=绿 / PROCESSING=黄 / COMPLETED=默认 / CANCELLED=红
- [ ] [SHOP-FE-1202] 订单详情抽屉：头部信息 + 明细表格 + 状态时间线

### Day 13 - 下单页（B 端收银）

- [ ] [SHOP-FE-1301] 收银台页 `/pos`：类似 POS 的下单界面
  - 左侧: 商品选择（分类 Tab + 商品卡片）
  - 右侧: 当前订单（明细 + 总价）
- [ ] [SHOP-FE-1302] 添加商品到订单：可输入数量、重量、单价
- [ ] [SHOP-FE-1303] 提交订单：选客户（可空）+ 支付方式 → 调 `/api/orders`
- [ ] [SHOP-FE-1304] 提交成功后打印小票预览（可选）

### Day 14 - 订单状态流转

- [ ] [SHOP-FE-1401] 订单详情抽屉加"修改状态"按钮
  - 组件: Steps 显示当前状态；按钮按状态机允许的方向显示
- [ ] [SHOP-FE-1402] 取消订单：二次确认 + 原因输入

### Day 15 - 仪表盘 v1

- [ ] [SHOP-FE-1501] 首页 Dashboard `/dashboard`
  - 卡片: 今日订单数、今日营收、今日客单价、昨日同比
  - 图表: 近 7 天营收折线、支付方式占比饼图
- [ ] [SHOP-FE-1502] **Sprint 3 验收**：能在前端完整下一单，能看到 Dashboard 实时数字变化

---

# Sprint 4 · 工程化基线（Day 16–20）

### Day 16 - 统一响应适配

- [ ] [SHOP-FE-1601] 后端进入 Sprint 4 后，更新响应拦截器严格按 `Result<T>`
- [ ] [SHOP-FE-1602] `traceId` 透传：请求头加 `X-Trace-Id`，错误时打印在 console

### Day 17 - 全局错误边界

- [ ] [SHOP-FE-1701] React `ErrorBoundary` 组件：友好错误页 + "返回首页 / 刷新"
- [ ] [SHOP-FE-1702] 404 / 500 / 403 三张错误页

### Day 18 - 表单校验一致性

- [ ] [SHOP-FE-1801] 校验规则抽到 `src/utils/validators.ts`（手机号、金额、中文名等）
- [ ] [SHOP-FE-1802] 后端 400 返回的字段错误自动回填到表单（`form.setFields`）

### Day 19 - OpenAPI 自动生成类型

- [ ] [SHOP-FE-1901] 装 `openapi-typescript`
- [ ] [SHOP-FE-1902] 脚本: `pnpm gen:api` 拉取 `http://localhost:8080/v3/api-docs` 生成 `src/types/api.d.ts`
- [ ] [SHOP-FE-1903] 改造 axios 调用，参数和返回全用生成的类型

### Day 20 - 主题与国际化

- [ ] [SHOP-FE-2001] Ant Design 暗色模式开关（`ConfigProvider theme`）
- [ ] [SHOP-FE-2002] i18n 框架接入（`react-i18next`），先中文一种
- [ ] [SHOP-FE-2003] **Sprint 4 验收**：换肤不刷页；OpenAPI 改动后 `pnpm gen:api` 一键同步

---

# Sprint 5 · 鉴权与权限（Day 21–25）

### Day 21 - 登录页

- [ ] [SHOP-FE-2101] 登录页 `/login`
  - UI: 左侧 brand 图 + 右侧表单（用户名/密码 + 记住我）
  - 请求: `POST /api/auth/login` → 保存 token 到 Zustand + localStorage
- [ ] [SHOP-FE-2102] 登出：清 token → 跳 `/login`

### Day 22 - Token 刷新

- [ ] [SHOP-FE-2201] 响应拦截器：401 用 refreshToken 换新 token 后自动重放原请求
  - 技术点: 并发请求队列；刷新中其它请求等待
- [ ] [SHOP-FE-2202] Token 过期前 5 分钟自动续期（定时器或 Page Focus 触发）

### Day 23 - 菜单 & 路由权限

- [ ] [SHOP-FE-2301] 菜单由后端返回（`GET /api/auth/menu`）动态渲染
- [ ] [SHOP-FE-2302] 路由守卫：无权限访问跳 403
- [ ] [SHOP-FE-2303] 刷新页面后不丢菜单（持久化）

### Day 24 - 按钮权限

- [ ] [SHOP-FE-2401] `<Auth code="store.delete">` 组件或 `useAuth()` hook
- [ ] [SHOP-FE-2402] 列表操作按钮按权限显隐

### Day 25 - 个人中心

- [ ] [SHOP-FE-2501] 页面 `/profile`：修改密码、头像、昵称
- [ ] [SHOP-FE-2502] 操作日志 Tab（调 `GET /api/audit-logs/mine`）
- [ ] [SHOP-FE-2503] **Sprint 5 验收**：不同角色登录后菜单/按钮不同；token 过期能无感刷新

---

# Sprint 6 · C 端商城上线（Day 26–30）

**本 Sprint 开始做 `qz-shop-mall` 项目。**

### Day 26 - mall 项目脚手架

- [ ] [SHOP-FE-2601] 创建 Next.js 14 项目
  - 命令: `pnpm create next-app qz-shop-mall --typescript --tailwind --eslint --app`
- [ ] [SHOP-FE-2602] 接入 shadcn/ui：`pnpm dlx shadcn@latest init`
- [ ] [SHOP-FE-2603] 接入 TanStack Query + axios + Zustand（同 admin 架构）
- [ ] [SHOP-FE-2604] 配 `next.config.js` 代理 `/api` 到 `http://localhost:8080`
- [ ] [SHOP-FE-2605] 移动优先：Tailwind 配 `sm/md/lg`，主容器 `max-w-md mx-auto`

### Day 27 - 首页与门店

- [ ] [SHOP-FE-2701] 首页 `/`：顶部轮播 + 分类导航 + 热销商品
- [ ] [SHOP-FE-2702] 门店选择页 `/stores`（简化：列表，下一步接地理位置）
- [ ] [SHOP-FE-2703] 选中门店存 `localStorage` + Zustand

### Day 28 - 商品

- [ ] [SHOP-FE-2801] 商品列表 `/products?categoryId=`
  - 布局: 两列卡片；支持下拉加载更多
  - 技术点: `useInfiniteQuery`
- [ ] [SHOP-FE-2802] 商品详情 `/products/[id]`
  - 内容: 图、名称、单位、价格、描述、加入购物车按钮
- [ ] [SHOP-FE-2803] 搜索页 `/search`：实时搜索（debounce 300ms）

### Day 29 - 购物车

- [ ] [SHOP-FE-2901] 购物车页 `/cart`
  - 功能: +/- 数量、勾选、全选、删除、总价
- [ ] [SHOP-FE-2902] 浮动"加入购物车"反馈（toast + 右上角红点数）
- [ ] [SHOP-FE-2903] 未登录时购物车存本地，登录后合并到服务端

### Day 30 - 下单

- [ ] [SHOP-FE-3001] 结算页 `/checkout`
  - 项: 商品确认、自取/配送、地址选择、备注、优惠券、金额明细
- [ ] [SHOP-FE-3002] 下单成功页 `/order/success`
- [ ] [SHOP-FE-3003] "我的订单" `/orders` + 订单详情 `/orders/[id]`
- [ ] [SHOP-FE-3004] **Sprint 6 验收**：手机浏览器访问能完整跑通 浏览 → 加购 → 下单

---

# Sprint 7 · 支付 & 履约（Day 31–35）

### Day 31 - 收银台（C 端）

- [ ] [SHOP-FE-3101] 支付方式选择：微信/支付宝（沙盒 Mock 即可）
- [ ] [SHOP-FE-3102] 扫码支付页：展示假二维码 + 轮询订单状态
- [ ] [SHOP-FE-3103] 支付成功/失败页

### Day 32 - 退款申请（C 端）

- [ ] [SHOP-FE-3201] 订单详情加"申请退款"按钮
- [ ] [SHOP-FE-3202] 退款表单：原因、说明、图片（可选）

### Day 33 - 退款审核（B 端）

- [ ] [SHOP-FE-3301] 退款列表页 `/refunds`（admin）
- [ ] [SHOP-FE-3302] 审核通过/驳回

### Day 34 - 宰杀录入（B 端）

- [ ] [SHOP-FE-3401] 宰杀工作台 `/slaughter`
  - 左: 待宰杀订单明细列表
  - 右: 录入表单（毛重、净重、宰杀师傅）
- [ ] [SHOP-FE-3402] 提交后订单明细状态变更

### Day 35 - 对账

- [ ] [SHOP-FE-3501] 对账列表页 `/settlements`（按日期、门店）
- [ ] [SHOP-FE-3502] 对账详情：总订单数、三种支付方式金额、明细跳转订单
- [ ] [SHOP-FE-3503] 导出 Excel（前端用 SheetJS 即可，或调后端异步任务）
- [ ] [SHOP-FE-3504] **Sprint 7 验收**：顾客下单 → 支付 → 退款 → 宰杀 → 对账，全链路能在前端看完

---

# Sprint 8 · 性能与体验（Day 36–40）

### Day 36 - 骨架屏与加载

- [ ] [SHOP-FE-3601] 所有列表页加 `Skeleton`（Antd Skeleton / shadcn）
- [ ] [SHOP-FE-3602] 全局顶部进度条（`nprogress` 或 `@tanstack/react-query` 状态）

### Day 37 - 列表虚拟化

- [ ] [SHOP-FE-3701] admin 大表格虚拟滚动（`@tanstack/react-virtual` 或 antd `virtual`）
- [ ] [SHOP-FE-3702] mall 无限滚动流畅性优化（图片占位 + LQIP）

### Day 38 - 图片优化

- [ ] [SHOP-FE-3801] mall 用 `next/image`，配置远程域名
- [ ] [SHOP-FE-3802] admin 接入懒加载图片组件

### Day 39 - 搜索增强

- [ ] [SHOP-FE-3901] mall 搜索改走后端 ES（Sprint 8 后端已上）
- [ ] [SHOP-FE-3902] 搜索历史 + 热门搜索词

### Day 40 - 缓存策略

- [ ] [SHOP-FE-4001] TanStack Query `staleTime / gcTime` 调优
- [ ] [SHOP-FE-4002] 静态资源 CDN 准备（domain 抽到 env）
- [ ] [SHOP-FE-4003] **Sprint 8 验收**：Lighthouse 性能 ≥ 85

---

# Sprint 9 · 实时与通知（Day 41–45）

### Day 41 - WebSocket

- [ ] [SHOP-FE-4101] admin 接入 STOMP over SockJS（或原生 WS）
- [ ] [SHOP-FE-4102] 新订单实时提醒（右下角 toast + 声音）

### Day 42 - 消息中心

- [ ] [SHOP-FE-4201] 顶栏铃铛 + 未读数
- [ ] [SHOP-FE-4202] 消息中心页：系统通知、订单消息、审核消息分类

### Day 43 - 异步任务进度

- [ ] [SHOP-FE-4301] 异步导出时显示进度条
- [ ] [SHOP-FE-4302] 任务中心页：我的任务列表（跑批、导出、大量操作）

### Day 44 - 订单超时倒计时

- [ ] [SHOP-FE-4401] mall 未支付订单详情显示倒计时（与服务端时间对齐）
- [ ] [SHOP-FE-4402] 倒计时结束自动刷新订单状态

### Day 45 - 离线与 PWA（可选）

- [ ] [SHOP-FE-4501] mall 接入 PWA（可安装到主屏）
- [ ] [SHOP-FE-4502] **Sprint 9 验收**：下单后 admin 响铃；超时倒计时归零自动转取消状态

---

# Sprint 10 · 测试 & 部署（Day 46–50）

### Day 46 - 单元测试

- [ ] [SHOP-FE-4601] 接入 Vitest + `@testing-library/react`
- [ ] [SHOP-FE-4602] 覆盖: utils / hooks / 关键业务组件（>= 20 个用例）

### Day 47 - E2E 测试

- [ ] [SHOP-FE-4701] 接入 Playwright
- [ ] [SHOP-FE-4702] 覆盖核心链路: 登录 / 新增门店 / 下单 / 支付

### Day 48 - Dockerize

- [ ] [SHOP-FE-4801] admin Dockerfile（nginx 托管 dist）
- [ ] [SHOP-FE-4802] mall Dockerfile（Next.js standalone 模式）
- [ ] [SHOP-FE-4803] 更新后端 `docker-compose.yml`，加入 admin + mall 两个 service

### Day 49 - CI/CD

- [ ] [SHOP-FE-4901] GitHub Actions: push 自动 lint + test + build
- [ ] [SHOP-FE-4902] 产物发布到 GHCR / 腾讯云 COS / Vercel

### Day 50 - 监控

- [ ] [SHOP-FE-5001] 接入 Sentry（前端异常 + 源码映射）
- [ ] [SHOP-FE-5002] PV/UV 埋点（先用 umami 或自研 `/api/event`）
- [ ] [SHOP-FE-5003] Web Vitals 上报
- [ ] [SHOP-FE-5004] **🎉 最终验收**：admin 和 mall 都有公网 URL，能完整跑业务，Sentry 能捕获测试异常

---

## 6. 每日工作流（给 AI 的 SOP）

```
for each day:
  1) 读本文档，锁定今天的 Ticket 列表
  2) for each ticket:
     a) 理解目标和验收
     b) 写代码（按上面"永远按 Ticket 顺序做"）
     c) pnpm dev 自测通过
     d) git commit -m "[SHOP-FE-XXX] ..."
     e) 把 [ ] 改 [x]
  3) 日终运行 pnpm build 不报错
```

---

## 7. Definition of Done（DoD）

一个 Ticket 满足下列全部才能 `[x]`：

1. ✅ TypeScript 无 error
2. ✅ ESLint 无 error
3. ✅ 本地 `pnpm dev` 能跑，改动无 console 报错
4. ✅ 空数据 / 加载中 / 错误三态都处理了
5. ✅ 响应式：admin ≥ 1280px 不出问题；mall 375–414px 移动端无横向滚动
6. ✅ 已 git commit，message 带 ticket 号

---

## 8. 技术栈锁定清单（不要替换）

### admin

| 类别 | 选型 |
|---|---|
| 构建 | Vite 5 |
| 语言 | TypeScript 5 |
| UI | Ant Design 5 + `@ant-design/icons` + `@ant-design/charts` |
| 路由 | React Router 6 |
| 请求 | Axios + TanStack Query v5 |
| 状态 | Zustand 4 |
| 表单 | antd Form / react-hook-form（二选一，优先 antd） |
| 日期 | dayjs |
| Mock | MSW |
| 包管理 | pnpm |
| Lint | ESLint + Prettier + husky + lint-staged |
| 测试 | Vitest + @testing-library/react + Playwright |

### mall

| 类别 | 选型 |
|---|---|
| 框架 | Next.js 14 App Router |
| 语言 | TypeScript 5 |
| 样式 | Tailwind CSS 3 + shadcn/ui |
| 图标 | lucide-react |
| 请求 | fetch (Server Components) + TanStack Query (Client) |
| 状态 | Zustand |
| 图片 | next/image |

---

## 9. 代码组织规范（给 AI 的硬约束）

### 9.1 API 层模板

```ts
// src/api/modules/storeApi.ts
import client from '../client';
import type { Store, StoreCreateDTO, PageQuery, PageResult } from '@/types/api';

export const storeApi = {
  list: (params: PageQuery) => client.get<PageResult<Store>>('/api/stores', { params }),
  get:  (id: number) => client.get<Store>(`/api/stores/${id}`),
  create: (body: StoreCreateDTO) => client.post<Store>('/api/stores', body),
  update: (id: number, body: Partial<StoreCreateDTO>) => client.put<Store>(`/api/stores/${id}`, body),
  remove: (id: number) => client.delete<void>(`/api/stores/${id}`),
};
```

### 9.2 查询 Hook 模板

```ts
// src/hooks/useStores.ts
import { useQuery } from '@tanstack/react-query';
import { storeApi } from '@/api/modules/storeApi';

export function useStores(params: PageQuery) {
  return useQuery({
    queryKey: ['stores', params],
    queryFn: () => storeApi.list(params),
  });
}
```

### 9.3 页面模板（admin）

```tsx
// src/pages/stores/StoreListPage.tsx
import { Card, Table } from 'antd';
import { useStores } from '@/hooks/useStores';

export default function StoreListPage() {
  const { data, isLoading } = useStores({ page: 0, size: 20 });
  return (
    <Card title="门店管理">
      <Table
        rowKey="id"
        loading={isLoading}
        dataSource={data?.list}
        columns={[
          { title: '名称', dataIndex: 'name' },
          { title: '地址', dataIndex: 'address' },
        ]}
      />
    </Card>
  );
}
```

**必须遵守**：
- 请求**只能**在 `api/modules` 里调 `client`；页面组件只能调 `hooks/useXxx`。
- 禁止在组件里裸写 `axios.get`。
- 页面组件只负责编排，逻辑全部拆到 hooks/utils。

---

## 10. 环境变量

### admin `.env.development`
```
VITE_API_BASE=http://localhost:8080
VITE_USE_MOCK=false
```

### mall `.env.local`
```
NEXT_PUBLIC_API_BASE=http://localhost:8080
NEXT_PUBLIC_SITE_NAME=QZ 活禽鲜送
```

---

## 11. 与后端联调节奏

| 时间 | 协作模式 |
|---|---|
| 后端 Sprint N 做完 | 前端 Sprint N 开始联调（落地页面 → 对真实接口） |
| 后端在写 | 前端用 MSW Mock 并行推进 |
| 契约变更 | 先改 Swagger → `pnpm gen:api` → 修复 TS 报错的地方 |

---

## 12. 一次性可粘贴给 AI 的执行提示词

> 下面这段可整段复制，后面接上本文件内容发给 AI：

```
你是一名资深前端架构师。请严格按照下方 "QZ Shop 前端执行路线" 执行。

约束：
1) 必须按 Sprint / Day / Ticket 顺序推进，做完一个 commit 一个，不允许跳跃或合并。
2) 每做完一个 Ticket，把文档里 [ ] 改成 [x]。
3) 技术栈锁定，禁止替换。
4) 每 Ticket 满足 Definition of Done 才算完成。
5) 当前我们处于 Sprint {X} Day {Y}，请从 [SHOP-FE-{NNN}] 开始。
6) 每完成一个 Day，停下来给我一份简短报告（完成了哪些 ticket、下一步做什么），等我确认再继续。

开始前请先回复："我已理解执行路线，准备从 [SHOP-FE-{NNN}] 开始，预计本次会完成以下 ticket：...，请确认。"
```

---

> 当前状态：**尚未开始**。下一步：从 [SHOP-FE-001] 起步，创建 `qz-shop-admin` 脚手架。
