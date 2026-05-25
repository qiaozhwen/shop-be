# Docker 部署配置 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为 shop-be Spring Boot 项目添加 Docker 部署、GitHub Actions CI/CD、docker-compose 本地/服务器编排

**Architecture:** 多阶段 Docker 构建 (Gradle 构建 + JRE 运行)，推送到阿里云 ACR，GitHub Actions 自动部署到轻量应用服务器

**Tech Stack:** Docker, GitHub Actions, Alibaba Cloud ACR, Spring Boot 3.2, PostgreSQL 16

---

### Task 1: Dockerfile - 多阶段构建

**Files:**
- Create: `Dockerfile`
- Create: `.dockerignore`

- [ ] **Step 1: 创建 Dockerfile**

```dockerfile
# ---------- build stage ----------
FROM gradle:8.12-jdk17-alpine AS builder
WORKDIR /app

COPY build.gradle settings.gradle gradlew ./
COPY gradle ./gradle
RUN ./gradlew dependencies --no-daemon -q || true

COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---------- runtime stage ----------
FROM eclipse-temurin:17-jre-alpine AS runner
WORKDIR /app

RUN addgroup -S appgroup && adduser -S appuser -G appgroup

COPY --from=builder /app/build/libs/*.jar app.jar

EXPOSE 8080
USER appuser
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 2: 创建 .dockerignore**

```
.gradle
build
.idea
.vscode
.git
.github
.superpowers
docs
*.md
*.sql
.env
Dockerfile
docker-compose.yml
```

- [ ] **Step 3: 验证构建**

```bash
docker build -t shop-be:test .
```

---

### Task 2: 生产环境配置

**Files:**
- Create: `src/main/resources/application-prod.properties`

- [ ] **Step 4: 创建 application-prod.properties**

```properties
server.port=8080
spring.application.name=shop-be

