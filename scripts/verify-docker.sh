#!/usr/bin/env bash
# 校验 docker 编排：compose 语法、中间件全部 healthy、ccloud 库自动导入、Nacos 账号可登录、XXL-Job 任务已登记。
# 用法：scripts/verify-docker.sh [--full]
#   --full  额外构建并启动 gateway / system / member / monitor，等待全部 healthy（需要先能执行 mvn package）
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
cd "$ROOT/docker"
# 载入 .env，但不覆盖已在环境中设置的变量：与 docker compose 的优先级一致，
# 例如本机端口冲突时可用 MYSQL_PORT=13306 scripts/verify-docker.sh 临时改端口
while IFS='=' read -r key value; do
  [[ -z "$key" || "$key" == \#* ]] && continue
  [ -z "${!key+x}" ] && export "$key=$value"
done < .env

FULL=false
[ "${1:-}" = "--full" ] && FULL=true

MIDDLEWARE=(-f docker-compose.middleware.yml)
ALL=(-f docker-compose.yml)

fail() { echo "FAIL $*"; exit 1; }

# 等待给定 compose 文件中的全部服务就绪：有健康检查的须 healthy，一次性任务（如 ccloud-nacos-init）须以 0 退出。
# 只看这些文件自己的服务（同一项目下其他文件的容器不算），且包含已退出的容器，崩溃不会被漏掉
wait_healthy() {
  local timeout=$1; shift
  local deadline=$((SECONDS + timeout))
  local services
  services=$(docker compose "$@" config --services | tr '\n' ' ')
  while true; do
    local pending=0 service state
    for service in $services; do
      state=$(docker compose "$@" ps -a --format '{{.State}} {{.Health}} {{.ExitCode}}' "$service" 2>/dev/null | head -1)
      case "$state" in
        "running healthy "*|"exited  0") ;;
        "exited "*) docker compose "$@" logs --tail 30 "$service"; fail "$service 异常退出：$state" ;;
        *) pending=$((pending + 1)) ;;
      esac
    done
    [ "$pending" -eq 0 ] && return 0
    if [ $SECONDS -ge $deadline ]; then
      docker compose "$@" ps -a
      fail "超时：仍有 $pending 个服务未就绪"
    fi
    sleep 5
  done
}

mysql_query() {
  docker compose "${MIDDLEWARE[@]}" exec -T ccloud-mysql \
    mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N -e "$1" 2>/dev/null
}

echo "==> 校验四个应用的 Dockerfile 内容一致"
for f in ccloud-monitor ccloud-module-system/ccloud-module-system-biz ccloud-module-member/ccloud-module-member-biz; do
  cmp -s "$ROOT/ccloud-gateway/Dockerfile" "$ROOT/$f/Dockerfile" || fail "$f/Dockerfile 与 ccloud-gateway/Dockerfile 不一致"
done

echo "==> 校验 compose 文件语法"
docker compose "${MIDDLEWARE[@]}" config >/dev/null
docker compose "${ALL[@]}" config >/dev/null

echo "==> 启动中间件"
docker compose "${MIDDLEWARE[@]}" up -d

echo "==> 等待全部中间件 healthy（最多 5 分钟）"
wait_healthy 300 "${MIDDLEWARE[@]}"

echo "==> 校验 ccloud 库已自动导入"
expected=$(grep -c '^CREATE TABLE' "$ROOT/sql/mysql/ccloud.sql")
actual=$(mysql_query "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ccloud';")
echo "    表数量 $actual（脚本中 $expected）"
[ "$actual" -eq "$expected" ] || fail "ccloud 库表数量不符"

echo "==> 校验 Nacos 账号可登录"
login=$(curl -s -X POST "http://127.0.0.1:${NACOS_PORT}/nacos/v3/auth/user/login" \
  -d "username=${NACOS_USERNAME}&password=${NACOS_PASSWORD}")
echo "$login" | grep -q accessToken || fail "Nacos 登录失败：$login"

echo "==> 校验 XXL-Job 执行器与任务已登记"
group=$(mysql_query "SELECT COUNT(*) FROM xxl_job.xxl_job_group WHERE app_name='ccloud-system-server';")
[ "$group" -eq 1 ] || fail "缺少执行器 ccloud-system-server"
for handler in outboxRetryJob redisStreamPendingMessageResendJob; do
  running=$(mysql_query "SELECT COUNT(*) FROM xxl_job.xxl_job_info i JOIN xxl_job.xxl_job_group g ON i.job_group=g.id
    WHERE g.app_name='ccloud-system-server' AND i.executor_handler='$handler' AND i.trigger_status=1;")
  [ "$running" -eq 1 ] || fail "任务 $handler 未登记或未启动"
done
# 上游脚本的 admin 默认密码 123456（SHA-256）必须已被 .env 中的密码替换
weak=$(mysql_query "SELECT COUNT(*) FROM xxl_job.xxl_job_user
  WHERE password='8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92';")
[ "$weak" -eq 0 ] || fail "XXL-Job 管理员仍是默认密码"
xxl_login=$(curl -s -X POST "http://127.0.0.1:${XXL_JOB_PORT}/xxl-job-admin/auth/doLogin" \
  --data-urlencode "userName=admin" --data-urlencode "password=${XXL_JOB_ADMIN_PASSWORD}")
echo "$xxl_login" | grep -q '"code":200' || fail "XXL-Job 管理员无法用 .env 中的密码登录：$xxl_login"

if $FULL; then
  echo "==> 构建应用 jar"
  # 必须 clean：只有依赖（如 framework）变化时，jar 插件认为模块 jar 未过期，repackage 又跳过已打包的 jar，
  # 镜像里会是旧的依赖
  (cd "$ROOT" && mvn -q -DskipTests clean package -pl ccloud-gateway,ccloud-monitor,ccloud-module-system/ccloud-module-system-biz,ccloud-module-member/ccloud-module-member-biz -am)
  echo "==> 构建镜像并启动全部服务"
  docker compose "${ALL[@]}" up -d --build
  echo "==> 等待全部服务 healthy（最多 8 分钟）"
  wait_healthy 480 "${ALL[@]}"
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
  until [ -n "$(mysql_query "SELECT address_list FROM xxl_job.xxl_job_group WHERE app_name='ccloud-system-server' AND address_list IS NOT NULL AND address_list<>'';")" ]; do
    [ $SECONDS -lt $deadline ] || fail "ccloud-system-server 执行器未注册到调度中心"
    sleep 5
  done
fi

echo "==> 通过"
