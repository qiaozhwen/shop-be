#!/usr/bin/env bash
# 服务器端手动部署脚本
#
# 用法:
#   1) 复制到服务器: scp deploy/deploy.sh deploy@<ECS_IP>:~/deploy-be.sh
#   2) 执行:
#      export REGISTRY_USERNAME=xxx REGISTRY_PASSWORD=xxx AUTH_JWT_SECRET=xxx
#      ./deploy-be.sh           # 拉 latest 并重启
#      ./deploy-be.sh <sha7>    # 回滚到指定版本

set -euo pipefail

# ====== 按需修改 ======
REGISTRY="${REGISTRY:-registry.cn-hangzhou.aliyuncs.com}"
NAMESPACE="${NAMESPACE:-qz-shop}"
IMAGE_NAME="${IMAGE_NAME:-shop-be}"
CONTAINER_NAME="${CONTAINER_NAME:-shop-be}"
HOST_PORT="${HOST_PORT:-8080}"
# 数据库配置（通过环境变量传入，避免落盘）
#   export DB_URL=xxx DB_USERNAME=xxx DB_PASSWORD=xxx AUTH_JWT_SECRET=xxx
# ======================

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

docker network inspect shop-network >/dev/null 2>&1 || docker network create shop-network

echo "==> 启动新容器"
docker run -d \
  --name "${CONTAINER_NAME}" \
  --restart unless-stopped \
  --network shop-network \
  -p "${HOST_PORT}:8080" \
  -e DB_URL="${DB_URL:?DB_URL required}" \
  -e DB_USERNAME="${DB_USERNAME:?DB_USERNAME required}" \
  -e DB_PASSWORD="${DB_PASSWORD:?DB_PASSWORD required}" \
  -e AUTH_JWT_SECRET="${AUTH_JWT_SECRET:?AUTH_JWT_SECRET required}" \
  -e BOOTSTRAP_ADMIN_PHONE="${BOOTSTRAP_ADMIN_PHONE:-}" \
  -e BOOTSTRAP_ADMIN_PASSWORD="${BOOTSTRAP_ADMIN_PASSWORD:-}" \
  -e BOOTSTRAP_ADMIN_NAME="${BOOTSTRAP_ADMIN_NAME:-系统管理员}" \
  -e BOOTSTRAP_ADMIN_STORE_ID="${BOOTSTRAP_ADMIN_STORE_ID:-1}" \
  -e SPRING_PROFILES_ACTIVE=prod \
  "${IMAGE}"

echo "==> 等待容器健康检查"
for _ in $(seq 1 45); do
  STATUS="$(docker inspect -f '{{.State.Health.Status}}' "${CONTAINER_NAME}" 2>/dev/null || true)"
  [[ "${STATUS}" == "healthy" ]] && break
  if [[ "${STATUS}" == "unhealthy" ]]; then
    docker logs "${CONTAINER_NAME}"
    exit 1
  fi
  sleep 2
done
[[ "$(docker inspect -f '{{.State.Health.Status}}' "${CONTAINER_NAME}")" == "healthy" ]] || {
  docker logs "${CONTAINER_NAME}"
  exit 1
}

echo "==> 清理悬挂镜像"
docker image prune -f

echo "==> 当前运行容器:"
docker ps --filter "name=${CONTAINER_NAME}"

echo "==> 完成，端口: ${HOST_PORT}"
