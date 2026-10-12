#!/usr/bin/env bash
# 在本机 MySQL 中导入 SQL 并校验结果。导入到独立的校验库 ccloud_verify（每次运行覆盖），不影响本机其他库。
# 用法：verify-sql.sh [脚本路径]，默认校验全量脚本 sql/mysql/ccloud.sql
set -euo pipefail
cd "$(dirname "$0")/.."
source scripts/lib/local-mysql.sh

SQL_FILE="${1:-sql/mysql/ccloud.sql}"
DB=ccloud_verify
ERR=$(mktemp)
trap 'rm -f "$ERR"' EXIT

if [ ! -f "$SQL_FILE" ]; then
  echo "  FAIL 脚本不存在: $SQL_FILE"; exit 1
fi

echo "==> 导入 $SQL_FILE 到本机 MySQL 的 $DB 库"
reset_db $DB
if ! rename_db $DB < "$SQL_FILE" | lmysql $DB 2>"$ERR"; then
  echo "  FAIL 导入失败："; cat "$ERR"; exit 1
fi
# MySQL 会把密码警告写到 stderr，只有出现 ERROR 才算真失败
if grep -q "ERROR" "$ERR"; then
  echo "  FAIL 导入过程中出现错误："; grep "ERROR" "$ERR"; exit 1
fi

echo "==> 校验结果"
q() { lq "$1"; }

TABLES=$(q "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB';")
echo "  表数量: $TABLES"
[ "$TABLES" -gt 0 ] || { echo "  FAIL $DB 库中没有表"; exit 1; }

