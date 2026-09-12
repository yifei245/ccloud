#!/usr/bin/env bash
# 在真实 MySQL 8.4 容器中导入 SQL 并校验结果。
# 用法：verify-sql.sh [脚本路径]，默认校验全量脚本 sql/mysql/ccloud.sql
set -euo pipefail
cd "$(dirname "$0")/.."

SQL_FILE="${1:-sql/mysql/ccloud.sql}"
CID=ccloud-sql-verify
PASSWORD=root123

if [ ! -f "$SQL_FILE" ]; then
  echo "  FAIL 脚本不存在: $SQL_FILE"; exit 1
fi

cleanup() { docker rm -f $CID >/dev/null 2>&1 || true; }
trap cleanup EXIT
cleanup

echo "==> 启动 MySQL 8.4 容器"
docker run -d --name $CID \
  -e MYSQL_ROOT_PASSWORD=$PASSWORD \
  -e MYSQL_ROOT_HOST=% \
  mysql:8.4 --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci >/dev/null

echo "==> 等待 MySQL 就绪（最多 120 秒）"
deadline=$((SECONDS + 120))
until docker exec $CID mysqladmin ping -uroot -p$PASSWORD --silent >/dev/null 2>&1; do
  if [ $SECONDS -ge $deadline ]; then
    echo "  FAIL MySQL 启动超时"; docker logs --tail 30 $CID; exit 1
  fi
  sleep 2
done

echo "==> 导入 $SQL_FILE"
if ! docker exec -i $CID mysql -uroot -p$PASSWORD < "$SQL_FILE" 2>/tmp/ccloud-sql-err.log; then
  echo "  FAIL 导入失败："; cat /tmp/ccloud-sql-err.log; exit 1
fi
# MySQL 会把密码警告写到 stderr，只有出现 ERROR 才算真失败
if grep -q "ERROR" /tmp/ccloud-sql-err.log; then
  echo "  FAIL 导入过程中出现错误："; grep "ERROR" /tmp/ccloud-sql-err.log; exit 1
fi

echo "==> 校验结果"
q() { docker exec $CID mysql -uroot -p$PASSWORD -N -B -e "$1" 2>/dev/null; }

TABLES=$(q "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='ccloud';")
echo "  表数量: $TABLES"
[ "$TABLES" -gt 0 ] || { echo "  FAIL ccloud 库中没有表"; exit 1; }

# 字符集必须是 utf8mb4，否则存不了 emoji 与部分生僻字
BAD_CHARSET=$(q "SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema='ccloud' AND table_collation NOT LIKE 'utf8mb4%';")
[ "$BAD_CHARSET" = "0" ] || { echo "  FAIL 有 $BAD_CHARSET 张表的字符集不是 utf8mb4"; exit 1; }
echo "  OK   全部表字符集为 utf8mb4"

# 每张业务表都应带审计字段，缺失说明建表时漏了公共列
MISSING_AUDIT=$(q "SELECT GROUP_CONCAT(t.table_name) FROM information_schema.tables t
  WHERE t.table_schema='ccloud' AND t.table_name NOT LIKE 'ACT!_%' ESCAPE '!'
    AND NOT EXISTS (SELECT 1 FROM information_schema.columns c
      WHERE c.table_schema='ccloud' AND c.table_name=t.table_name AND c.column_name='create_time');")
if [ -n "$MISSING_AUDIT" ] && [ "$MISSING_AUDIT" != "NULL" ]; then
  echo "  FAIL 以下表缺少审计字段 create_time: $MISSING_AUDIT"; exit 1
fi
echo "  OK   全部表均带审计字段"

# 每张表都必须有主键，无主键表在主从复制与在线 DDL 下都会出问题
NO_PK=$(q "SELECT GROUP_CONCAT(t.table_name) FROM information_schema.tables t
  WHERE t.table_schema='ccloud'
    AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints tc
      WHERE tc.table_schema='ccloud' AND tc.table_name=t.table_name
        AND tc.constraint_type='PRIMARY KEY');")
if [ -n "$NO_PK" ] && [ "$NO_PK" != "NULL" ]; then
  echo "  FAIL 以下表没有主键: $NO_PK"; exit 1
fi
echo "  OK   全部表均有主键"

# 每张表都应有注释，否则代码生成器与数据库文档都产不出可读内容
NO_COMMENT=$(q "SELECT GROUP_CONCAT(table_name) FROM information_schema.tables
  WHERE table_schema='ccloud' AND (table_comment IS NULL OR table_comment='');")
if [ -n "$NO_COMMENT" ] && [ "$NO_COMMENT" != "NULL" ]; then
  echo "  FAIL 以下表缺少注释: $NO_COMMENT"; exit 1
fi
echo "  OK   全部表均有注释"

# 校验初始化数据（仅当导入的是全量脚本时）
if q "SELECT 1 FROM information_schema.tables WHERE table_schema='ccloud' AND table_name='system_users';" | grep -q 1; then
  ADMIN=$(q "SELECT COUNT(*) FROM ccloud.system_users WHERE username='admin';")
  MENUS=$(q "SELECT COUNT(*) FROM ccloud.system_menu;")
  CLIENTS=$(q "SELECT COUNT(*) FROM ccloud.system_oauth2_client;")
  echo "  初始数据: admin=$ADMIN, 菜单=$MENUS, OAuth2 客户端=$CLIENTS"
fi

echo "==> 通过"
