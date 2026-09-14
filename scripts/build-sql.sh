#!/usr/bin/env bash
# 拼装全量数据库脚本 sql/mysql/ccloud.sql。
# ddl/ 与 dml/ 下的分文件是唯一真源，ccloud.sql 是产物，请勿手工编辑。
set -euo pipefail
cd "$(dirname "$0")/../sql/mysql"
out=ccloud.sql
{
  echo "-- ============================================================================"
  echo "-- ccloud 全量数据库脚本"
  echo "-- 由 scripts/build-sql.sh 从 ddl/ 与 dml/ 拼装生成，请勿手工编辑本文件"
  echo "-- ============================================================================"
  echo
  cat ddl/00-schema.sql
  for f in ddl/[0-9][0-9]-*.sql; do
    [ "$f" = "ddl/00-schema.sql" ] && continue
    printf '\n-- >>>>>>>>>> %s <<<<<<<<<<\n' "$f"; cat "$f"
  done
  for f in dml/[0-9][0-9]-*.sql; do
    printf '\n-- >>>>>>>>>> %s <<<<<<<<<<\n' "$f"; cat "$f"
  done
  printf '\nSET FOREIGN_KEY_CHECKS = 1;\n'
} > "$out"
echo "已生成 sql/mysql/$out（$(wc -l < "$out" | tr -d ' ') 行，$(grep -c '^CREATE TABLE' "$out") 张表）"
