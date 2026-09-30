#!/usr/bin/env bash
# 比对本地消息表：正式 DDL（02-infra.sql）与 starter-mq 单测建表脚本的列集合必须一致。
# 两边一旦漂移，单测全绿而生产插入失败——这正是计划中标注的风险点。
set -euo pipefail
cd "$(dirname "$0")/.."

source scripts/lib/local-mysql.sh

# 正式 DDL 与单测脚本分别导入本机 MySQL 的两个校验库（每次运行覆盖）
DB=ccloud_verify
TEST_DB=ccloud_verify_mq_test
TABLES="infra_message_outbox infra_message_consume_log infra_message_dead_letter"

q() { lq "$1"; }

reset_db $DB
reset_db $TEST_DB
rename_db $DB < sql/mysql/ddl/02-infra.sql | lmysql $DB 2>/dev/null
lmysql $TEST_DB < ccloud-framework/ccloud-spring-boot-starter-mq/src/test/resources/sql/schema.sql 2>/dev/null

fail=0
for t in $TABLES; do
  ddl=$(q "SELECT column_name FROM information_schema.columns WHERE table_schema='$DB' AND table_name='$t' ORDER BY column_name" | tr '\n' ' ')
  tst=$(q "SELECT column_name FROM information_schema.columns WHERE table_schema='$TEST_DB' AND table_name='$t' ORDER BY column_name" | tr '\n' ' ')
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
  cols=$(q "SELECT column_name FROM information_schema.columns WHERE table_schema='$DB' AND table_name='$table'")
  for f in $(grep -oE 'private [A-Za-z<>]+ [a-zA-Z]+;' "$file" | awk '{print $3}' | tr -d ';'); do
    col=$(echo "$f" | perl -pe 's/([A-Z])/_\l$1/g')
    echo "$cols" | grep -qx "$col" || { echo "  FAIL $table 缺少 DO 字段 $f 对应的列 $col"; fail=1; }
  done
}
D=ccloud-framework/ccloud-spring-boot-starter-mq/src/main/java/com/ccloud/framework/mq/outbox/core
check_do $D/OutboxMessageDO.java infra_message_outbox
check_do $D/ConsumeLogDO.java infra_message_consume_log

[ $fail -eq 0 ] && echo "==> 本地消息表结构一致" || exit 1