# 字符集必须是 utf8mb4，否则存不了 emoji 与部分生僻字
BAD_CHARSET=$(q "SELECT COUNT(*) FROM information_schema.tables
  WHERE table_schema='$DB' AND table_collation NOT LIKE 'utf8mb4%';")
[ "$BAD_CHARSET" = "0" ] || { echo "  FAIL 有 $BAD_CHARSET 张表的字符集不是 utf8mb4"; exit 1; }
echo "  OK   全部表字符集为 utf8mb4"

# 每张业务表都应带审计字段，缺失说明建表时漏了公共列
MISSING_AUDIT=$(q "SELECT GROUP_CONCAT(t.table_name) FROM information_schema.tables t
  WHERE t.table_schema='$DB' AND t.table_name NOT LIKE 'ACT!_%' ESCAPE '!'
    AND NOT EXISTS (SELECT 1 FROM information_schema.columns c
      WHERE c.table_schema='$DB' AND c.table_name=t.table_name AND c.column_name='create_time');")
if [ -n "$MISSING_AUDIT" ] && [ "$MISSING_AUDIT" != "NULL" ]; then
  echo "  FAIL 以下表缺少审计字段 create_time: $MISSING_AUDIT"; exit 1
fi
echo "  OK   全部表均带审计字段"

# 逻辑删除列须为 bigint：删除时写入记录编号，唯一索引 (..., deleted) 才不会与已删除的记录冲突；
# 若为 bit / tinyint，同一个值第二次删除就会违反唯一约束
BAD_DELETED=$(q "SELECT GROUP_CONCAT(table_name) FROM information_schema.columns
  WHERE table_schema='$DB' AND column_name='deleted' AND (data_type<>'bigint' OR column_default<>'0' OR is_nullable<>'NO');")
if [ -n "$BAD_DELETED" ] && [ "$BAD_DELETED" != "NULL" ]; then
  echo "  FAIL 以下表的 deleted 列不是 bigint NOT NULL DEFAULT 0: $BAD_DELETED"; exit 1
fi
echo "  OK   全部表的 deleted 列为 bigint NOT NULL DEFAULT 0"

# 每张表都必须有主键，无主键表在主从复制与在线 DDL 下都会出问题
NO_PK=$(q "SELECT GROUP_CONCAT(t.table_name) FROM information_schema.tables t
  WHERE t.table_schema='$DB'
    AND NOT EXISTS (SELECT 1 FROM information_schema.table_constraints tc
      WHERE tc.table_schema='$DB' AND tc.table_name=t.table_name
        AND tc.constraint_type='PRIMARY KEY');")
if [ -n "$NO_PK" ] && [ "$NO_PK" != "NULL" ]; then
  echo "  FAIL 以下表没有主键: $NO_PK"; exit 1
fi
echo "  OK   全部表均有主键"

# 每张表都应有注释，否则代码生成器与数据库文档都产不出可读内容
NO_COMMENT=$(q "SELECT GROUP_CONCAT(table_name) FROM information_schema.tables
  WHERE table_schema='$DB' AND (table_comment IS NULL OR table_comment='');")
if [ -n "$NO_COMMENT" ] && [ "$NO_COMMENT" != "NULL" ]; then
  echo "  FAIL 以下表缺少注释: $NO_COMMENT"; exit 1
fi
echo "  OK   全部表均有注释"

# 校验初始化数据（仅当导入的是全量脚本时）
HAS_DATA=$(q "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB' AND table_name='system_menu';")
if [ "$HAS_DATA" = "1" ] && [ "$(q "SELECT COUNT(*) FROM $DB.system_menu;")" != "0" ]; then
  check() {  # $1=说明 $2=SQL（返回 0 表示通过）
    local r; r=$(q "$2")
    if [ "$r" = "0" ]; then echo "  OK   $1"; else echo "  FAIL $1（违规 $r 条）"; exit 1; fi
  }
  expect_eq() {  # $1=说明 $2=期望 $3=SQL
    local r; r=$(q "$3")
    if [ "$r" = "$2" ]; then echo "  OK   $1 = $r"; else echo "  FAIL $1 = $r，期望 $2"; exit 1; fi
  }
  expect_eq "表数量（按设计清单）" 142 "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$DB';"
  expect_eq "admin 账号"          1   "SELECT COUNT(*) FROM $DB.system_users WHERE username='admin';"
  expect_eq "OAuth2 客户端"        4   "SELECT COUNT(*) FROM $DB.system_oauth2_client;"
  MENUS=$(q "SELECT COUNT(*) FROM $DB.system_menu;")
  [ "$MENUS" -gt 200 ] || { echo "  FAIL 菜单数 $MENUS 不足 200"; exit 1; }
  echo "  OK   菜单数 = $MENUS"

  check "菜单无孤儿节点" "SELECT COUNT(*) FROM $DB.system_menu m WHERE m.parent_id<>0
    AND NOT EXISTS (SELECT 1 FROM $DB.system_menu p WHERE p.id=m.parent_id);"
  check "按钮下不再挂子菜单" "SELECT COUNT(*) FROM $DB.system_menu m JOIN $DB.system_menu p
    ON m.parent_id=p.id WHERE p.type=3;"
  check "按钮均有权限标识" "SELECT COUNT(*) FROM $DB.system_menu WHERE type=3 AND permission='';"
  check "权限标识不重复" "SELECT COUNT(*) FROM (SELECT permission FROM $DB.system_menu
    WHERE permission<>'' GROUP BY permission HAVING COUNT(*)>1) t;"
  check "角色菜单关联无悬空引用" "SELECT COUNT(*) FROM $DB.system_role_menu rm
    WHERE NOT EXISTS (SELECT 1 FROM $DB.system_menu m WHERE m.id=rm.menu_id)
       OR NOT EXISTS (SELECT 1 FROM $DB.system_role r WHERE r.id=rm.role_id);"
  check "用户角色关联无悬空引用" "SELECT COUNT(*) FROM $DB.system_user_role ur
    WHERE NOT EXISTS (SELECT 1 FROM $DB.system_users u WHERE u.id=ur.user_id)
       OR NOT EXISTS (SELECT 1 FROM $DB.system_role r WHERE r.id=ur.role_id);"
  check "用户所属部门均存在" "SELECT COUNT(*) FROM $DB.system_users u WHERE u.dept_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM $DB.system_dept d WHERE d.id=u.dept_id);"
  check "字典数据的类型均已定义" "SELECT COUNT(*) FROM $DB.system_dict_data d
    WHERE NOT EXISTS (SELECT 1 FROM $DB.system_dict_type t WHERE t.type=d.dict_type);"
  check "同一字典类型下值不重复" "SELECT COUNT(*) FROM (SELECT dict_type, value FROM $DB.system_dict_data
    GROUP BY dict_type, value HAVING COUNT(*)>1) t;"
  check "密码均为 BCrypt 格式" "SELECT COUNT(*) FROM $DB.system_users WHERE password NOT LIKE '\$2a\$04\$%';"
  check "普通管理员没有任何删除权限（演示权限差异）" "SELECT COUNT(*) FROM $DB.system_user_role ur
    JOIN $DB.system_role_menu rm ON rm.role_id=ur.role_id JOIN $DB.system_menu m ON m.id=rm.menu_id
    WHERE ur.user_id=2 AND m.permission LIKE '%:delete';"
  expect_eq "普通管理员拥有 system:user:query" 1 "SELECT COUNT(*) FROM $DB.system_user_role ur
    JOIN $DB.system_role_menu rm ON rm.role_id=ur.role_id JOIN $DB.system_menu m ON m.id=rm.menu_id
    WHERE ur.user_id=2 AND m.permission='system:user:query';"
  expect_eq "SSO 回调地址白名单包含 E2E 使用的地址" 1 "SELECT COUNT(*) FROM $DB.system_oauth2_client
    WHERE client_id='ccloud-web' AND JSON_CONTAINS(redirect_uris, '\"http://localhost:8080/cb\"');"
  # 中文按字节比对而非肉眼看：容器内客户端缺 locale 时中文会显示为 ???，
  # 与真正的编码损坏在屏幕上无法区分；源文件若丢失 SET NAMES utf8mb4 则会真的写坏
  EXPECT_HEX=$(printf '超级管理员' | xxd -p | tr 'a-f' 'A-F')
  expect_eq "中文按 UTF-8 字节原样入库" "$EXPECT_HEX" "SELECT HEX(nickname) FROM $DB.system_users WHERE id=1;"
  expect_eq "错误码中英文文案条数一致" 0 "SELECT ABS(SUM(locale='zh_CN') - SUM(locale='en_US')) FROM $DB.system_i18n_message;"
  expect_eq "会员演示账号" 1 "SELECT COUNT(*) FROM $DB.member_user WHERE mobile='13800138000';"
fi

echo "==> 通过"
