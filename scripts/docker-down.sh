#!/usr/bin/env bash
# 停止 ccloud 的应用编排。本机中间件及其中的 ccloud 数据（ccloud 库、Nacos 命名空间、XXL-Job 任务）保持不变。
# 用法：scripts/docker-down.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/docker"
while IFS='=' read -r key value; do
  [[ -z "$key" || "$key" == \#* ]] && continue
  if [ -z "${!key+x}" ]; then export "$key=$value"; fi
done < .env

# docker-up.sh 把调度中心接入了 ccloud 的网络，先断开，否则删除网络会失败
NETWORK="${COMPOSE_PROJECT_NAME}_default"
XXL_JOB_CONTAINER="${CCLOUD_XXL_JOB_CONTAINER:-xxl-job-admin}"
if docker inspect "$XXL_JOB_CONTAINER" --format '{{json .NetworkSettings.Networks}}' 2>/dev/null | grep -q "\"$NETWORK\""; then
  docker network disconnect "$NETWORK" "$XXL_JOB_CONTAINER"
fi

docker compose down --remove-orphans
# 清理本项目留下的匿名卷与悬空镜像。volume prune 只删匿名卷，具名卷（如 infra-files）保留
docker volume prune -f --filter "label=com.docker.compose.project=${COMPOSE_PROJECT_NAME}" >/dev/null || true
docker image prune -f --filter "label=com.docker.compose.project=${COMPOSE_PROJECT_NAME}" >/dev/null || true
echo "已停止 ccloud 应用（本机中间件未受影响），已清理本项目的匿名卷与悬空镜像"
