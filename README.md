# QZ Shop Customer Deployment

本仓库包含后端、数据库迁移和完整三容器部署编排。前端镜像由 `shop-fe` 仓库构建。

## 环境要求

- Docker Engine 24+ 与 Docker Compose v2
- 可用端口：前端 `80`、后端诊断端口 `8080`
- 两个镜像：`shop-be`、`shop-fe`

## 启动

1. 分别在前后端仓库构建镜像：`docker build -t shop-be:latest .` 和 `docker build -t shop-fe:latest .`。
2. 在本仓库执行 `cp .env.example .env`，修改数据库密码和至少 32 位随机 `AUTH_JWT_SECRET`。
3. 首次部署填写 `BOOTSTRAP_ADMIN_PHONE` 与不少于 8 位的 `BOOTSTRAP_ADMIN_PASSWORD`。
4. 执行 `docker compose up -d`，再用 `docker compose ps` 确认三个服务均为 healthy。
5. 执行 `bash scripts/smoke.sh` 验证前后端。

Flyway 会在后端启动时自动执行数据库迁移，Hibernate 仅校验结构，不会自行改表。

## 首次登录

使用 `.env` 中的管理员手机号和密码登录。账号创建后可清空两个 `BOOTSTRAP_ADMIN_*` 值并重新部署；初始化器不会覆盖已存在账号。

## 日常运维

- 日志：`docker compose logs -f shop-be` 或 `docker compose logs -f shop-fe`
- 状态：`curl http://127.0.0.1:8080/health`
- 停止：`docker compose down`
- 停止但保留数据：不要添加 `-v`

## 备份

```bash
docker compose exec -T postgres pg_dump -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Fc > shop-$(date +%F).dump
```

## 恢复

先停止后端写入，再恢复到空数据库：

```bash
docker compose stop shop-be
cat shop-YYYY-MM-DD.dump | docker compose exec -T postgres pg_restore -U "$POSTGRES_USER" -d "$POSTGRES_DB" --clean --if-exists
docker compose start shop-be
```

恢复后执行 `bash scripts/smoke.sh`。

## 升级

1. 先备份数据库。
2. 将 `.env` 的 `SHOP_BE_IMAGE`、`SHOP_FE_IMAGE` 改为不可变版本标签。
3. 执行 `docker compose pull && docker compose up -d`。
4. 检查迁移日志并运行 smoke。

## 回滚

应用回滚时把镜像标签改回上一版本并执行 `docker compose up -d`。数据库迁移默认只前进；若新版本包含不兼容迁移，必须使用升级前备份恢复，不能手工删除 Flyway 记录。

## 密钥轮换

- 更新 `AUTH_JWT_SECRET` 后重启后端，所有访问令牌立即失效，用户需重新登录。
- 轮换数据库密码时先更新 PostgreSQL 用户密码，再同步 `.env` 并重建后端容器。
- 不要提交 `.env`、数据库备份、真实手机号、密码或注册表凭证。

## 静态交付检查

`bash scripts/smoke.sh --static` 不需要 Docker 守护进程，可在交付前检查编排、健康检查、密钥模板和运维文档。
