#!/usr/bin/env bash
# 启动 ccloud 的 docker 编排（本地开发与联调）。
# 用法：
#   scripts/docker-up.sh              打包全部应用、构建镜像，启动中间件与应用
#   scripts/docker-up.sh middleware   只启动中间件（应用在 IDE 中运行时使用）
# 本机端口冲突时可临时覆盖 .env 中的端口，例如：MYSQL_PORT=13306 NACOS_PORT=18848 NACOS_GRPC_PORT=19848 scripts/docker-up.sh
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/docker"

if [ "${1:-}" = "middleware" ]; then
  docker compose -f docker-compose.middleware.yml up -d --wait --wait-timeout 300
  echo "中间件已就绪：MySQL / Redis / Nacos / Zipkin / XXL-Job"
  exit 0
fi

echo "==> 打包应用"
# 必须 clean：只有依赖（如 framework）变化时，jar 插件认为模块 jar 未过期，repackage 又跳过已打包的 jar，镜像里会是旧依赖
(cd "$ROOT" && mvn -q -DskipTests clean package \
  -pl ccloud-gateway,ccloud-monitor,ccloud-module-system/ccloud-module-system-biz,ccloud-module-member/ccloud-module-member-biz -am)

echo "==> 构建镜像并启动（等待全部服务健康，最多 8 分钟）"
docker compose up -d --build --wait --wait-timeout 480

# 与 docker compose 一致：环境变量优先于 .env
while IFS='=' read -r key value; do
  [[ -z "$key" || "$key" == \#* ]] && continue
  [ -z "${!key+x}" ] && export "$key=$value"
done < .env
cat <<INFO
==> 已启动
  网关            http://127.0.0.1:${GATEWAY_PORT}      （接口文档 /doc.html）
  服务监控        http://127.0.0.1:${MONITOR_PORT}      admin / CCLOUD_MONITOR_PASSWORD
  Nacos 控制台    http://127.0.0.1:${NACOS_CONSOLE_PORT}      ${NACOS_USERNAME} / NACOS_PASSWORD
  XXL-Job 控制台  http://127.0.0.1:${XXL_JOB_PORT}/xxl-job-admin      admin / XXL_JOB_ADMIN_PASSWORD
  Zipkin          http://127.0.0.1:${ZIPKIN_PORT}
INFO
