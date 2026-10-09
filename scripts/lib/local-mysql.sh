# 校验脚本共用：在本机已有的 MySQL 容器里执行 SQL，不再为校验临时启动容器。
# root 密码取自容器自身的环境变量 MYSQL_ROOT_PASSWORD，脚本与仓库中都不出现密码。
# 用法：source "$(dirname "$0")/lib/local-mysql.sh"
#   lmysql [mysql 参数...]      以 root 执行 mysql 客户端，标准输入透传
#   lq "SQL"                    执行查询，输出无表头、制表符分隔
#   rename_db 新库名            过滤器：把 SQL 中的库名 `ccloud` 改为新库名（只动带反引号的库名）
#   reset_db 库名               删除并重建本脚本专用的校验库（只允许 ccloud_verify 开头的库）

MYSQL_CONTAINER="${CCLOUD_MYSQL_CONTAINER:-mysql}"

if [ "$(docker inspect -f '{{.State.Running}}' "$MYSQL_CONTAINER" 2>/dev/null)" != "true" ]; then
  echo "  FAIL 本机 MySQL 容器 $MYSQL_CONTAINER 未运行：请先 docker start $MYSQL_CONTAINER（容器名可用 CCLOUD_MYSQL_CONTAINER 指定）"
  exit 1
fi

lmysql() {
  docker exec -i "$MYSQL_CONTAINER" sh -c 'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" --default-character-set=utf8mb4 "$@"' mysql "$@"
}

lq() {
  lmysql -N -B -e "$1" 2>/dev/null
}

rename_db() {
  sed "s/\`ccloud\`/\`$1\`/g"
}

reset_db() {
  case "$1" in
    ccloud_verify*) ;;
    *) echo "  FAIL reset_db 只能用于 ccloud_verify 开头的校验库，拒绝操作 $1"; exit 1 ;;
  esac
  lq "DROP DATABASE IF EXISTS \`$1\`; CREATE DATABASE \`$1\` DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;"
}
