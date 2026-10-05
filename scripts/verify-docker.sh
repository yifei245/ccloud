#!/usr/bin/env bash
# 校验 docker 编排与本机中间件：compose 语法、本机中间件在运行、ccloud 库、Nacos 账号与配置、XXL-Job 任务已登记。
# 用法：scripts/verify-docker.sh [--full]
#   --full  另外执行 scripts/docker-up.sh 打包并启动 gateway / system / member / monitor，校验路由、文档与执行器注册
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
# shellcheck source=lib/local-middleware.sh
source "$ROOT/scripts/lib/local-middleware.sh"

FULL=false
[ "${1:-}" = "--full" ] && FULL=true

fail() { echo "FAIL $*"; exit 1; }

echo "==> 校验四个应用的 Dockerfile 内容一致"
for f in ccloud-monitor ccloud-module-system/ccloud-module-system-biz ccloud-module-member/ccloud-module-member-biz; do
  cmp -s "$ROOT/ccloud-gateway/Dockerfile" "$ROOT/$f/Dockerfile" || fail "$f/Dockerfile 与 ccloud-gateway/Dockerfile 不一致"
done

echo "==> 校验 compose 文件语法"
(cd "$ROOT/docker" && docker compose config >/dev/null && docker compose -f docker-compose.middleware.yml config >/dev/null)

echo "==> 初始化本机中间件中的 ccloud 数据（已有则保留）"
bash "$ROOT/scripts/docker-up.sh" init >/dev/null

echo "==> 校验 ccloud 库"
expected=$(grep -c '^CREATE TABLE' "$ROOT/sql/mysql/ccloud.sql")
actual=$(lq "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ccloud';")
echo "    表数量 $actual（脚本中 $expected）"
[ "$actual" -eq "$expected" ] || fail "ccloud 库表数量不符"

echo "==> 校验 Nacos 账号可登录、命名空间 $NACOS_NAMESPACE 中的配置齐全"
token=$(nacos_token)
[ -n "$token" ] || fail "Nacos 登录失败，请检查 $CCLOUD_MIDDLEWARE_FILE 中的 NACOS_USERNAME / NACOS_PASSWORD"
for file in "$ROOT"/docker/nacos/config/*.yaml; do
  data_id=$(basename "$file")
  curl -s -G "$NACOS/v3/admin/cs/config" -H "accessToken: $token" --data-urlencode "dataId=$data_id" \
    --data-urlencode "groupName=DEFAULT_GROUP" --data-urlencode "namespaceId=$NACOS_NAMESPACE" \
    | grep -q '"code":0' || fail "命名空间 $NACOS_NAMESPACE 中缺少 $data_id"
done

echo "==> 校验 XXL-Job 执行器与任务已登记"
for app in ccloud-system-server ccloud-infra-server; do
  group=$(lq "SELECT COUNT(*) FROM xxl_job.xxl_job_group WHERE app_name='$app';")
  [ "$group" -eq 1 ] || fail "缺少执行器 $app"
done
for spec in "ccloud-system-server:outboxRetryJob" "ccloud-system-server:redisStreamPendingMessageResendJob" \
            "ccloud-infra-server:redisStreamPendingMessageResendJob" "ccloud-infra-server:apiAccessLogCleanJob" \
            "ccloud-infra-server:apiErrorLogCleanJob"; do
  app="${spec%%:*}"; handler="${spec#*:}"
  running=$(lq "SELECT COUNT(*) FROM xxl_job.xxl_job_info i JOIN xxl_job.xxl_job_group g ON i.job_group=g.id
    WHERE g.app_name='$app' AND i.executor_handler='$handler' AND i.trigger_status=1;")
  [ "$running" -eq 1 ] || fail "$app 的任务 $handler 未登记或未启动"
done
# 上游脚本的 admin 默认密码 123456（SHA-256）必须已被 .env 中的密码替换
weak=$(lq "SELECT COUNT(*) FROM xxl_job.xxl_job_user
  WHERE password='8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92';")
[ "$weak" -eq 0 ] || fail "XXL-Job 管理员仍是默认密码"
xxl_login=$(curl -s -X POST "http://127.0.0.1:${XXL_JOB_PORT}/xxl-job-admin/auth/doLogin" \
  --data-urlencode "userName=admin" --data-urlencode "password=${XXL_JOB_ADMIN_PASSWORD}")
echo "$xxl_login" | grep -q '"code":200' || fail "XXL-Job 管理员无法用 .env 中的密码登录：$xxl_login"

if $FULL; then
  echo "==> 打包、构建并启动全部应用"
  bash "$ROOT/scripts/docker-up.sh" >/dev/null
  echo "==> 校验网关可路由、内部接口被阻断"
  code=$(curl -s -o /dev/null -w '%{http_code}' "http://127.0.0.1:${GATEWAY_PORT}/rpc-api/system/tenant/valid?id=1")
  [ "$code" = 403 ] || fail "网关未阻断内部接口：$code"
  resp=$(curl -s -G -H 'X-Tenant-Id: 1' --data-urlencode 'name=ccloud 总部' \
    "http://127.0.0.1:${GATEWAY_PORT}/admin-api/system/tenant/get-id-by-name")
  echo "$resp" | grep -q '"data":"\{0,1\}1"\{0,1\}' || fail "经网关访问 system 失败：$resp"
  echo "==> 校验接口文档经网关聚合可用"
  for module in system member; do
    doc=$(curl -s "http://127.0.0.1:${GATEWAY_PORT}/${module}/v3/api-docs/admin")
    echo "$doc" | grep -q '"openapi"' || fail "$module 的接口文档无法生成：$(echo "$doc" | head -c 200)"
  done
  echo "==> 校验执行器已注册到 XXL-Job"
  deadline=$((SECONDS + 90))
  until [ -n "$(lq "SELECT address_list FROM xxl_job.xxl_job_group WHERE app_name='ccloud-system-server' AND address_list IS NOT NULL AND address_list<>'';")" ]; do
    [ $SECONDS -lt $deadline ] || fail "ccloud-system-server 执行器未注册到调度中心"
    sleep 5
  done
fi

echo "==> 通过"
