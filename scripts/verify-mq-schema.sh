#!/usr/bin/env bash
# 比对本地消息表：正式 DDL（02-infra.sql）与 starter-mq 单测建表脚本的列集合必须一致。
# 两边一旦漂移，单测全绿而生产插入失败——这正是计划中标注的风险点。
set -euo pipefail
cd "$(dirname "$0")/.."

CID=ccloud-mq-schema-verify
PASSWORD=root123
TABLES="infra_message_outbox infra_message_consume_log infra_message_dead_letter"

cleanup() { docker rm -f $CID >/dev/null 2>&1 || true; }
trap cleanup EXIT
cleanup

docker run -d --name $CID -e MYSQL_ROOT_PASSWORD=$PASSWORD mysql:8.4 >/dev/null
until docker exec $CID mysqladmin ping -uroot -p$PASSWORD --silent >/dev/null 2>&1; do sleep 2; done

q() { docker exec $CID mysql -uroot -p$PASSWORD -N -B -e "$1" 2>/dev/null; }

# 正式 DDL 导入 ccloud 库
cat sql/mysql/ddl/00-schema.sql sql/mysql/ddl/02-infra.sql | docker exec -i $CID mysql -uroot -p$PASSWORD 2>/dev/null
# 单测脚本导入独立库
q "CREATE DATABASE mq_test"
docker exec -i $CID mysql -uroot -p$PASSWORD mq_test \
  < ccloud-framework/ccloud-spring-boot-starter-mq/src/test/resources/sql/schema.sql 2>/dev/null

fail=0
for t in $TABLES; do
  ddl=$(q "SELECT column_name FROM information_schema.columns WHERE table_schema='ccloud' AND table_name='$t' ORDER BY column_name" | tr '\n' ' ')
  tst=$(q "SELECT column_name FROM information_schema.columns WHERE table_schema='mq_test' AND table_name='$t' ORDER BY column_name" | tr '\n' ' ')
  if [ "$ddl" = "$tst" ]; then
    echo "  OK   $t（$(echo $ddl | wc -w | tr -d ' ') 列一致）"
  else
    echo "  FAIL $t 列不一致"
    echo "       正式 DDL : $ddl"
    echo "       单测脚本 : $tst"
    fail=1
  fi
done

# DO 字段必须都能在正式表中找到对应列（驼峰转下划线）
check_do() {
  local file=$1 table=$2
  cols=$(q "SELECT column_name FROM information_schema.columns WHERE table_schema='ccloud' AND table_name='$table'")
  for f in $(grep -oE 'private [A-Za-z<>]+ [a-zA-Z]+;' "$file" | awk '{print $3}' | tr -d ';'); do
    col=$(echo "$f" | perl -pe 's/([A-Z])/_\l$1/g')
    echo "$cols" | grep -qx "$col" || { echo "  FAIL $table 缺少 DO 字段 $f 对应的列 $col"; fail=1; }
  done
}
D=ccloud-framework/ccloud-spring-boot-starter-mq/src/main/java/com/ccloud/framework/mq/outbox/core
check_do $D/OutboxMessageDO.java infra_message_outbox
check_do $D/ConsumeLogDO.java infra_message_consume_log

[ $fail -eq 0 ] && echo "==> 本地消息表结构一致" || exit 1
