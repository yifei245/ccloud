#!/usr/bin/env bash
# 启动 ccloud 的应用编排（本地开发与联调）。中间件使用本机已有的实例，ccloud 只启动自己的 6 个应用。
# 用法：
#   scripts/docker-up.sh               检查本机中间件，初始化 ccloud 库、XXL-Job 任务与 Nacos 配置，打包、构建镜像并启动应用
#   scripts/docker-up.sh init          只做初始化，不启动应用（在 IDE 中运行应用时使用）
#   scripts/docker-up.sh --reset-db    重新导入 ccloud 库（会清空 ccloud 库中的数据，仅影响 ccloud 库）
# 本机中间件的约定见 scripts/lib/local-middleware.sh；Nacos 账号写在 ~/.ccloud/middleware.env
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib/local-middleware.sh
source "$ROOT/scripts/lib/local-middleware.sh"

MODE="${1:-all}"

echo "==> 初始化 ccloud 库"
if [ "$MODE" = "--reset-db" ] || [ "$(lq "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='ccloud'")" = "0" ]; then
  lmysql < "$ROOT/sql/mysql/ccloud.sql" 2>&1 | { grep -v "Using a password" || true; }
  echo "    已导入 sql/mysql/ccloud.sql"
else
  echo "    ccloud 库已存在，保留现有数据（重新导入请加 --reset-db）"
fi

echo "==> 初始化 XXL-Job 执行器与任务"
if [ "$(lq "SELECT COUNT(*) FROM information_schema.schemata WHERE schema_name='xxl_job'")" = "0" ]; then
  lmysql < "$ROOT/docker/mysql/init/03-xxl-job.sql" 2>&1 | { grep -v "Using a password" || true; }
fi
# 登记脚本可重复执行：已登记的执行器与任务会跳过，新增的会补上
docker cp "$ROOT/docker/mysql/init/04-ccloud-init.sh" "$MYSQL_CONTAINER:/tmp/04-ccloud-init.sh"
docker exec -e XXL_JOB_ADMIN_PASSWORD="$XXL_JOB_ADMIN_PASSWORD" "$MYSQL_CONTAINER" bash /tmp/04-ccloud-init.sh 2>&1 \
  | { grep -v "Using a password" || true; }

echo "==> 导入 Nacos 配置到命名空间 $NACOS_NAMESPACE"
bash "$ROOT/scripts/nacos-import.sh"

if [ "$MODE" = "init" ]; then
  echo "==> 初始化完成，未启动应用"
  exit 0
fi

echo "==> 打包应用"
# 必须 clean：只有依赖（如 framework）变化时，jar 插件认为模块 jar 未过期，repackage 又跳过已打包的 jar，镜像里会是旧依赖
(cd "$ROOT" && mvn -q -DskipTests clean package \
  -pl ccloud-gateway,ccloud-monitor,ccloud-module-system/ccloud-module-system-biz,ccloud-module-infra/ccloud-module-infra-biz,ccloud-module-member/ccloud-module-member-biz,ccloud-module-open/ccloud-module-open-biz -am)

# 每次重建镜像，旧镜像会失去标签成为悬空镜像，反复构建会累积几十 GB。
# 只清理本项目（compose 标签 ccloud）的悬空镜像，不影响其他项目；构建失败退出时也要清理
prune_ccloud_images() {
  docker image prune -f --filter "label=com.docker.compose.project=${COMPOSE_PROJECT_NAME}" >/dev/null || true
}
trap prune_ccloud_images EXIT

echo "==> 构建镜像并启动（等待全部应用健康，最多 8 分钟）"
(cd "$ROOT/docker" && docker compose up -d --build --wait --wait-timeout 480)

# 调度中心按执行器注册的容器地址回调执行器，须接入 ccloud 的网络；docker-down.sh 会断开
NETWORK="${COMPOSE_PROJECT_NAME}_default"
if ! docker inspect "$XXL_JOB_CONTAINER" --format '{{json .NetworkSettings.Networks}}' | grep -q "\"$NETWORK\""; then
  docker network connect "$NETWORK" "$XXL_JOB_CONTAINER"
  echo "==> 已将 $XXL_JOB_CONTAINER 接入网络 $NETWORK"
fi

cat <<INFO
==> 已启动
  网关            http://127.0.0.1:${GATEWAY_PORT}      （接口文档 /doc.html）
  服务监控        http://127.0.0.1:${MONITOR_PORT}      admin / CCLOUD_MONITOR_PASSWORD
  Nacos 控制台    本机 Nacos，命名空间 ${NACOS_NAMESPACE}
  XXL-Job 控制台  http://127.0.0.1:${XXL_JOB_PORT}/xxl-job-admin      admin / XXL_JOB_ADMIN_PASSWORD
  Zipkin          http://127.0.0.1:${ZIPKIN_PORT}
INFO
