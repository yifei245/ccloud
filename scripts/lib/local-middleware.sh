# 部署与验收脚本共用：连接本机已有的中间件（MySQL、Redis、Nacos、Zipkin、XXL-Job 调度中心），ccloud 不再自带一套。
# 用法：source "$ROOT/scripts/lib/local-middleware.sh"（调用方须已设置 ROOT 为仓库根目录）
#
# 载入顺序（后者覆盖前者，已在环境中设置的变量始终优先）：
#   1. docker/.env                    端口、库编号、命名空间等非敏感配置
#   2. ~/.ccloud/middleware.env       本机中间件的账号，如 NACOS_USERNAME / NACOS_PASSWORD，不进仓库
#   3. 本机容器自身的配置             MYSQL_PASSWORD 取自 mysql 容器的 MYSQL_ROOT_PASSWORD，
#                                     REDIS_PASSWORD 取自 redis 容器启动参数 --requirepass
# 容器名可用 CCLOUD_{MYSQL,REDIS,NACOS,ZIPKIN,XXL_JOB}_CONTAINER 覆盖。

CCLOUD_MIDDLEWARE_FILE="$HOME/.ccloud/middleware.env"

_ccloud_load_env() {  # $1=文件；只设置环境中尚未设置的变量
  local key value
  while IFS='=' read -r key value; do
    [[ -z "$key" || "$key" == \#* ]] && continue
    if [ -z "${!key+x}" ]; then export "$key=$value"; fi
  done < "$1"
}

# 个人文件先于 .env 载入：两者都有的键（如 NACOS_PASSWORD）以个人文件为准
[ -f "$CCLOUD_MIDDLEWARE_FILE" ] && _ccloud_load_env "$CCLOUD_MIDDLEWARE_FILE"
_ccloud_load_env "$ROOT/docker/.env"

MYSQL_CONTAINER="${CCLOUD_MYSQL_CONTAINER:-mysql}"
REDIS_CONTAINER="${CCLOUD_REDIS_CONTAINER:-redis}"
NACOS_CONTAINER="${CCLOUD_NACOS_CONTAINER:-nacos}"
ZIPKIN_CONTAINER="${CCLOUD_ZIPKIN_CONTAINER:-zipkin}"
XXL_JOB_CONTAINER="${CCLOUD_XXL_JOB_CONTAINER:-xxl-job-admin}"

missing=""
for c in "$MYSQL_CONTAINER" "$REDIS_CONTAINER" "$NACOS_CONTAINER" "$ZIPKIN_CONTAINER" "$XXL_JOB_CONTAINER"; do
  [ "$(docker inspect -f '{{.State.Running}}' "$c" 2>/dev/null)" = "true" ] || missing="$missing $c"
done
if [ -n "$missing" ]; then
  echo "  FAIL 本机中间件容器未运行：$missing"
  echo "       请先 docker start$missing；容器名不同时用 CCLOUD_*_CONTAINER 指定"
  exit 1
fi

if [ -z "${MYSQL_PASSWORD:-}" ]; then
  MYSQL_PASSWORD="$(docker inspect "$MYSQL_CONTAINER" --format '{{range .Config.Env}}{{println .}}{{end}}' \
    | sed -n 's/^MYSQL_ROOT_PASSWORD=//p')"
  export MYSQL_PASSWORD
fi
if [ -z "${REDIS_PASSWORD+x}" ]; then
  REDIS_PASSWORD="$(docker inspect "$REDIS_CONTAINER" --format '{{json .Config.Cmd}}' | python3 -c '
import json, sys
cmd = json.load(sys.stdin) or []
print(cmd[cmd.index("--requirepass") + 1] if "--requirepass" in cmd else "")')"
  export REDIS_PASSWORD
fi
if [ "${REDIS_DATABASE:-0}" = "0" ]; then
  echo "  FAIL REDIS_DATABASE 不能为 0：本机 Redis 与其他项目共用，ccloud 须使用独立的库编号"
  exit 1
fi

# 本机 Nacos 的地址（宿主机访问）
NACOS="http://127.0.0.1:${NACOS_PORT}/nacos"

nacos_token() {
  : "${NACOS_USERNAME:?未设置 NACOS_USERNAME，请写入 $CCLOUD_MIDDLEWARE_FILE}"
  : "${NACOS_PASSWORD:?未设置 NACOS_PASSWORD，请写入 $CCLOUD_MIDDLEWARE_FILE}"
  curl -s -X POST "$NACOS/v3/auth/user/login" --data-urlencode "username=$NACOS_USERNAME" \
    --data-urlencode "password=$NACOS_PASSWORD" | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p'
}

export CCLOUD_MYSQL_CONTAINER="$MYSQL_CONTAINER"
# shellcheck source=local-mysql.sh
source "$ROOT/scripts/lib/local-mysql.sh"