# PostgreSQL (通过环境变量注入)
spring.datasource.url=${DB_URL:jdbc:postgresql://localhost:5432/postgres?currentSchema=shop}
spring.datasource.username=${DB_USERNAME:qiaozhen}
spring.datasource.password=${DB_PASSWORD:postgres}
spring.datasource.driver-class-name=org.postgresql.Driver
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
spring.jpa.hibernate.ddl-auto=none
spring.jpa.show-sql=false
spring.h2.console.enabled=false

# Auth
auth.jwt.issuer=qz-shop-be
auth.jwt.secret=${AUTH_JWT_SECRET:please-change-me-this-is-a-32byte-default-secret!!}
auth.jwt.access-ttl=PT30M
auth.jwt.refresh-ttl=P14D
auth.jwt.bind-pending-ttl=PT5M

auth.staff.max-failed-attempts=5
auth.staff.lock-duration=PT15M

auth.sms.provider=mock
auth.sms.code-ttl=PT5M
auth.sms.resend-cooldown=PT1M
auth.sms.daily-limit=10
```

---

### Task 3: deploy/deploy.sh 手动部署脚本

**Files:**
- Create: `deploy/deploy.sh`

- [ ] **Step 5: 创建 deploy.sh**

```bash
#!/usr/bin/env bash
set -euo pipefail

REGISTRY="${REGISTRY:-registry.cn-hangzhou.aliyuncs.com}"
NAMESPACE="${NAMESPACE:-qz-shop}"
IMAGE_NAME="${IMAGE_NAME:-shop-be}"
CONTAINER_NAME="${CONTAINER_NAME:-shop-be}"
HOST_PORT="${HOST_PORT:-8080}"

TAG="${1:-latest}"
IMAGE="${REGISTRY}/${NAMESPACE}/${IMAGE_NAME}:${TAG}"

echo "==> 登录镜像仓库 ${REGISTRY}"
if [[ -n "${REGISTRY_PASSWORD:-}" ]]; then
  echo "${REGISTRY_PASSWORD}" | docker login "${REGISTRY}" -u "${REGISTRY_USERNAME}" --password-stdin
else
  docker login "${REGISTRY}"
fi

echo "==> 拉取镜像 ${IMAGE}"
docker pull "${IMAGE}"

echo "==> 停止并移除旧容器 ${CONTAINER_NAME} (若存在)"
docker rm -f "${CONTAINER_NAME}" 2>/dev/null || true

echo "==> 启动新容器"
docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --network shop-net \
  -p "${HOST_PORT}:8080" \
  -e DB_URL="${DB_URL:-jdbc:postgresql://postgres:5432/postgres?currentSchema=shop}" \
  -e DB_USERNAME="${DB_USERNAME:-qiaozhen}" \
  -e DB_PASSWORD="${DB_PASSWORD:-postgres}" \
  -e AUTH_JWT_SECRET="${AUTH_JWT_SECRET}" \
  -e SPRING_PROFILES_ACTIVE=prod \
  "${IMAGE}"

echo "==> 清理悬挂镜像"
docker image prune -f

echo "==> 当前运行容器:"
docker ps --filter "name=${CONTAINER_NAME}"

echo "==> 完成"
```

- [ ] **Step 6: 添加执行权限**

```bash
chmod +x deploy/deploy.sh
```

---

### Task 4: GitHub Actions CI/CD

**Files:**
- Create: `.github/workflows/deploy.yml`

- [ ] **Step 7: 创建 GitHub Actions workflow**

```yaml
name: Build & Deploy

on:
  push:
    branches: [main]
  workflow_dispatch:

env:
  IMAGE_NAME: shop-be
  CONTAINER_NAME: shop-be
  HOST_PORT: 8080

jobs:
  build-and-deploy:
    runs-on: ubuntu-latest
    steps:
      - name: Checkout
        uses: actions/checkout@v4

      - name: Set up Docker Buildx
        uses: docker/setup-buildx-action@v3

      - name: Login to container registry
        uses: docker/login-action@v3
        with:
          registry: ${{ secrets.REGISTRY }}
          username: ${{ secrets.REGISTRY_USERNAME }}
          password: ${{ secrets.REGISTRY_PASSWORD }}

      - name: Compute image tags
        id: meta
        run: |
          IMAGE="${{ secrets.REGISTRY }}/${{ secrets.REGISTRY_NAMESPACE }}/${{ env.IMAGE_NAME }}"
          echo "image=$IMAGE" >> $GITHUB_OUTPUT
          echo "sha_tag=$IMAGE:${GITHUB_SHA::7}" >> $GITHUB_OUTPUT
          echo "latest_tag=$IMAGE:latest" >> $GITHUB_OUTPUT

      - name: Build and push
        uses: docker/build-push-action@v6
        with:
          context: .
          push: true
          tags: |
            ${{ steps.meta.outputs.sha_tag }}
            ${{ steps.meta.outputs.latest_tag }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

      - name: Deploy on server via SSH
        uses: appleboy/ssh-action@v1.2.0
        with:
          host: ${{ secrets.SSH_HOST }}
          username: ${{ secrets.SSH_USER }}
          port: ${{ secrets.SSH_PORT }}
          key: ${{ secrets.SSH_PRIVATE_KEY }}
          script: |
            set -e
            echo "${{ secrets.REGISTRY_PASSWORD }}" | docker login ${{ secrets.REGISTRY }} -u "${{ secrets.REGISTRY_USERNAME }}" --password-stdin
            docker pull ${{ steps.meta.outputs.latest_tag }}
            docker rm -f ${{ env.CONTAINER_NAME }} 2>/dev/null || true
            docker run -d \
              --name ${{ env.CONTAINER_NAME }} \
              --restart unless-stopped \
              --network shop-net \
              -p ${{ env.HOST_PORT }}:8080 \
              -e DB_URL="${{ secrets.DB_URL }}" \
              -e DB_USERNAME="${{ secrets.DB_USERNAME }}" \
              -e DB_PASSWORD="${{ secrets.DB_PASSWORD }}" \
              -e AUTH_JWT_SECRET="${{ secrets.AUTH_JWT_SECRET }}" \
              -e SPRING_PROFILES_ACTIVE=prod \
              ${{ steps.meta.outputs.latest_tag }}
            docker image prune -f
```

---

### Task 5: docker-compose.yml 服务器编排

**Files:**
- Create: `docker-compose.yml`

- [ ] **Step 8: 创建 docker-compose.yml**

```yaml
version: "3.9"

services:
  postgres:
    image: postgres:16-alpine
    container_name: shop-postgres
    restart: unless-stopped
    environment:
      POSTGRES_USER: ${DB_USERNAME:-qiaozhen}
      POSTGRES_PASSWORD: ${DB_PASSWORD:-postgres}
      POSTGRES_DB: postgres
    volumes:
      - pgdata:/var/lib/postgresql/data
      - ./schema.sql:/docker-entrypoint-initdb.d/01_schema.sql
      - ./data.sql:/docker-entrypoint-initdb.d/02_data.sql
    networks:
      - shop-net
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U ${DB_USERNAME:-qiaozhen} -d postgres"]
      interval: 5s
      timeout: 5s
      retries: 5

  shop-be:
    image: ${REGISTRY:-registry.cn-hangzhou.aliyuncs.com}/${NAMESPACE:-qz-shop}/shop-be:${TAG:-latest}
    container_name: shop-be
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      DB_URL: jdbc:postgresql://postgres:5432/postgres?currentSchema=shop
      DB_USERNAME: ${DB_USERNAME:-qiaozhen}
      DB_PASSWORD: ${DB_PASSWORD:-postgres}
      AUTH_JWT_SECRET: ${AUTH_JWT_SECRET:-please-change-me-this-is-a-32byte-default-secret!!}
      SPRING_PROFILES_ACTIVE: prod
    depends_on:
      postgres:
        condition: service_healthy
    networks:
      - shop-net

  shop-fe:
    image: ${REGISTRY:-registry.cn-hangzhou.aliyuncs.com}/${NAMESPACE:-qz-shop}/shop-fe:${TAG:-latest}
    container_name: shop-fe
    restart: unless-stopped
    ports:
      - "80:80"
    networks:
      - shop-net

volumes:
  pgdata:

networks:
  shop-net:
    driver: bridge
```

---

### Task 6: 验证与提交

- [ ] **Step 9: 验证 Dockerfile 构建**

```bash
docker build -t shop-be:test .
```

- [ ] **Step 10: 提交所有文件**

```bash
git add Dockerfile .dockerignore deploy/ .github/ docker-compose.yml src/main/resources/application-prod.properties
git commit -m "feat: add Docker deploy and GitHub Actions CI/CD"
```
